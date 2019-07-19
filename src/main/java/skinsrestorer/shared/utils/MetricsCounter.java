package skinsrestorer.shared.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by McLive on 09.02.2019.
 */
public class MetricsCounter {
    private static int minetools_calls = 0;
    private static int mojang_calls = 0;
    private static int backup_calls = 0;

    public static int getMinetools_calls() {
        return minetools_calls;
    }

    public static int collectMinetools_calls() {
        int value = minetools_calls;
        minetools_calls = 0;
        return value;
    }

    public static void setMinetools_calls(int minetools_calls) {
        minetools_calls = minetools_calls;
    }

    public static void incrMinetools_calls() {
        minetools_calls = minetools_calls + 1;
    }

    public static int getMojang_calls() {
        return mojang_calls;
    }

    public static int collectMojang_calls() {
        int value = mojang_calls;
        mojang_calls = 0;
        return value;
    }

    public static void setMojang_calls(int mojang_calls) {
        mojang_calls = mojang_calls;
    }

    public static void incrMojang_calls() {
        mojang_calls = mojang_calls + 1;
    }

    public static int getBackup_calls() {
        return backup_calls;
    }

    public static int collectBackup_calls() {
        int value = backup_calls;
        backup_calls = 0;
        return value;
    }

    public static void setBackup_calls(int backup_calls) {
        backup_calls = backup_calls;
    }

    public static void incrBackup_calls() {
        backup_calls = backup_calls + 1;
    }

    public static void incrAPI(String url) {
        if (url.startsWith("https://api.minetools.eu/"))
            incrMinetools_calls();

        if (url.startsWith("https://api.mojang.com/") || url.startsWith("https://sessionserver.mojang.com/"))
            incrMojang_calls();

        if (url.startsWith("https://api.ashcon.app/"))
            incrBackup_calls();
    }
}
