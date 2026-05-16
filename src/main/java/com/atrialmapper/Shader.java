package com.atrialmapper;

import org.lwjgl.system.MemoryStack;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

/**
 * Represents an OpenGL shader program consisting of a vertex shader
 * and a fragment shader.
 *
 * Handles:
 * - Compiling shaders
 * - Linking shaders into a program
 * - Binding the shader program
 * - Uploading uniform values
 * - Cleaning up GPU resources
 *
 * Requires an active OpenGL context before use.
 */
public class Shader {

    /**
     * OpenGL ID of the linked shader program.
     */
    private final int programId;

    /**
     * Creates and links a shader program from vertex and fragment shader source code.
     *
     * @param vertexSource GLSL source code for the vertex shader
     * @param fragmentSource GLSL source code for the fragment shader
     *
     * @throws RuntimeException if shader compilation or linking fails
     */
    public Shader(String vertexSource, String fragmentSource) {
        int vertexShaderId = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();

        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);

        glLinkProgram(programId);

        // Shader objects are no longer needed after linking.
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException(
                    "Shader link error: " + glGetProgramInfoLog(programId)
            );
    }

    /**
     * Compiles an individual shader.
     *
     * @param shaderType OpenGL shader type
     * @param shaderSource GLSL source code
     *
     * @return the OpenGL shader ID
     *
     * @throws RuntimeException if compilation fails
     */
    private int compileShader(int shaderType, String shaderSource) {
        int shaderId = glCreateShader(shaderType);

        glShaderSource(shaderId, shaderSource);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException(
                    "Shader compile error: " + glGetShaderInfoLog(shaderId)
            );

        return shaderId;
    }

    /**
     * Activates this shader program for rendering.
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Uploads a 4x4 matrix uniform to the shader program.
     *
     * The matrix array must contain exactly 16 float values.
     *
     * @param uniformName name of the GLSL uniform variable
     * @param matrix matrix data as a float array
     */
    public void setUniformMatrix4(String uniformName, float[] matrix) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrixBuffer = stack.mallocFloat(16);

            matrixBuffer.put(matrix).flip();

            glUniformMatrix4fv(uniformLocation, false, matrixBuffer);
        }
    }

    /**
     * Uploads a vec3 uniform value to the shader program.
     *
     * @param uniformName name of the GLSL uniform variable
     * @param x x component
     * @param y y component
     * @param z z component
     */
    public void setUniformVec3(String uniformName, float x, float y, float z) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);

        glUniform3f(uniformLocation, x, y, z);
    }

    /**
     * Deletes the shader program and frees GPU resources.
     */
    public void cleanup() {
        glDeleteProgram(programId);
    }
}