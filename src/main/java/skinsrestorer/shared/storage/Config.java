package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Config {

    public static boolean SKINWITHOUTPERM = false;
    public static int SKIN_CHANGE_COOLDOWN = 30;
    public static int SKIN_ERROR_COOLDOWN = 5;
    public static boolean DEFAULT_SKINS_ENABLED = false;
    public static boolean DEFAULT_SKINS_PREMIUM = false;
    public static List<String> DEFAULT_SKINS = null;
    public static boolean DISABLED_SKINS_ENABLED = false;
    public static List<String> DISABLED_SKINS = null;
    public static boolean CUSTOM_GUI_ENABLED = false;
    public static boolean CUSTOM_GUI_ONLY = false;
    public static List<String> CUSTOM_GUI_SKINS = null;
    public static boolean DISABLE_PREFIX = true; //TODO: turn false after a few updates
    public static boolean USE_OLD_SKIN_HELP = false;
    public static boolean PER_SKIN_PERMISSIONS = false;
    public static int SKIN_EXPIRES_AFTER = 20;
    public static boolean MULTIBUNGEE_ENABLED = false;
    public static boolean USE_MYSQL = false;
    public static String MYSQL_HOST = "localhost";
    public static String MYSQL_PORT = "3306";
    public static String MYSQL_DATABASE = "db";
    public static String MYSQL_USERNAME = "root";
    public static String MYSQL_PASSWORD = "pass";
    public static String MYSQL_SKINTABLE = "Skins";
    public static String MYSQL_PLAYERTABLE = "Players";
    public static String MYSQL_CONNECTIONOPTIONS = "verifyServerCertificate=false&useSSL=false&serverTimezone=UTC";
    public static boolean DISABLE_ONJOIN_SKINS = false; // hidden
    public static boolean NO_SKIN_IF_LOGIN_CANCELED = true;
    public static boolean UPDATER_ENABLED = true;
    public static boolean UPDATER_PERIODIC = true;
    public static boolean DEBUG = false;
    public static boolean DISMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean REMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean DISMOUNT_PASSENGERS_ON_UPDATE = false;


    // UPCOMING MULTIPLE LANGUAGE SUPPORT
    public static String LOCALE_FILE = "english.yml";

    // private static YamlConfig config = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "config", false);
    private static YamlConfig config;

    public static void load(String path, InputStream is) {
        config = new YamlConfig(path + File.separator, "config", false);
        config.saveDefaultConfig(is);
        config.reload();
        SKINWITHOUTPERM = config.getBoolean("SkinWithoutPerm", SKINWITHOUTPERM);
        SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown", SKIN_CHANGE_COOLDOWN);
        SKIN_ERROR_COOLDOWN = config.getInt("SkinErrorCooldown", SKIN_ERROR_COOLDOWN);
        DEFAULT_SKINS_ENABLED = config.getBoolean("DefaultSkins.Enabled", DEFAULT_SKINS_ENABLED);
        DEFAULT_SKINS_PREMIUM = config.getBoolean("DefaultSkins.ApplyForPremium", DEFAULT_SKINS_PREMIUM);
        DEFAULT_SKINS = config.getStringList("DefaultSkins.Names");
        DISABLED_SKINS_ENABLED = config.getBoolean("DisabledSkins.Enabled", DISABLED_SKINS_ENABLED);
        DISABLED_SKINS = config.getStringList("DisabledSkins.Names");
        CUSTOM_GUI_ENABLED = config.getBoolean("CustomGUI.Enabled", CUSTOM_GUI_ENABLED);
        CUSTOM_GUI_ONLY = config.getBoolean("CustomGUI.ShowOnlyCustomGUI", CUSTOM_GUI_ONLY);
        CUSTOM_GUI_SKINS = config.getStringList("CustomGUI.Names");
        DISABLE_PREFIX = config.getBoolean("DisablePrefix", DISABLE_PREFIX);
        USE_OLD_SKIN_HELP = config.getBoolean("UseOldSkinHelp", USE_OLD_SKIN_HELP);
        PER_SKIN_PERMISSIONS = config.getBoolean("PerSkinPermissions", PER_SKIN_PERMISSIONS);
        SKIN_EXPIRES_AFTER = config.getInt("SkinExpiresAfter", SKIN_EXPIRES_AFTER);
        MULTIBUNGEE_ENABLED = config.getBoolean("MultiBungee.Enabled", MULTIBUNGEE_ENABLED);
        USE_MYSQL = config.getBoolean("MySQL.Enabled", USE_MYSQL);
        MYSQL_HOST = config.getString("MySQL.Host", MYSQL_HOST);
        MYSQL_PORT = config.getString("MySQL.Port", MYSQL_PORT);
        MYSQL_DATABASE = config.getString("MySQL.Database", MYSQL_DATABASE);
        MYSQL_USERNAME = config.getString("MySQL.Username", MYSQL_USERNAME);
        MYSQL_PASSWORD = config.getString("MySQL.Password", MYSQL_PASSWORD);
        MYSQL_SKINTABLE = config.getString("MySQL.SkinTable", MYSQL_SKINTABLE);
        MYSQL_PLAYERTABLE = config.getString("MySQL.PlayerTable", MYSQL_PLAYERTABLE);
        MYSQL_CONNECTIONOPTIONS = config.getString("MySQL.ConnectionOptions",MYSQL_CONNECTIONOPTIONS);
        DISABLE_ONJOIN_SKINS = config.getBoolean("DisableOnJoinSkins", DISABLE_ONJOIN_SKINS);
        NO_SKIN_IF_LOGIN_CANCELED = config.getBoolean("NoSkinIfLoginCanceled", NO_SKIN_IF_LOGIN_CANCELED);
        UPDATER_ENABLED = config.getBoolean("Updater.Enabled");
        UPDATER_PERIODIC = config.getBoolean("Updater.PeriodicChecks", UPDATER_PERIODIC);
        DISMOUNT_PLAYER_ON_UPDATE = config.getBoolean("DismountPlayerOnSkinUpdate", DISMOUNT_PLAYER_ON_UPDATE);
        REMOUNT_PLAYER_ON_UPDATE = config.getBoolean("RemountPlayerOnSkinUpdate", REMOUNT_PLAYER_ON_UPDATE);
        DISMOUNT_PASSENGERS_ON_UPDATE = config.getBoolean("DismountPassengersOnSkinUpdate", DISMOUNT_PASSENGERS_ON_UPDATE);
        DEBUG = config.getBoolean("Debug", DEBUG);


        if (!CUSTOM_GUI_ENABLED)
            CUSTOM_GUI_ONLY = false;

        if (!DISMOUNT_PLAYER_ON_UPDATE)
            REMOUNT_PLAYER_ON_UPDATE = false;
    }

    public static void set(String path, Object value) {
        config.set(path, value);
    }
}
