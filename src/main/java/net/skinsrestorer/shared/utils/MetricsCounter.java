package net.skinsrestorer.shared.utils;


/**
 * Created by McLive on 09.02.2019.
 */
public class MetricsCounter {
    private static int mineskinCalls = 0;
    private static int minetoolsCalls = 0;
    private static int mojangCalls = 0;
    private static int backupCalls = 0;

    private MetricsCounter() {}

    public static int collectMineskinCalls() {
        int value = MetricsCounter.mineskinCalls;
        MetricsCounter.mineskinCalls = 0;
        return value;
    }

    public static void incrMineskinCalls() {
        MetricsCounter.mineskinCalls = MetricsCounter.mineskinCalls + 1;
    }

    public static int collectMinetoolsCalls() {
        int value = MetricsCounter.minetoolsCalls;
        MetricsCounter.minetoolsCalls = 0;
        return value;
    }

    public static void incrMinetoolsCalls() {
        MetricsCounter.minetoolsCalls = MetricsCounter.minetoolsCalls + 1;
    }

    public static int collectMojangCalls() {
        int value = MetricsCounter.mojangCalls;
        MetricsCounter.mojangCalls = 0;
        return value;
    }

    public static void incrMojangCalls() {
        MetricsCounter.mojangCalls = MetricsCounter.mojangCalls + 1;
    }

    public static int collectBackupCalls() {
        int value = MetricsCounter.backupCalls;
        MetricsCounter.backupCalls = 0;
        return value;
    }

    public static void incrBackupCalls() {
        MetricsCounter.backupCalls = MetricsCounter.backupCalls + 1;
    }

    public static void incrAPI(String url) {
        if (url.startsWith("https://api.mineskin.org/"))
            incrMineskinCalls();

        if (url.startsWith("https://api.minetools.eu/"))
            incrMinetoolsCalls();

        if (url.startsWith("https://api.mojang.com/") || url.startsWith("https://sessionserver.mojang.com/"))
            incrMojangCalls();

        if (url.startsWith("https://api.ashcon.app/"))
            incrBackupCalls();
    }
}
