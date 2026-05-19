package com.atrialmapper;

import org.lwjgl.system.MemoryStack;
import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class Shader {

    private final int programId;

    public Shader(String vertexResourcePath, String fragmentResourcePath) throws IOException {
        programId = ShaderCompiler.buildProgram(vertexResourcePath, fragmentResourcePath);
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