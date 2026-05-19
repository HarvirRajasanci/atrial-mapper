package com.atrialmapper;

public final class MatrixMath {

    private MatrixMath() {}

    public static float[] buildProjectionMatrix(float fieldOfViewDegrees, float aspectRatio,
                                                float nearPlane, float farPlane) {
        float fieldOfViewRadians = (float) Math.toRadians(fieldOfViewDegrees);
        float frustumScale = (float)(1.0 / Math.tan(fieldOfViewRadians / 2));

        float[] projectionMatrix = new float[16];
        projectionMatrix[0]  = frustumScale / aspectRatio;
        projectionMatrix[5]  = frustumScale;
        projectionMatrix[10] = (farPlane + nearPlane) / (nearPlane - farPlane);
        projectionMatrix[11] = -1f;
        projectionMatrix[14] = (2 * farPlane * nearPlane) / (nearPlane - farPlane);
        return projectionMatrix;
    }

    public static float[] buildRotationX(float angleDegrees) {
        float angleRadians = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);
        return new float[]{
                1,   0,   0, 0,
                0,  cos, sin, 0,
                0, -sin, cos, 0,
                0,   0,   0, 1
        };
    }

    public static float[] buildRotationY(float angleDegrees) {
        float angleRadians = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);
        return new float[]{
                cos, 0, -sin, 0,
                0, 1,    0, 0,
                sin, 0,  cos, 0,
                0, 0,    0, 1
        };
    }

    public static float[] multiply(float[] matrixA, float[] matrixB) {
        float[] result = new float[16];
        for (int row = 0; row < 4; row++)
            for (int col = 0; col < 4; col++)
                for (int inner = 0; inner < 4; inner++)
                    result[row + col * 4] += matrixA[row + inner * 4] * matrixB[inner + col * 4];
        return result;
    }

    public static float[] identity() {
        return new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }
}