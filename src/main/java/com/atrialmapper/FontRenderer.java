package com.atrialmapper;

import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryUtil.*;

public class FontRenderer {

    private static final int   BITMAP_WIDTH  = 512;
    private static final int   BITMAP_HEIGHT = 512;
    private static final int   FIRST_CHAR    = 32;
    private static final int   CHAR_COUNT    = 96;
    private static final float FONT_SIZE_PX  = 16f;

    private int fontTextureId;
    private int vertexArrayObjectId;
    private int vertexBufferObjectId;
    private int shaderProgramId;
    private STBTTBakedChar.Buffer bakedCharData;

    public FontRenderer() throws IOException {
        bakedCharData   = STBTTBakedChar.malloc(CHAR_COUNT);
        buildFontTexture();
        buildRenderingBuffers();
        shaderProgramId = buildShaderProgram();
    }

    private void buildFontTexture() throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("fonts/Roboto-Regular.ttf");
        if (inputStream == null)
            throw new IOException("Could not find fonts/Roboto-Regular.ttf");

        byte[] fontFileBytes = inputStream.readAllBytes();
        inputStream.close();

        ByteBuffer fontBuffer   = memAlloc(fontFileBytes.length);
        fontBuffer.put(fontFileBytes).flip();

        ByteBuffer bitmapBuffer = memAlloc(BITMAP_WIDTH * BITMAP_HEIGHT);
        stbtt_BakeFontBitmap(fontBuffer, FONT_SIZE_PX, bitmapBuffer,
                BITMAP_WIDTH, BITMAP_HEIGHT, FIRST_CHAR, bakedCharData);

        memFree(fontBuffer);

        fontTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED,
                BITMAP_WIDTH, BITMAP_HEIGHT, 0, GL_RED, GL_UNSIGNED_BYTE, bitmapBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        memFree(bitmapBuffer);
    }

    private void buildRenderingBuffers() {
        vertexArrayObjectId  = glGenVertexArrays();
        vertexBufferObjectId = glGenBuffers();

        glBindVertexArray(vertexArrayObjectId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);
        glBufferData(GL_ARRAY_BUFFER, 6 * 4 * Float.BYTES * 256, GL_DYNAMIC_DRAW);

        int strideBytes = 4 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, strideBytes, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, strideBytes, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private int buildShaderProgram() throws IOException {
        int vertexShaderId   = compileShader(GL_VERTEX_SHADER,
                loadResource("shaders/font.vert"));
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER,
                loadResource("shaders/font.frag"));

        int programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Font shader link error: "
                    + glGetProgramInfoLog(programId));

        return programId;
    }

    private String loadResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
        if (inputStream == null)
            throw new IOException("Could not find shader resource: " + resourcePath);
        return new String(inputStream.readAllBytes());
    }

    private int compileShader(int shaderType, String shaderSource) {
        int shaderId = glCreateShader(shaderType);
        glShaderSource(shaderId, shaderSource);
        glCompileShader(shaderId);
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Font shader compile error: "
                    + glGetShaderInfoLog(shaderId));
        return shaderId;
    }

    public void drawText(String text, float pixelX, float pixelY,
                         float red, float green, float blue,
                         int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glUseProgram(shaderProgramId);
        glUniform2f(glGetUniformLocation(shaderProgramId, "uScreenSize"),
                screenWidth, screenHeight);
        glUniform3f(glGetUniformLocation(shaderProgramId, "uTextColor"),
                red, green, blue);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, fontTextureId);
        glUniform1i(glGetUniformLocation(shaderProgramId, "uFontTexture"), 0);

        glBindVertexArray(vertexArrayObjectId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer cursorX = stack.floats(pixelX);
            FloatBuffer cursorY = stack.floats(pixelY);

            for (char character : text.toCharArray()) {
                if (character < FIRST_CHAR || character >= FIRST_CHAR + CHAR_COUNT) continue;

                STBTTAlignedQuad quad = STBTTAlignedQuad.malloc(stack);
                stbtt_GetBakedQuad(bakedCharData, BITMAP_WIDTH, BITMAP_HEIGHT,
                        character - FIRST_CHAR, cursorX, cursorY, quad, true);

                float[] quadVertices = {
                        quad.x0(), quad.y0(), quad.s0(), quad.t0(),
                        quad.x1(), quad.y0(), quad.s1(), quad.t0(),
                        quad.x1(), quad.y1(), quad.s1(), quad.t1(),
                        quad.x0(), quad.y0(), quad.s0(), quad.t0(),
                        quad.x1(), quad.y1(), quad.s1(), quad.t1(),
                        quad.x0(), quad.y1(), quad.s0(), quad.t1(),
                };

                glBufferSubData(GL_ARRAY_BUFFER, 0, quadVertices);
                glDrawArrays(GL_TRIANGLES, 0, 6);
            }
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBindVertexArray(0);
    }

    public void cleanup() {
        bakedCharData.free();
        glDeleteTextures(fontTextureId);
        glDeleteVertexArrays(vertexArrayObjectId);
        glDeleteBuffers(vertexBufferObjectId);
        glDeleteProgram(shaderProgramId);
    }
}