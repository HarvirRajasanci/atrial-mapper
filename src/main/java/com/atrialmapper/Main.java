package com.atrialmapper;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

import java.io.IOException;

public class Main {

    private static final int    WINDOW_WIDTH           = 1280;
    private static final int    WINDOW_HEIGHT          = 720;
    private static final String WINDOW_TITLE           = "Atrial Mapper";
    private static final int    SPHERE_STACKS          = 40;
    private static final int    SPHERE_SLICES          = 40;
    private static final float  FIELD_OF_VIEW_DEGREES  = 60f;
    private static final float  NEAR_PLANE             = 0.1f;
    private static final float  FAR_PLANE              = 100f;
    private static final float  MODEL_ROTATION_DEGREES = -90f;
    private static final float  AUTO_ROTATION_SPEED    = 20f;

    private WindowManager windowManager;
    private Shader shader;
    private Mesh sphereMesh;
    private Camera camera;
    private ElectrodeDataStream electrodeDataStream;
    private ColorLegend colorLegend;

    private float autoRotationAngle = 0f;
    private long lastFrameTime  = System.currentTimeMillis();
    private long lastFpsTime    = System.currentTimeMillis();
    private int frameCount      = 0;
    private boolean isWireframeMode = false;

    public void run() {
        windowManager = new WindowManager(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE);
        windowManager.initialize();
        initializeOpenGL();
        runGameLoop();
        cleanup();
    }

    private void initializeOpenGL() {
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.08f, 0.08f, 0.12f, 1f);

        try {
            shader = new Shader("shaders/surface.vert", "shaders/surface.frag");
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to load shaders: " + ioException.getMessage());
        }

        try {
            MeshData heartMeshData = StlLoader.loadFromResources("heart.stl");
            sphereMesh = new Mesh(heartMeshData);
            electrodeDataStream = new ElectrodeDataStream(
                    heartMeshData.vertexCount,
                    heartMeshData.vertexData
            );
        } catch (IOException ioException) {
            System.err.println("Could not load heart.stl, falling back to sphere: "
                    + ioException.getMessage());
            sphereMesh = new Mesh(SPHERE_STACKS, SPHERE_SLICES);
            int totalVertexCount   = (SPHERE_STACKS + 1) * (SPHERE_SLICES + 1);
            float[] flatVertexData = new float[totalVertexCount * 7];
            electrodeDataStream    = new ElectrodeDataStream(totalVertexCount, flatVertexData);
        }

        electrodeDataStream.start();

        camera = new Camera();
        camera.registerCallbacks(windowManager.getHandle());

        try {
            colorLegend = new ColorLegend(WINDOW_WIDTH, WINDOW_HEIGHT);
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to initialize color legend: "
                    + ioException.getMessage());
        }

        registerKeyCallbacks();
    }

    private void registerKeyCallbacks() {
        glfwSetKeyCallback(windowManager.getHandle(), (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_W && action == GLFW_PRESS) {
                isWireframeMode = !isWireframeMode;
                glPolygonMode(GL_FRONT_AND_BACK, isWireframeMode ? GL_LINE : GL_FILL);
            }
        });
    }

    private void runGameLoop() {
        while (!windowManager.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            long currentFrameTime  = System.currentTimeMillis();
            float deltaTimeSeconds = (currentFrameTime - lastFrameTime) / 1000f;
            lastFrameTime          = currentFrameTime;

            if (!camera.isUserDragging()) {
                autoRotationAngle += AUTO_ROTATION_SPEED * deltaTimeSeconds;
                if (autoRotationAngle > 360f) autoRotationAngle -= 360f;
            }

            float aspectRatio  = (float) WINDOW_WIDTH / WINDOW_HEIGHT;
            float[] projection = MatrixMath.buildProjectionMatrix(
                    FIELD_OF_VIEW_DEGREES, aspectRatio, NEAR_PLANE, FAR_PLANE);
            float[] view       = camera.computeViewMatrix();
            float[] model      = buildModelMatrix();
            float[] mvpMatrix  = MatrixMath.multiply(
                    MatrixMath.multiply(projection, view), model);

            sphereMesh.updateVertexValues(electrodeDataStream.pollLatestValues());

            shader.bind();
            shader.setUniformMatrix4("uMVPMatrix", mvpMatrix);
            shader.setUniformMatrix4("uModelMatrix", model);
            shader.setUniformVec3("uLightDirection", 1f, 1.5f, 1f);

            sphereMesh.draw();
            colorLegend.draw();
            updateFpsCounter();

            windowManager.swapAndPoll();
        }
    }

    private float[] buildModelMatrix() {
        float[] rotationX = MatrixMath.buildRotationX(MODEL_ROTATION_DEGREES);
        float[] rotationY = MatrixMath.buildRotationY(autoRotationAngle);
        return MatrixMath.multiply(rotationY, rotationX);
    }

    private void updateFpsCounter() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastFpsTime;

        if (elapsedTime >= 1000) {
            int fps = (int)(frameCount * 1000.0 / elapsedTime);
            windowManager.setTitle(WINDOW_TITLE + "  |  " + fps + " FPS");
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }

    private void cleanup() {
        electrodeDataStream.stop();
        colorLegend.cleanup();
        sphereMesh.cleanup();
        shader.cleanup();
        windowManager.destroy();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}