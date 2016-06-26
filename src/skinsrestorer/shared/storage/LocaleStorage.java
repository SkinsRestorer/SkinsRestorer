package skinsrestorer.shared.storage;

import java.io.File;
import java.lang.reflect.Field;

import skinsrestorer.shared.utils.YamlConfig;

public class LocaleStorage {

	public static String PLAYER_NOT_ONLINE = "&cPlayer is not online!";
	public static String PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED = "&9Your skin data has been removed.";
	public static String PLAYER_SKIN_COOLDOWN = "&cYou can change your skin only once per %s seconds.";
	public static String PLAYER_SKIN_CHANGE_SUCCESS = "&9Your skin has been updated.";
	public static String PLAYER_SKIN_CHANGE_SUCCESS_DATABASE = "&9Your skin has been updated from database.";
	public static String DO_YOU_WANT_SKIN = "&9Click me if you want your skin.";

	public static String PLAYER_HAS_NO_PERMISSION = "&cYou don't have permission to do this.";
	public static String DISABLED_SKIN = "&cThis skin is currently disabled by Administrator.";
	public static String UNKNOWN_COMMAND = "Unknown command. Type \"/help\" for help.";

	public static String SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER = "&cPremium player with that name does not exist.";
	public static String SKIN_FETCH_FAILED_NO_SKIN_DATA = "&cNo skin data found for player with that name.";
	public static String SKIN_FETCH_FAILED_RATE_LIMITED = "&cRate limited. Please wait 10 minutes before requesting the same skin again.";
	public static String SKIN_FETCH_FAILED_MCAPI_PROBLEM = " &cCould not connect to McAPI.ca .";
	public static String SKIN_FETCH_FAILED_ERROR = "&cAn error occured while requesting skin data.";
	public static String SKIN_DATA_DROPPED = "&9Skin data for player %player dropped.";
	public static String SKIN_DATA_SAVED = "&9Skin data saved successfully.";
	public static String SKIN_DATA_UPDATED = "&9Skin data updated.";
	public static String SKIN_FETCH_FAILED = "&4Skin request failed:";
	public static String MCAPI_FAILED_ERROR = "&cTried using McAPI.ca ,but its overloaded!";
	public static String TRYING_TO_USE_MCAPI = "MojangAPI failed, using McAPI.ca for skin %skin .";

	public static String USE_SKIN_HELP = "&9Use '/skin help' for help.";
	public static String PLAYER_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Help &8]&7&m-------------&r&8[\n &9/skin set <skinname> &9-&a Sets your skin.\n &9/skin clear &9-&a Clears your skin.";
	public static String ADMIN_USE_SKIN_HELP = "&9Use '/skinsrestorer help' for help.";
	public static String ADMIN_SET_SKIN = "You set %player''s skin.";
	public static String ADMIN_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Admin Help &8]&7&m-------------&r&8[\n &9/skinsrestorer drop <player> &9-&a Drops player skin data.\n &9/skinsrestorer update <player> &9-&a Updates player skin data.\n &9/skinsrestorer set <player> <skin name> &9-&a Sets Player's skin.\n &9/skinsrestorer info &9- &aShows some info.\n &9/skinsrestorer debug &9- &aCreates a debug.txt file for reporting issues\n &9/skinsrestorer reload &9- &aReloads the config and locale";;
	public static String RELOAD = "&9Config and Locale has been reloaded!";
	private static YamlConfig locale = new YamlConfig(
			"plugins" + File.separator + "SkinsRestorer" + File.separator + "", "messages");

	public static void init() {
		try {
			locale.reload();

			for (Field f : LocaleStorage.class.getFields()) {

				if (f.getType() != String.class)
					continue;

				f.set(null, locale.get(f.getName(), f.get(null)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}