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

    private void registerCallbacks(long windowHandle){
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW_GAMEPAD_BUTTON_LAST) {
                isMouseDragging = action == GLFW_PRESS;

                // Snapshot mouse position when drag starts
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

    // TODO: Build a view matrix from the current spherical coordinates
//    public float[] computeViewMatrix() {}
}
