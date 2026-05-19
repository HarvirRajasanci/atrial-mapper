package com.atrialmapper;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

public class WindowManager {

    private final int width;
    private final int height;
    private final String title;
    private long windowHandle;

    public WindowManager(int width, int height, String title) {
        this.width  = width;
        this.height = height;
        this.title  = title;
    }

    public void initialize() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("GLFW initialization failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        windowHandle = glfwCreateWindow(width, height, title, 0, 0);
        if (windowHandle == 0) throw new RuntimeException("Failed to create window");

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1);
        glfwShowWindow(windowHandle);

        GL.createCapabilities();
        if (!GL.getCapabilities().OpenGL33)
            throw new RuntimeException("OpenGL 3.3 Core Profile not supported by this GPU");
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public void swapAndPoll() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    public void setTitle(String newTitle) {
        glfwSetWindowTitle(windowHandle, newTitle);
    }

    public long getHandle() {
        return windowHandle;
    }

    public void destroy() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
}
