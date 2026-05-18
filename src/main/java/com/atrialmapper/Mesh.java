package com.atrialmapper;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class Mesh {

    private int vertexArrayObjectId;
    private int vertexBufferObjectId;
    private int elementBufferObjectId;
    private int indexCount;

    // Each vertex has: position (xyz), normal (xyz), value (float) = 7 floats
    private static final int FLOATS_PER_VERTEX = 7;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    // Attribute layout locations (must match vertex shader)
    private static final int ATTRIB_POSITION = 0;
    private static final int ATTRIB_NORMAL = 1;
    private static final int ATTRIB_VALUE = 2;

    public Mesh(int stackCount, int sliceCount) {
        float[] vertexData = buildVertexData(stackCount, sliceCount);
        int[] indexData = buildIndexData(stackCount, sliceCount);
        indexCount = indexData.length;
        uploadToGpu(vertexData, indexData);
    }

    private float[] buildVertexData(int stackCount, int sliceCount) {
        List<Float> vertices = new ArrayList<>();

        for (int stackIndex = 0; stackIndex <= stackCount; stackIndex++) {
            float phi = (float)(Math.PI * stackIndex / stackCount);

            for (int sliceIndex = 0; sliceIndex <= sliceCount; sliceIndex++) {
                float theta = (float)(2 * Math.PI * sliceIndex / sliceCount);

                // Position on unit sphere
                float x = (float)(Math.sin(phi) * Math.cos(theta));
                float y = (float)(Math.cos(phi));
                float z = (float)(Math.sin(phi) * Math.sin(theta));

                // Position
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);

                // Normal (same as position on a unit sphere)
                vertices.add(x);
                vertices.add(y);
                vertices.add(z);

                // Electrode value — starts at 0, updated each frame by the data stream
                vertices.add(0.0f);
            }
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) vertexArray[i] = vertices.get(i);
        return vertexArray;
    }

    private int[] buildIndexData(int stackCount, int sliceCount) {
        List<Integer> indices = new ArrayList<>();

        for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
            for (int sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++) {
                int topLeft = stackIndex * (sliceCount + 1) + sliceIndex;
                int bottomLeft = topLeft + sliceCount + 1;

                // First triangle of quad
                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topLeft + 1);

                // Second triangle of quad
                indices.add(bottomLeft);
                indices.add(bottomLeft + 1);
                indices.add(topLeft + 1);
            }
        }

        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private void uploadToGpu(float[] vertexData, int[] indexData) {
        vertexArrayObjectId = glGenVertexArrays();
        vertexBufferObjectId = glGenBuffers();
        elementBufferObjectId = glGenBuffers();

        glBindVertexArray(vertexArrayObjectId);

        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);

        // Upload index data
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBufferObjectId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

        // Tell OpenGL how to read position from the buffer
        glVertexAttribPointer(ATTRIB_POSITION, 3, GL_FLOAT, false, STRIDE_BYTES, 0);
        glEnableVertexAttribArray(ATTRIB_POSITION);

        // Tell OpenGL how to read normal from the buffer
        glVertexAttribPointer(ATTRIB_NORMAL, 3, GL_FLOAT, false, STRIDE_BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(ATTRIB_NORMAL);

        // Tell OpenGL how to read electrode value from the buffer
        glVertexAttribPointer(ATTRIB_VALUE, 1, GL_FLOAT, false, STRIDE_BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(ATTRIB_VALUE);

        glBindVertexArray(0);
    }

    public void updateVertexValues(float[] updatedValues) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);

        // Only re-upload the value float for each vertex, not the whole buffer
        for (int vertexIndex = 0; vertexIndex < updatedValues.length; vertexIndex++) {
            int byteOffset = vertexIndex * STRIDE_BYTES + 6 * Float.BYTES;
            glBufferSubData(GL_ARRAY_BUFFER, byteOffset, new float[]{ updatedValues[vertexIndex] });
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        glBindVertexArray(vertexArrayObjectId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vertexArrayObjectId);
        glDeleteBuffers(vertexBufferObjectId);
        glDeleteBuffers(elementBufferObjectId);
    }
}