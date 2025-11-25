package demo.downloads;

import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.concurrent.Task;

import java.util.concurrent.Semaphore;

public class DownloadTask extends Task<Void> {

    private final String name;
    private final long totalBytes;
    private final int speedKBps;
    private final Semaphore limit;

    private volatile boolean paused = false;

    // propiedad observable para mostrar progreso global con precisión
    private final ReadOnlyLongWrapper downloaded = new ReadOnlyLongWrapper(this, "downloaded", 0);

    public DownloadTask(DownloadItem item, Semaphore limit) {
        this.name = item.getName();
        this.totalBytes = item.getSizeBytes();
        this.speedKBps = Math.max(1, item.getSpeedKBps());
        this.limit = limit;

        updateTitle(name);
        updateMessage("En cola...");
        updateProgress(0, totalBytes);
    }

    // getters de utilidad
    public long getTotalBytes() { return totalBytes; }
    public long getDownloadedBytes() { return downloaded.get(); }
    public ReadOnlyLongWrapper downloadedBytesProperty() { return downloaded; }

    public void pause() {
        paused = true;
        updateMessage("Pausado");
    }

    public void resumeTask() {
        if (paused) {
            synchronized (this) {
                paused = false;
                notifyAll();
            }
            updateMessage("Reanudando...");
        }
    }

    @Override
    protected Void call() throws Exception {
        // Espera cupo de concurrencia
        limit.acquire();
        try {
            long downloadedSoFar = 0L;
            final long bytesPerSecond = (long) speedKBps * 1024L;
            final long chunk = Math.max(8 * 1024L, bytesPerSecond / 10L); // ~10 updates por segundo

            long startNs = System.nanoTime();
            updateMessage("Descargando...");

            while (downloadedSoFar < totalBytes) {
                if (isCancelled()) {
                    updateMessage("Cancelado");
                    break;
                }

                // pausa cooperativa
                if (paused) {
                    synchronized (this) {
                        while (paused && !isCancelled()) {
                            wait(200);
                        }
                    }
                }
                if (isCancelled()) break;

                long remaining = totalBytes - downloadedSoFar;
                long step = Math.min(chunk, remaining);
                downloadedSoFar += step;

                // actualiza propiedades y progreso
                downloaded.set(downloadedSoFar);
                updateProgress(downloadedSoFar, totalBytes);

                // ETA simple
                double elapsedSec = (System.nanoTime() - startNs) / 1_000_000_000.0;
                double avgBps = Math.max(1.0, downloadedSoFar / Math.max(1e-6, elapsedSec));
                double eta = (totalBytes - downloadedSoFar) / avgBps;
                updateMessage(String.format("Descargando %.1f%% | ETA ~ %.1fs",
                        100.0 * downloadedSoFar / totalBytes, eta));

                // duerme según velocidad y chunk
                long sleepMs = Math.max(10, (long) (1000.0 * step / bytesPerSecond));
                Thread.sleep(sleepMs);
            }

            if (!isCancelled() && downloadedSoFar >= totalBytes) {
                downloaded.set(totalBytes);
                updateProgress(totalBytes, totalBytes);
                updateMessage("Completado");
            }
        } finally {
            limit.release();
        }
        return null;
    }
}
