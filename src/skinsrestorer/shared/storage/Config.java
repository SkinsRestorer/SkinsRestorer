package skinsrestorer.shared.storage;

import java.io.File;
import java.io.InputStream;

import skinsrestorer.shared.utils.YamlConfig;

public class Config {

	private static YamlConfig config = new YamlConfig(
			"plugins" + File.separator + "SkinsRestorer" + File.separator + "", "config", true);

	public static boolean DISABLE_ONJOIN_SKINS = false;
	public static int SKIN_CHANGE_COOLDOWN = 30;
	public static String ALT_SKIN_PROPERTY_URL = "http://mcapi.ca/name/uuid/";
	public static String ALT_UUID_URL = "http://us.mc-api.net/v3/uuid/";
	public static String ALT_UUID_URL2 = "http://mcapi.ca/uuid/player/";
	public static boolean USE_MYSQL = false;
	public static String MYSQL_HOST = "localhost";
	public static String MYSQL_PORT = "3306";
	public static String MYSQL_DATABASE = "db";
	public static String MYSQL_SKINTABLE = "Skins";
	public static String MYSQL_PLAYERTABLE = "Skins";
	public static String MYSQL_USERNAME = "admin";
	public static String MYSQL_PASSWORD = "pass";
	public static boolean DISABLE_SKIN_COMMAND = false;
	public static String CUSTOMSKINS_USERNAME = null;
	public static String CUSTOMSKINS_PASSWORD = null;
	public static String CUSTOMSKINS_NAME = null;
	public static String CUSTOMSKINS_ID = null;
	public static String CUSTOMSKINS_AUTHTOKEN = null;
	public static String CUSTOMSKINS_CLIENTTOKEN = null;

	public static void load(InputStream is) {
		config.copyDefaults(is);
		config.reload();

		DISABLE_ONJOIN_SKINS = config.getBoolean("DisableOnJoinSkins", DISABLE_ONJOIN_SKINS);
		SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown", SKIN_CHANGE_COOLDOWN);
		ALT_SKIN_PROPERTY_URL = config.getString("Alternatives.SkinPropertyURL", ALT_SKIN_PROPERTY_URL);
		ALT_UUID_URL = config.getString("Alternatives.UUIDURL", ALT_UUID_URL);
		ALT_UUID_URL2 = config.getString("Alternatives.UUIDURL2", ALT_UUID_URL2);
		USE_MYSQL = config.getBoolean("MySQL.Enabled", USE_MYSQL);
		MYSQL_HOST = config.getString("MySQL.Host", MYSQL_HOST);
		MYSQL_PORT = config.getString("MySQL.Port", MYSQL_PORT);
		MYSQL_DATABASE = config.getString("MySQL.Database", MYSQL_DATABASE);
		MYSQL_SKINTABLE = config.getString("MySQL.SkinTable", MYSQL_SKINTABLE);
		MYSQL_PLAYERTABLE = config.getString("MySQL.PlayerTable", MYSQL_PLAYERTABLE);
		MYSQL_USERNAME = config.getString("MySQL.Username", MYSQL_USERNAME);
		MYSQL_PASSWORD = config.getString("MySQL.Password", MYSQL_PASSWORD);
		/*
		 * CUSTOMSKINS_USERNAME = config.getString("CustomSkins.Username");
		 * CUSTOMSKINS_PASSWORD = config.getString("CustomSkins.Password");
		 * CUSTOMSKINS_NAME = config.getString("CustomSkins.Name");
		 * CUSTOMSKINS_ID = config.getString("CustomSkins.ID");
		 * CUSTOMSKINS_AUTHTOKEN = config.getString("CustomSkins.Authtoken");
		 * CUSTOMSKINS_CLIENTTOKEN =
		 * config.getString("CustomSkins.Clienttoken");
		 */
	}
}
