package com.atrialmapper;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    // Spherical coordinates around the target point
    private float yawDegrees = 0f;
    private float pitchDegrees = 20f;
    private float distanceFromTarget = 3f;

    // Mouse tracking
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isMouseDragging = false;

    // Sensitivity settings
    private static final float ORBIT_SENSITIVITY = 0.4f;
    private static final float SCROLL_SENSITIVITY = 0.2f;
    private static final float MIN_DISTANCE = 1.5f;
    private static final float MAX_DISTANCE = 8f;
    private static final float MIN_PITCH = -89f;
    private static final float MAX_PITCH = 89f;

    public void registerCallbacks(long windowHandle){
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                isMouseDragging = action == GLFW_PRESS;

                double[] mouseX = new double[1];
                double[] mouseY = new double[1];
                glfwGetCursorPos(window, mouseX, mouseY);
                lastMouseX = mouseX[0];
                lastMouseY = mouseY[0];
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, mouseX, mouseY) -> {
            if (isMouseDragging) {
                float deltaX = (float)(mouseX - lastMouseX);
                float deltaY = (float)(mouseY - lastMouseY);

                yawDegrees += deltaX * ORBIT_SENSITIVITY;
                pitchDegrees += deltaY * ORBIT_SENSITIVITY;
                pitchDegrees = Math.clamp(pitchDegrees, MIN_PITCH, MAX_PITCH);
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        });

        glfwSetScrollCallback(windowHandle, (window, scrollX, scrollY) -> {
            distanceFromTarget -= (float) scrollY * SCROLL_SENSITIVITY;
            distanceFromTarget = Math.clamp(distanceFromTarget, MIN_DISTANCE, MAX_DISTANCE);
        });
    }

    // Builds a view matrix from the current spherical coordinates
    public float[] computeViewMatrix() {
        float yawRadians = (float) Math.toRadians(yawDegrees);
        float pitchRadians = (float) Math.toRadians(pitchDegrees);

        // Convert spherical -> cartesian to get camera position
        float cameraX = (float)(distanceFromTarget * Math.cos(pitchRadians) * Math.sin(yawRadians));
        float cameraY = (float)(distanceFromTarget * Math.sin(pitchRadians));
        float cameraZ = (float)(distanceFromTarget * Math.cos(pitchRadians) * Math.cos(yawRadians));

        // Build orthonormal basis vectors for the camera
        float[] forwardVector = normalize(new float[]{ -cameraX, -cameraY, -cameraZ });
        float[] worldUp = { 0f, 1f, 0f };
        float[] rightVector = normalize(cross(forwardVector, worldUp));
        float[] upVector = cross(rightVector, forwardVector);

        float[] cameraPosition = { cameraX, cameraY, cameraZ };

        // Column-major view matrix (what OpenGL expects)
        return new float[]{
                rightVector[0],   upVector[0],  -forwardVector[0],  0f,
                rightVector[1],   upVector[1],  -forwardVector[1],  0f,
                rightVector[2],   upVector[2],  -forwardVector[2],  0f,
                -dot(rightVector, cameraPosition),
                -dot(upVector, cameraPosition),
                dot(forwardVector, cameraPosition), 1f
        };
    }

    // --- Vector math helpers ---

    private float[] normalize(float[] vector) {
        float length = (float) Math.sqrt(
                vector[0] * vector[0] +
                        vector[1] * vector[1] +
                        vector[2] * vector[2]
        );
        return new float[]{ vector[0] / length, vector[1] / length, vector[2] / length };
    }

    private float[] cross(float[] vectorA, float[] vectorB) {
        return new float[]{
                vectorA[1] * vectorB[2] - vectorA[2] * vectorB[1],
                vectorA[2] * vectorB[0] - vectorA[0] * vectorB[2],
                vectorA[0] * vectorB[1] - vectorA[1] * vectorB[0]
        };
    }

    private float dot(float[] vectorA, float[] vectorB) {
        return vectorA[0] * vectorB[0] +
                vectorA[1] * vectorB[1] +
                vectorA[2] * vectorB[2];
    }
}
