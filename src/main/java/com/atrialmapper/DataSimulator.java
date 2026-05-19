package com.atrialmapper;

public class DataSimulator {

    private final int vertexCount;
    private final float[] electrodeValues;
    private final float[] vertexPositions; // x, y, z per vertex
    private float simulationTime = 0f;

    private static final float ANIMATION_SPEED             = 0.5f;
    private static final float ACTIVATION_WAVE_FREQUENCY   = 3.0f;
    private static final float ACTIVATION_WAVE_SPEED       = 2.0f;
    private static final float REENTRANT_WAVE_FREQUENCY    = 6.0f;
    private static final float REENTRANT_WAVE_SPEED        = 1.5f;
    private static final float REENTRANT_WAVE_AMPLITUDE    = 0.3f;
    private static final float REENTRANT_SPATIAL_SPREAD    = 2.0f;
    private static final float NORMALIZATION_OFFSET        = 1.3f;
    private static final float NORMALIZATION_SCALE         = 2.6f;

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
                    Math.sin(distanceFromOrigin * ACTIVATION_WAVE_FREQUENCY
                            - simulationTime * ACTIVATION_WAVE_SPEED)
            );

            float reentrantWave = (float)(
                    REENTRANT_WAVE_AMPLITUDE * Math.sin(
                            distanceFromOrigin * REENTRANT_WAVE_FREQUENCY
                            + simulationTime * REENTRANT_WAVE_SPEED
                            + y * REENTRANT_SPATIAL_SPREAD)
            );

            float combinedSignal = activationWave + reentrantWave;
            electrodeValues[vertexIndex] = (combinedSignal + NORMALIZATION_OFFSET) / NORMALIZATION_SCALE;
        }
    }

    public float[] getElectrodeValues() {
        return electrodeValues;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}