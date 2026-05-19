package com.atrialmapper;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private float yawDegrees = 0f;
    private float pitchDegrees = 20f;
    private float distanceFromTarget = 3f;

    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean isMouseDragging = false;

    private final float[] forwardVector  = new float[3];
    private final float[] rightVector    = new float[3];
    private final float[] upVector       = new float[3];
    private final float[] worldUp        = { 0f, 1f, 0f };
    private final float[] cameraPosition = new float[3];

    private static final float ORBIT_SENSITIVITY = 0.4f;
    private static final float SCROLL_SENSITIVITY = 0.2f;
    private static final float MIN_DISTANCE = 1.5f;
    private static final float MAX_DISTANCE = 8f;
    private static final float MIN_PITCH = -89f;
    private static final float MAX_PITCH = 89f;

    public void registerCallbacks(long windowHandle) {
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

                yawDegrees   += deltaX * ORBIT_SENSITIVITY;
                pitchDegrees += deltaY * ORBIT_SENSITIVITY;
                pitchDegrees  = Math.clamp(pitchDegrees, MIN_PITCH, MAX_PITCH);
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        });

        glfwSetScrollCallback(windowHandle, (window, scrollX, scrollY) -> {
            distanceFromTarget -= (float) scrollY * SCROLL_SENSITIVITY;
            distanceFromTarget  = Math.clamp(distanceFromTarget, MIN_DISTANCE, MAX_DISTANCE);
        });
    }

    public boolean isUserDragging() {
        return isMouseDragging;
    }

    public float[] computeViewMatrix() {
        float yawRadians   = (float) Math.toRadians(yawDegrees % 360f);
        float pitchRadians = (float) Math.toRadians(pitchDegrees);

        float cameraX = (float)(distanceFromTarget * Math.cos(pitchRadians) * Math.sin(yawRadians));
        float cameraY = (float)(distanceFromTarget * Math.sin(pitchRadians));
        float cameraZ = (float)(distanceFromTarget * Math.cos(pitchRadians) * Math.cos(yawRadians));

        cameraPosition[0] = cameraX;
        cameraPosition[1] = cameraY;
        cameraPosition[2] = cameraZ;

        normalizeInto(new float[]{ -cameraX, -cameraY, -cameraZ }, forwardVector);
        crossInto(forwardVector, worldUp, rightVector);
        normalizeInto(rightVector, rightVector);
        crossInto(rightVector, forwardVector, upVector);

        return new float[]{
                rightVector[0],   upVector[0],  -forwardVector[0],  0f,
                rightVector[1],   upVector[1],  -forwardVector[1],  0f,
                rightVector[2],   upVector[2],  -forwardVector[2],  0f,
                -dot(rightVector, cameraPosition),
                -dot(upVector,    cameraPosition),
                dot(forwardVector, cameraPosition), 1f
        };
    }

    private void normalizeInto(float[] src, float[] dest) {
        float length = (float) Math.sqrt(
                src[0] * src[0] + src[1] * src[1] + src[2] * src[2]);
        dest[0] = src[0] / length;
        dest[1] = src[1] / length;
        dest[2] = src[2] / length;
    }

    private void crossInto(float[] a, float[] b, float[] dest) {
        dest[0] = a[1] * b[2] - a[2] * b[1];
        dest[1] = a[2] * b[0] - a[0] * b[2];
        dest[2] = a[0] * b[1] - a[1] * b[0];
    }

    private float dot(float[] vectorA, float[] vectorB) {
        return vectorA[0] * vectorB[0] +
                vectorA[1] * vectorB[1] +
                vectorA[2] * vectorB[2];
    }
}