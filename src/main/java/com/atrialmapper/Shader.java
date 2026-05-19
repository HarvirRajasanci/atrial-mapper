package com.atrialmapper;

import org.lwjgl.system.MemoryStack;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Shader {

    private final int programId;

    public Shader(String vertexResourcePath, String fragmentResourcePath) throws IOException {
        String vertexSource   = loadResource(vertexResourcePath);
        String fragmentSource = loadResource(fragmentResourcePath);

        if (vertexSource.isEmpty() || fragmentSource.isEmpty())
            throw new IOException("Shader source is empty");

        int vertexShaderId   = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId));
    }

    private String loadResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (inputStream == null)
            throw new IOException("Could not find shader resource: " + resourcePath);
        return new String(inputStream.readAllBytes());
    }

    private int compileShader(int shaderType, String shaderSource) {
        int shaderId = glCreateShader(shaderType);
        glShaderSource(shaderId, shaderSource);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shaderId));
        return shaderId;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void setUniformMatrix4(String uniformName, float[] matrix) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer matrixBuffer = stack.mallocFloat(16);
            matrixBuffer.put(matrix).flip();
            glUniformMatrix4fv(uniformLocation, false, matrixBuffer);
        }
    }

    public void setUniformVec3(String uniformName, float x, float y, float z) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        glUniform3f(uniformLocation, x, y, z);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }
}