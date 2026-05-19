package com.atrialmapper;

import java.util.concurrent.atomic.AtomicReference;

public class ElectrodeDataStream {

    private final AtomicReference<float[]> latestElectrodeValues;
    private final DataSimulator dataSimulator;
    private final Thread producerThread;
    private volatile boolean isRunning = false;

    private static final int UPDATE_INTERVAL_MS = 16;

    public ElectrodeDataStream(int vertexCount, float[] cachedVertexData) {
        this.dataSimulator = new DataSimulator(vertexCount, cachedVertexData);
        this.latestElectrodeValues = new AtomicReference<>(new float[vertexCount]);
        this.producerThread = new Thread(this::runProducerLoop, "electrode-data-producer");
        this.producerThread.setDaemon(true);
    }

    public void start() {
        isRunning = true;
        producerThread.start();
    }

    public void stop() {
        isRunning = false;
        try {
            producerThread.join(2000);
            if (producerThread.isAlive())
                System.err.println("Warning: electrode data thread did not terminate");
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void runProducerLoop() {
        while (isRunning) {
            long frameStartTime = System.currentTimeMillis();

            dataSimulator.update(UPDATE_INTERVAL_MS / 1000f);

            float[] currentValues = dataSimulator.getElectrodeValues();
            float[] snapshot = new float[currentValues.length];
            System.arraycopy(currentValues, 0, snapshot, 0, currentValues.length);

            latestElectrodeValues.set(snapshot);

            long elapsed = System.currentTimeMillis() - frameStartTime;
            long sleepTime = UPDATE_INTERVAL_MS - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public float[] pollLatestValues() {
        return latestElectrodeValues.get();
    }
}