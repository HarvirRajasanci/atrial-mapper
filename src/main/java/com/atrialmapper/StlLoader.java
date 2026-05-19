package com.atrialmapper;

import java.io.*;
import java.nio.*;
import java.util.*;

public class StlLoader {

    public static MeshData loadFromResources(String resourceFileName) throws IOException {
        InputStream inputStream = StlLoader.class.getClassLoader()
                .getResourceAsStream(resourceFileName);
        if (inputStream == null)
            throw new FileNotFoundException("Could not find resource: " + resourceFileName);

        byte[] fileBytes = inputStream.readAllBytes();
        inputStream.close();

        if (isAsciiStl(fileBytes)) {
            return parseAsciiStl(new String(fileBytes));
        } else {
            return parseBinaryStl(fileBytes);
        }
    }

    private static boolean isAsciiStl(byte[] fileBytes) {
        if (fileBytes.length >= 84) {
            int triangleCount = ByteBuffer.wrap(fileBytes, 80, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (80 + 4 + (50L * triangleCount) == fileBytes.length) return false;
        }
        String header = new String(fileBytes, 0, Math.min(256, fileBytes.length)).trim();
        return header.toLowerCase().startsWith("solid");
    }

    private static MeshData parseAsciiStl(String fileContent) {
        List<Float> vertexData = new ArrayList<>();
        String[] lines = fileContent.split("\n");
        float[] currentNormal = new float[3];

        for (String line : lines) {
            String trimmedLine = line.trim();
            try {
                if (trimmedLine.startsWith("facet normal")) {
                    String[] tokens = trimmedLine.split("\\s+");
                    if (tokens.length < 5) continue;
                    currentNormal[0] = Float.parseFloat(tokens[2]);
                    currentNormal[1] = Float.parseFloat(tokens[3]);
                    currentNormal[2] = Float.parseFloat(tokens[4]);

                } else if (trimmedLine.startsWith("vertex")) {
                    String[] tokens = trimmedLine.split("\\s+");
                    if (tokens.length < 4) continue;
                    float x = Float.parseFloat(tokens[1]);
                    float y = Float.parseFloat(tokens[2]);
                    float z = Float.parseFloat(tokens[3]);

                    vertexData.add(x); vertexData.add(y); vertexData.add(z);
                    vertexData.add(currentNormal[0]);
                    vertexData.add(currentNormal[1]);
                    vertexData.add(currentNormal[2]);
                    vertexData.add(0.0f);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Malformed STL line, skipping: " + trimmedLine);
            }
        }

        float[] vertexArray = new float[vertexData.size()];
        for (int i = 0; i < vertexData.size(); i++) vertexArray[i] = vertexData.get(i);

        int vertexCount = vertexArray.length / 7;
        return new MeshData(normalizeToUnitScale(vertexArray, vertexCount), vertexCount);
    }

    private static MeshData parseBinaryStl(byte[] fileBytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(fileBytes).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.position(80);

        int triangleCount     = byteBuffer.getInt();
        float[] vertexArray   = new float[triangleCount * 3 * 7];
        int vertexArrayIndex  = 0;

        for (int triangleIndex = 0; triangleIndex < triangleCount; triangleIndex++) {
            float normalX = byteBuffer.getFloat();
            float normalY = byteBuffer.getFloat();
            float normalZ = byteBuffer.getFloat();

            for (int cornerIndex = 0; cornerIndex < 3; cornerIndex++) {
                float x = byteBuffer.getFloat();
                float y = byteBuffer.getFloat();
                float z = byteBuffer.getFloat();

                vertexArray[vertexArrayIndex++] = x;
                vertexArray[vertexArrayIndex++] = y;
                vertexArray[vertexArrayIndex++] = z;
                vertexArray[vertexArrayIndex++] = normalX;
                vertexArray[vertexArrayIndex++] = normalY;
                vertexArray[vertexArrayIndex++] = normalZ;
                vertexArray[vertexArrayIndex++] = 0.0f;
            }

            byteBuffer.getShort();
        }

        int vertexCount = triangleCount * 3;
        return new MeshData(normalizeToUnitScale(vertexArray, vertexCount), vertexCount);
    }

    private static float[] normalizeToUnitScale(float[] vertexArray, int vertexCount) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int baseIndex = vertexIndex * 7;
            float x = vertexArray[baseIndex];
            float y = vertexArray[baseIndex + 1];
            float z = vertexArray[baseIndex + 2];

            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
        }

        float centerX    = (minX + maxX) / 2f;
        float centerY    = (minY + maxY) / 2f;
        float centerZ    = (minZ + maxZ) / 2f;
        float maxExtent  = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        float scaleFactor = 2.0f / maxExtent;

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int baseIndex = vertexIndex * 7;
            vertexArray[baseIndex]     = (vertexArray[baseIndex]     - centerX) * scaleFactor;
            vertexArray[baseIndex + 1] = (vertexArray[baseIndex + 1] - centerY) * scaleFactor;
            vertexArray[baseIndex + 2] = (vertexArray[baseIndex + 2] - centerZ) * scaleFactor;
        }

        return vertexArray;
    }
}