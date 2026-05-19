package com.atrialmapper;

public class DataSimulator {

    private final int vertexCount;
    private final float[] electrodeValues;
    private final float[] vertexPositions; // x, y, z per vertex
    private float simulationTime = 0f;

    private static final float ANIMATION_SPEED = 0.5f;

    public DataSimulator(int vertexCount, float[] cachedVertexData) {
        this.vertexCount = vertexCount;
        this.electrodeValues = new float[vertexCount];

        // Extract just the positions (first 3 floats of every 7)
        this.vertexPositions = new float[vertexCount * 3];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int sourceIndex = vertexIndex * 7;
            int destIndex   = vertexIndex * 3;
            vertexPositions[destIndex]     = cachedVertexData[sourceIndex];
            vertexPositions[destIndex + 1] = cachedVertexData[sourceIndex + 1];
            vertexPositions[destIndex + 2] = cachedVertexData[sourceIndex + 2];
        }
    }

    public void update(float deltaTimeSeconds) {
        simulationTime += deltaTimeSeconds * ANIMATION_SPEED;

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int positionIndex = vertexIndex * 3;
            float x = vertexPositions[positionIndex];
            float y = vertexPositions[positionIndex + 1];
            float z = vertexPositions[positionIndex + 2];

            // Origin shifted to front-center of the heart
            // which is where the sinoatrial node sits anatomically
            float distanceFromOrigin = (float) Math.sqrt(
                    x * x +
                            y * y +
                            (z - 1f) * (z - 1f)  // offset along Z since model is oriented that way
            );

            float activationWave = (float)(
                    Math.sin(distanceFromOrigin * 3.0 - simulationTime * 2.0)
            );

            float reentrantWave = (float)(
                    0.3 * Math.sin(distanceFromOrigin * 6.0 + simulationTime * 1.5 + y * 2.0)
            );

            float combinedSignal = activationWave + reentrantWave;
            electrodeValues[vertexIndex] = (combinedSignal + 1.3f) / 2.6f;
        }
    }

    public float[] getElectrodeValues() {
        return electrodeValues;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}