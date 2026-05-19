package com.atrialmapper;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

public class Mesh {

    private enum MeshType { INDEXED, FLAT }

    private int vertexArrayObjectId;
    private int vertexBufferObjectId;
    private int elementBufferObjectId;
    private int indexCount;
    private int vertexCount = 0;
    private float[] cachedVertexData;
    private MeshType meshType;

    private static final int FLOATS_PER_VERTEX = 7;
    private static final int STRIDE_BYTES      = FLOATS_PER_VERTEX * Float.BYTES;

    private static final int ATTRIB_POSITION = 0;
    private static final int ATTRIB_NORMAL   = 1;
    private static final int ATTRIB_VALUE    = 2;

    // Constructor 1 — procedural sphere
    public Mesh(int stackCount, int sliceCount) {
        meshType           = MeshType.INDEXED;
        float[] vertexData = buildSphereVertexData(stackCount, sliceCount);
        int[]   indexData  = buildSphereIndexData(stackCount, sliceCount);
        indexCount         = indexData.length;
        uploadIndexedMeshToGpu(vertexData, indexData);
    }

    // Constructor 2 — from loaded mesh data
    public Mesh(MeshData meshData) {
        meshType = MeshType.FLAT;
        uploadFlatMeshToGpu(meshData.vertexData, meshData.vertexCount);
    }

    private float[] buildSphereVertexData(int stackCount, int sliceCount) {
        List<Float> vertices = new ArrayList<>();

        for (int stackIndex = 0; stackIndex <= stackCount; stackIndex++) {
            float phi = (float)(Math.PI * stackIndex / stackCount);

            for (int sliceIndex = 0; sliceIndex <= sliceCount; sliceIndex++) {
                float theta = (float)(2 * Math.PI * sliceIndex / sliceCount);

                float x = (float)(Math.sin(phi) * Math.cos(theta));
                float y = (float)(Math.cos(phi));
                float z = (float)(Math.sin(phi) * Math.sin(theta));

                vertices.add(x); vertices.add(y); vertices.add(z);
                vertices.add(x); vertices.add(y); vertices.add(z);
                vertices.add(0.0f);
            }
        }

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) vertexArray[i] = vertices.get(i);
        return vertexArray;
    }

    private int[] buildSphereIndexData(int stackCount, int sliceCount) {
        List<Integer> indices = new ArrayList<>();

        for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
            for (int sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++) {
                int topLeft    = stackIndex * (sliceCount + 1) + sliceIndex;
                int bottomLeft = topLeft + sliceCount + 1;

                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topLeft + 1);

                indices.add(bottomLeft);
                indices.add(bottomLeft + 1);
                indices.add(topLeft + 1);
            }
        }

        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    private void uploadIndexedMeshToGpu(float[] vertexData, int[] indexData) {
        vertexArrayObjectId   = glGenVertexArrays();
        vertexBufferObjectId  = glGenBuffers();
        elementBufferObjectId = glGenBuffers();

        glBindVertexArray(vertexArrayObjectId);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBufferObjectId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

        setupVertexAttributes();
        glBindVertexArray(0);

        this.cachedVertexData = vertexData;
    }

    private void uploadFlatMeshToGpu(float[] vertexData, int vertexCount) {
        vertexArrayObjectId  = glGenVertexArrays();
        vertexBufferObjectId = glGenBuffers();

        glBindVertexArray(vertexArrayObjectId);

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW);

        setupVertexAttributes();
        glBindVertexArray(0);

        this.vertexCount      = vertexCount;
        this.cachedVertexData = vertexData;
    }

    private void setupVertexAttributes() {
        glVertexAttribPointer(ATTRIB_POSITION, 3, GL_FLOAT, false, STRIDE_BYTES, 0);
        glEnableVertexAttribArray(ATTRIB_POSITION);

        glVertexAttribPointer(ATTRIB_NORMAL, 3, GL_FLOAT, false, STRIDE_BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(ATTRIB_NORMAL);

        glVertexAttribPointer(ATTRIB_VALUE, 1, GL_FLOAT, false, STRIDE_BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(ATTRIB_VALUE);
    }

    public void updateVertexValues(float[] updatedValues) {
        if (updatedValues == null || cachedVertexData == null) return;
        if ((long) updatedValues.length * FLOATS_PER_VERTEX > cachedVertexData.length)
            throw new IllegalArgumentException("Updated values exceed vertex count");

        for (int vertexIndex = 0; vertexIndex < updatedValues.length; vertexIndex++) {
            cachedVertexData[vertexIndex * FLOATS_PER_VERTEX + 6] = updatedValues[vertexIndex];
        }

        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObjectId);
        glBufferData(GL_ARRAY_BUFFER, cachedVertexData, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        glBindVertexArray(vertexArrayObjectId);
        switch (meshType) {
            case INDEXED -> glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            case FLAT    -> glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vertexArrayObjectId);
        glDeleteBuffers(vertexBufferObjectId);
        glDeleteBuffers(elementBufferObjectId);
    }
}