package com.atrialmapper;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL33.*;

public class ColorLegend {

    private int vertexArrayObjectId;
    private int vertexBufferObjectId;
    private int shaderProgramId;
    private FontRenderer fontRenderer;

    private final int screenWidth;
    private final int screenHeight;

    private static final int   GRADIENT_STEPS      = 100;
    private static final float BAR_LEFT_FRACTION   = 1180f / 1280f;
    private static final float BAR_RIGHT_FRACTION  = 1210f / 1280f;
    private static final float BAR_TOP_FRACTION    =   80f /  720f;
    private static final float BAR_BOTTOM_FRACTION =  640f /  720f;
    private static final float LABEL_X_FRACTION    = 1120f / 1280f;

    private final float barLeft;
    private final float barRight;
    private final float barTop;
    private final float barBottom;
    private final float labelX;

    public ColorLegend(int screenWidth, int screenHeight) throws IOException {
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        this.barLeft   = screenWidth  * BAR_LEFT_FRACTION;
        this.barRight  = screenWidth  * BAR_RIGHT_FRACTION;
        this.barTop    = screenHeight * BAR_TOP_FRACTION;
        this.barBottom = screenHeight * BAR_BOTTOM_FRACTION;
        this.labelX    = screenWidth  * LABEL_X_FRACTION;
        shaderProgramId   = ShaderCompiler.buildProgram("shaders/legend.vert", "shaders/legend.frag");
        fontRenderer = new FontRenderer();
        if (fontRenderer == null)
            throw new IllegalStateException("FontRenderer must not be null");
        buildLegendMesh();
    }

    private void buildLegendMesh() {
        float[] vertexData  = new float[GRADIENT_STEPS * 6 * 5];
        int vertexDataIndex = 0;

        for (int stepIndex = 0; stepIndex < GRADIENT_STEPS; stepIndex++) {
            float bottomValue = (float) stepIndex / GRADIENT_STEPS;
            float topValue    = (float)(stepIndex + 1) / GRADIENT_STEPS;

            float bottomY = barTop + (barBottom - barTop) * (1f - bottomValue);
            float topY    = barTop + (barBottom - barTop) * (1f - topValue);

            float[] bottomColor = heatmapColor(bottomValue);
            float[] topColor    = heatmapColor(topValue);

            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barLeft,  bottomY, bottomColor);
            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barRight, bottomY, bottomColor);
            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barRight, topY,    topColor);
            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barLeft,  bottomY, bottomColor);
            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barRight, topY,    topColor);
            vertexDataIndex = addVertex(vertexData, vertexDataIndex, barLeft,  topY,    topColor);
        }

        vertexArrayObjectId  = glGenVertexArrays();
        vertexBufferObjectId = glGenBuffers();

        glBindVertexArray(vertexArrayObjectId);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertexBuffer = stack.mallocFloat(vertexData.length);
            vertexBuffer.put(vertexData).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        }

        int strideBytes = 5 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, strideBytes, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, strideBytes, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private int addVertex(float[] vertexData, int index,
                          float x, float y, float[] color) {
        vertexData[index++] = x;
        vertexData[index++] = y;
        vertexData[index++] = color[0];
        vertexData[index++] = color[1];
        vertexData[index++] = color[2];
        return index;
    }

    public void draw() {
        glDisable(GL_DEPTH_TEST);

        glUseProgram(shaderProgramId);
        glUniform2f(glGetUniformLocation(shaderProgramId, "uScreenSize"),
                screenWidth, screenHeight);
        glBindVertexArray(vertexArrayObjectId);
        glDrawArrays(GL_TRIANGLES, 0, GRADIENT_STEPS * 6);
        glBindVertexArray(0);

        // Title
        fontRenderer.drawText("mV", labelX + 20f, barTop - 15f,
                1f, 1f, 1f, screenWidth, screenHeight);

        // Tick labels mapped to realistic atrial voltage range
        // < 0.5 mV = scar tissue (blue), > 1.5 mV = healthy tissue (red)
        drawTickLabel("1.5", 1.0f);
        drawTickLabel("1.1", 0.75f);
        drawTickLabel("0.8", 0.5f);
        drawTickLabel("0.4", 0.25f);
        drawTickLabel("0.0", 0.0f);

        glEnable(GL_DEPTH_TEST);
    }

    private void drawTickLabel(String labelText, float normalizedValue) {
        float pixelY = barTop + (barBottom - barTop) * (1f - normalizedValue) + 5f;
        fontRenderer.drawText(labelText, labelX, pixelY,
                0.9f, 0.9f, 0.9f, screenWidth, screenHeight);
    }

    private float[] heatmapColor(float value) {
        value = Math.clamp(value, 0f, 1f);
        float red   = smoothstep(0.5f, 1.0f, value);
        float green = smoothstep(0.0f, 0.5f, value) - smoothstep(0.75f, 1.0f, value);
        float blue  = 1.0f - smoothstep(0.0f, 0.5f, value);
        return new float[]{ red, green, blue };
    }

    private float smoothstep(float edge0, float edge1, float value) {
        float t = Math.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public void cleanup() {
        fontRenderer.cleanup();
        glDeleteVertexArrays(vertexArrayObjectId);
        glDeleteBuffers(vertexBufferObjectId);
        glDeleteProgram(shaderProgramId);
    }
}