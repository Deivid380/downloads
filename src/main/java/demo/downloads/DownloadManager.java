package demo.downloads;

import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class DownloadManager {

    private final Semaphore limit;
    private final ExecutorService pool;
    private final List<DownloadTask> tasks = new ArrayList<>();

    public DownloadManager(int maxConcurrent) {
        this.limit = new Semaphore(Math.max(1, maxConcurrent));
        // pool con más hilos que el límite: el semáforo es quien “decide”
        this.pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public DownloadTask createAndSubmit(DownloadItem item) {
        DownloadTask task = new DownloadTask(item, limit);
        tasks.add(task);
        pool.submit(task); // se pone a ejecutar (respetará el semáforo al iniciar)
        return task;
    }

    public List<DownloadTask> getTasks() {
        return tasks;
    }

    public void shutdown() {
        for (Task<?> t : tasks) {
            t.cancel();
        }
        pool.shutdownNow();
    }
}
