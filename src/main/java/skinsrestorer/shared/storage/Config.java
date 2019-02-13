package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Config {

    public static boolean DISABLE_ONJOIN_SKINS = false; // hidden
    public static boolean SKINWITHOUTPERM = false;
    public static int SKIN_EXPIRES_AFTER = 1584;
    public static int SKIN_CHANGE_COOLDOWN = 30;
    public static boolean DEFAULT_SKINS_ENABLED = false;
    public static boolean DEFAULT_SKINS_PREMIUM = false;
    public static List<String> DEFAULT_SKINS = null;
    public static boolean DISABLED_SKINS_ENABLED = false;
    public static List<String> DISABLED_SKINS = null;
    public static boolean MULTIBUNGEE_ENABLED = false;
    public static boolean USE_MYSQL = false;
    public static String MYSQL_HOST = "localhost";
    public static String MYSQL_PORT = "3306";
    public static String MYSQL_DATABASE = "db";
    public static String MYSQL_SKINTABLE = "Skins";
    public static String MYSQL_PLAYERTABLE = "Players";
    public static String MYSQL_USERNAME = "admin";
    public static String MYSQL_PASSWORD = "pass";
    public static boolean UPDATER_ENABLED = true;
    public static boolean UPDATER_PERIODIC = true;
    public static boolean NO_SKIN_IF_LOGIN_CANCELED = true;
    public static boolean USE_NEW_PERMISSIONS = false;
    public static boolean PER_SKIN_PERMISSIONS = false;

    // UPCOMING MULTIPLE LANGUAGE SUPPORT
    public static String LOCALE_FILE = "english.yml";

    private static YamlConfig config = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "config", false);

    public static void load(InputStream is) {
        config.saveDefaultConfig(is);
        config.reload();
        DISABLE_ONJOIN_SKINS = config.getBoolean("DisableOnJoinSkins", DISABLE_ONJOIN_SKINS); //hidden
        SKINWITHOUTPERM = config.getBoolean("SkinWithoutPerm", SKINWITHOUTPERM);
        SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown", SKIN_CHANGE_COOLDOWN);
        SKIN_EXPIRES_AFTER = config.getInt("SkinExpiresAfter", SKIN_EXPIRES_AFTER);
        DEFAULT_SKINS_ENABLED = config.getBoolean("DefaultSkins.Enabled", DEFAULT_SKINS_ENABLED);
        DEFAULT_SKINS_PREMIUM = config.getBoolean("DefaultSkins.ApplyForPremium", DEFAULT_SKINS_PREMIUM);
        DISABLED_SKINS_ENABLED = config.getBoolean("DisabledSkins.Enabled", DISABLED_SKINS_ENABLED);
        MULTIBUNGEE_ENABLED = config.getBoolean("MultiBungee.Enabled", MULTIBUNGEE_ENABLED);
        USE_MYSQL = config.getBoolean("MySQL.Enabled", USE_MYSQL);
        MYSQL_HOST = config.getString("MySQL.Host", MYSQL_HOST);
        MYSQL_PORT = config.getString("MySQL.Port", MYSQL_PORT);
        MYSQL_DATABASE = config.getString("MySQL.Database", MYSQL_DATABASE);
        MYSQL_SKINTABLE = config.getString("MySQL.SkinTable", MYSQL_SKINTABLE);
        MYSQL_PLAYERTABLE = config.getString("MySQL.PlayerTable", MYSQL_PLAYERTABLE);
        MYSQL_USERNAME = config.getString("MySQL.Username", MYSQL_USERNAME);
        MYSQL_PASSWORD = config.getString("MySQL.Password", MYSQL_PASSWORD);
        UPDATER_ENABLED = config.getBoolean("Updater.Enabled");
        UPDATER_PERIODIC = config.getBoolean("Updater.PeriodicChecks", UPDATER_PERIODIC);
        SKINWITHOUTPERM = config.getBoolean("SkinWithoutPerm");
        DEFAULT_SKINS = config.getStringList("DefaultSkins.Names");
        DISABLED_SKINS = config.getStringList("DisabledSkins.Names");
        USE_NEW_PERMISSIONS = config.getBoolean("Permissions.NewPermissions", USE_NEW_PERMISSIONS);
        PER_SKIN_PERMISSIONS = config.getBoolean("PerSkinPermissions", PER_SKIN_PERMISSIONS);

        // Permissions
        NO_SKIN_IF_LOGIN_CANCELED = config.getBoolean("NoSkinIfLoginCanceled", NO_SKIN_IF_LOGIN_CANCELED);

        if (PER_SKIN_PERMISSIONS && !USE_NEW_PERMISSIONS)
            System.out.println("[SkinsRestorer] Warning: PerSkinPermissions only work with Permissions.NewPermissions set to true!");
    }

    public static void set(String path, Object value) {
        config.set(path, value);
    }
}
