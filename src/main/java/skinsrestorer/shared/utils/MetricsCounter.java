package skinsrestorer.shared.utils;


/**
 * Created by McLive on 09.02.2019.
 */
public class MetricsCounter {
    private static int mineskin_calls = 0;
    private static int minetools_calls = 0;
    private static int mojang_calls = 0;
    private static int backup_calls = 0;

    public static int getMineskin_calls() {
        return MetricsCounter.mineskin_calls;
    }

    public static int collectMineskin_calls() {
        int value = MetricsCounter.mineskin_calls;
        MetricsCounter.mineskin_calls = 0;
        return value;
    }

    public static void setMineskin_calls(int mineskin_calls) {
        MetricsCounter.mineskin_calls = mineskin_calls;
    }

    public static void incrMineskin_calls() {
        MetricsCounter.mineskin_calls = MetricsCounter.mineskin_calls + 1;
    }


    public static int getMinetools_calls() {
        return MetricsCounter.minetools_calls;
    }

    public static int collectMinetools_calls() {
        int value = MetricsCounter.minetools_calls;
        MetricsCounter.minetools_calls = 0;
        return value;
    }

    public static void setMinetools_calls(int minetools_calls) {
        MetricsCounter.minetools_calls = minetools_calls;
    }

    public static void incrMinetools_calls() {
        MetricsCounter.minetools_calls = MetricsCounter.minetools_calls + 1;
    }

    public static int getMojang_calls() {
        return MetricsCounter.mojang_calls;
    }

    public static int collectMojang_calls() {
        int value = MetricsCounter.mojang_calls;
        MetricsCounter.mojang_calls = 0;
        return value;
    }

    public static void setMojang_calls(int mojang_calls) {
        MetricsCounter.mojang_calls = mojang_calls;
    }

    public static void incrMojang_calls() {
        MetricsCounter.mojang_calls = MetricsCounter.mojang_calls + 1;
    }

    public static int getBackup_calls() {
        return MetricsCounter.backup_calls;
    }

    public static int collectBackup_calls() {
        int value = MetricsCounter.backup_calls;
        MetricsCounter.backup_calls = 0;
        return value;
    }

    public static void setBackup_calls(int backup_calls) {
        MetricsCounter.backup_calls = backup_calls;
    }

    public static void incrBackup_calls() {
        MetricsCounter.backup_calls = MetricsCounter.backup_calls + 1;
    }

    public static void incrAPI(String url) {
        if (url.startsWith("https://api.mineskin.org/"))
            incrMineskin_calls();

        if (url.startsWith("https://api.minetools.eu/"))
            incrMinetools_calls();

        if (url.startsWith("https://api.mojang.com/") || url.startsWith("https://sessionserver.mojang.com/"))
            incrMojang_calls();

        if (url.startsWith("https://api.ashcon.app/"))
            incrBackup_calls();
    }
}
