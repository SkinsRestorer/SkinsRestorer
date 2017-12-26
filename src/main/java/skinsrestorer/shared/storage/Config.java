package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Config {

    // v This is a hidden feature v
    public static boolean DISABLE_ONJOIN_SKINS = false;
    public static boolean SKINWITHOUTPERM = false;
    public static int SKIN_EXPIRES_AFTER = 1;
    public static int SKIN_CHANGE_COOLDOWN = 30;
    public static boolean USE_MYSQL = false;
    public static String MYSQL_HOST = "localhost";
    public static String MYSQL_PORT = "3306";
    public static String MYSQL_DATABASE = "db";
    public static String MYSQL_SKINTABLE = "Skins";
    public static String MYSQL_PLAYERTABLE = "Skins";
    public static String MYSQL_USERNAME = "admin";
    public static String MYSQL_PASSWORD = "pass";
    public static boolean DEFAULT_SKINS_ENABLED = false;
    public static boolean DISABLED_SKINS_ENABLED = false;
    public static List<String> DEFAULT_SKINS = null;
    public static List<String> DISABLED_SKINS = null;
    public static boolean MULTIBUNGEE_ENABLED = false;
    public static boolean UPDATER_ENABLED = true;
    public static boolean AUTOUPDATE = true;
    private static YamlConfig config = new YamlConfig(
            "plugins" + File.separator + "SkinsRestorer" + File.separator + "", "config", true);

    public static void load(InputStream is) {
        config.copyDefaults(is);
        config.reload();
    }

    public static void set(String path, Object value) {
        config.set(path, value);
    }
}
