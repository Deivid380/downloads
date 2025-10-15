package demo.downloads;

public class DownloadItem {
    private final String name;
    private final long sizeBytes;      // tamaño simulado
    private final int speedKBps;       // velocidad simulada

    public DownloadItem(String name, long sizeBytes, int speedKBps) {
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.speedKBps = speedKBps;
    }

    public String getName() { return name; }
    public long getSizeBytes() { return sizeBytes; }
    public int getSpeedKBps() { return speedKBps; }
}
