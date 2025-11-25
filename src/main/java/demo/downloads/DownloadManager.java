package demo.downloads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class DownloadManager {

    private volatile Semaphore limit;
    private final ExecutorService pool;
    private final List<DownloadTask> tasks = Collections.synchronizedList(new ArrayList<>());

    public DownloadManager(int maxConcurrent) {
        this.limit = new Semaphore(Math.max(1, maxConcurrent));
        this.pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void setMaxConcurrent(int newMax) {
        if (newMax < 1) newMax = 1;
        // reemplaza el semáforo para nuevas tareas
        this.limit = new Semaphore(newMax);
    }

    // devuelve la referencia actual del semáforo para pasar a las tareas
    public Semaphore currentSemaphore() {
        return this.limit;
    }

    public DownloadTask createAndSubmit(DownloadItem item) {
        DownloadTask task = new DownloadTask(item, currentSemaphore());
        tasks.add(task);
        pool.submit(task);
        return task;
    }

    public List<DownloadTask> getTasks() {
        return tasks;
    }

    public void shutdown() {
        for (DownloadTask t : tasks) {
            t.cancel();
        }
        pool.shutdownNow();
    }
}
