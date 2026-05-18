package com.atrialmapper;

public class DataSimulator {

    private final int vertexCount;
    private final float[] electrodeValues;
    private float simulationTime = 0f;

    // Controls how fast the wave animation moves
    private static final float ANIMATION_SPEED = 0.8f;

    public DataSimulator(int vertexCount) {
        this.vertexCount = vertexCount;
        this.electrodeValues = new float[vertexCount];
    }

    // Called every frame to advance the simulation
    public void update(float deltaTimeSeconds) {
        simulationTime += deltaTimeSeconds * ANIMATION_SPEED;

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            // Convert vertex index to a position angle on the sphere
            // This gives each vertex a unique wave phase
            float longitudeAngle = (float)(2 * Math.PI * (vertexIndex % 41) / 41.0);
            float latitudeAngle  = (float)(Math.PI * (vertexIndex / 41) / 40.0);

            // Combine two sine waves to simulate electrical wavefront propagation
            // This loosely mimics how atrial fibrillation signals travel across heart tissue
            float waveFront = (float)(
                    0.5 * Math.sin(longitudeAngle * 2 + simulationTime) +
                            0.5 * Math.sin(latitudeAngle  * 3 - simulationTime * 0.7)
            );

            // Normalize from [-1, 1] to [0, 1] for the heatmap
            electrodeValues[vertexIndex] = (waveFront + 1f) / 2f;
        }
    }

    public float[] getElectrodeValues() {
        return electrodeValues;
    }

    public int getVertexCount() {
        return vertexCount;
    }
}