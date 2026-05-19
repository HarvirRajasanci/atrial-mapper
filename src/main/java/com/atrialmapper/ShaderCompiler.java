package com.atrialmapper;

import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL33.*;

public final class ShaderCompiler {

    private ShaderCompiler() {}

    public static int buildProgram(String vertPath, String fragPath) throws IOException {
        String vertSource = loadResource(vertPath);
        String fragSource = loadResource(fragPath);

        if (vertSource.isEmpty() || fragSource.isEmpty())
            throw new IOException("Shader source is empty");

        int vertId = compileShader(GL_VERTEX_SHADER, vertSource);
        int fragId = compileShader(GL_FRAGMENT_SHADER, fragSource);

        int programId = glCreateProgram();
        glAttachShader(programId, vertId);
        glAttachShader(programId, fragId);
        glLinkProgram(programId);
        glDeleteShader(vertId);
        glDeleteShader(fragId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId));

        return programId;
    }

    private static int compileShader(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(id));
        return id;
    }

    private static String loadResource(String path) throws IOException {
        InputStream in = ShaderCompiler.class.getClassLoader().getResourceAsStream(path);
        if (in == null)
            throw new IOException("Could not find shader resource: " + path);
        return new String(in.readAllBytes());
    }
}
