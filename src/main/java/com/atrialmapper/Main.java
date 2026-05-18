package com.atrialmapper;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public class Main {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String WINDOW_TITLE = "Atrial Mapper";

    private static final int SPHERE_STACKS = 40;
    private static final int SPHERE_SLICES = 40;

    private long windowHandle;
    private Shader shader;
    private Mesh sphereMesh;
    private Camera camera;
    private DataSimulator dataSimulator;

    // --- GLSL Shader Sources ---

    private static final String VERTEX_SHADER_SOURCE = """
        #version 330 core
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        layout(location = 2) in float aElectrodeValue;

        out vec3 fragmentNormal;
        out float electrodeValue;

        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;

        void main() {
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            fragmentNormal = normalize(mat3(uModelMatrix) * aNormal);
            electrodeValue = aElectrodeValue;
        }
        """;

    private static final String FRAGMENT_SHADER_SOURCE = """
        #version 330 core
        in vec3 fragmentNormal;
        in float electrodeValue;
        out vec4 outputColor;

        uniform vec3 uLightDirection;

        vec3 heatmapColor(float value) {
            value = clamp(value, 0.0, 1.0);
            float red   = smoothstep(0.5, 1.0, value);
            float green = smoothstep(0.0, 0.5, value) - smoothstep(0.75, 1.0, value);
            float blue  = 1.0 - smoothstep(0.0, 0.5, value);
            return vec3(red, green, blue);
        }

        void main() {
            vec3 surfaceColor = heatmapColor(electrodeValue);
            float lightIntensity = max(dot(normalize(fragmentNormal), normalize(uLightDirection)), 0.15);
            outputColor = vec4(surfaceColor * lightIntensity, 1.0);
        }
        """;

    public void run() {
        initializeWindow();
        initializeOpenGL();
        runGameLoop();
        cleanup();
    }

    private void initializeWindow() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        windowHandle = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, 0, 0);
        if (windowHandle == 0) throw new RuntimeException("Failed to create window");

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);
    }

    private void initializeOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.08f, 0.08f, 0.12f, 1f);

        shader = new Shader(VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
        sphereMesh = new Mesh(SPHERE_STACKS, SPHERE_SLICES);
        camera = new Camera();
        camera.registerCallbacks(windowHandle);

        int totalVertexCount = (SPHERE_STACKS + 1) * (SPHERE_SLICES + 1);
        dataSimulator = new DataSimulator(totalVertexCount);
    }

    private void runGameLoop() {
        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float[] projectionMatrix = buildProjectionMatrix();
            float[] viewMatrix = camera.computeViewMatrix();
            float[] modelMatrix = buildIdentityMatrix();
            float[] mvpMatrix = multiply(multiply(projectionMatrix, viewMatrix), modelMatrix);
            
            dataSimulator.update(0.016f); // ~60fps timestep
            sphereMesh.updateVertexValues(dataSimulator.getElectrodeValues());

            shader.bind();
            shader.setUniformMatrix4("uMVPMatrix", mvpMatrix);
            shader.setUniformMatrix4("uModelMatrix", modelMatrix);
            shader.setUniformVec3("uLightDirection", 1f, 1.5f, 1f);

            sphereMesh.draw();

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    private float[] buildProjectionMatrix() {
        float fieldOfView = (float) Math.toRadians(60f);
        float aspectRatio = (float) WINDOW_WIDTH / WINDOW_HEIGHT;
        float nearPlane = 0.1f;
        float farPlane = 100f;
        float frustumScale = (float)(1.0 / Math.tan(fieldOfView / 2));

        float[] projectionMatrix = new float[16];
        projectionMatrix[0]  = frustumScale / aspectRatio;
        projectionMatrix[5]  = frustumScale;
        projectionMatrix[10] = (farPlane + nearPlane) / (nearPlane - farPlane);
        projectionMatrix[11] = -1f;
        projectionMatrix[14] = (2 * farPlane * nearPlane) / (nearPlane - farPlane);
        return projectionMatrix;
    }

    private float[] buildIdentityMatrix() {
        return new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }

    private float[] multiply(float[] matrixA, float[] matrixB) {
        float[] result = new float[16];
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                for (int inner = 0; inner < 4; inner++)
                    result[row + col * 4] += matrixA[row + inner * 4] * matrixB[inner + col * 4];
        return result;
    }

    private void cleanup() {
        sphereMesh.cleanup();
        shader.cleanup();
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}