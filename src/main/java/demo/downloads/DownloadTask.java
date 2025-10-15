package demo.downloads;

import javafx.concurrent.Task;
import java.util.concurrent.Semaphore;

public class DownloadTask extends Task<Void> {

    private final DownloadItem item;
    private final Semaphore limit; // control de concurrencia compartido
    private volatile boolean paused = false;

    public DownloadTask(DownloadItem item, Semaphore limit) {
        this.item = item;
        this.limit = limit;
        updateTitle(item.getName());
        updateMessage("En cola…");
    }

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
            updateMessage("Reanudando…");
        }
    }

    @Override
    protected Void call() throws Exception {
        // Adquiere un cupo de concurrencia
        limit.acquire();
        try {
            final long total = item.getSizeBytes();
            long downloaded = 0L;

            // paso en bytes por iteración (simula "chunks")
            int speedKBps = Math.max(1, item.getSpeedKBps());
            long bytesPerSecond = speedKBps * 1024L;
            long chunk = Math.max(8 * 1024L, bytesPerSecond / 10); // ~10 updates/seg

            long startNanos = System.nanoTime();
            updateMessage("Descargando…");

            while (downloaded < total) {
                if (isCancelled()) {
                    updateMessage("Cancelado");
                    break;
                }

                // Manejo de pausa
                if (paused) {
                    synchronized (this) {
                        while (paused && !isCancelled()) {
                            wait(200);
                        }
                    }
                }
                if (isCancelled()) break;

                long remaining = total - downloaded;
                long step = Math.min(chunk, remaining);
                downloaded += step;

                // Progreso (0.0 a 1.0)
                updateProgress(downloaded, total);

                // ETA estimada
                double elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
                double avgBps = downloaded / Math.max(1e-6, elapsedSec);
                double etaSec = (total - downloaded) / Math.max(1.0, avgBps);
                updateMessage(String.format("Descargando… %.1f%% | ETA ~ %.1fs",
                        100.0 * downloaded / total, etaSec));

                // Simular tiempo en función de la velocidad
                long sleepMs = Math.max(10, (long) (1000.0 * step / bytesPerSecond));
                Thread.sleep(sleepMs);
            }

            if (!isCancelled()) {
                updateProgress(total, total);
                updateMessage("Completado");
            }
        } finally {
            // Libera el cupo de concurrencia
            limit.release();
        }
        return null;
    }
}
