package skinsrestorer.shared.storage;

import java.io.File;
import java.lang.reflect.Field;

import skinsrestorer.shared.utils.DataFiles;

public class LocaleStorage {

	public String PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED = "&9Your skin data has been removed";
	public String PLAYER_SKIN_COOLDOWN = "&cYou can change your skin only once per %s seconds";
	public String PLAYER_SKIN_CHANGE_SUCCESS = "&9Your skin has been updated.";
	public String PLAYER_SKIN_CHANGE_SUCCESS_DATABASE = "&9Your skin has been updated from database.";
	public String DO_YOU_WANT_SKIN = "&9Click me if you want your skin.";

	public String PLAYER_HAS_NO_PERMISSION = "&cYou don't have permission to do this!";
	public String DISABLED_SKIN = "&cThis skin is disabled by an Administrator";
	public String UNKNOWN_COMMAND = "Unknown command. Type \"/help\" for help.";

	public String SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER = "&cCan't find a valid premium player with that name";
	public String SKIN_FETCH_FAILED_NO_SKIN_DATA = "&cNo skin data found for player with that name";
	public String SKIN_FETCH_FAILED_PARSE_FAILED = "&cCan't decode skin data";
	public String SKIN_FETCH_FAILED_RATE_LIMITED = "&cRate limited";
	public String SKIN_FETCH_FAILED_MCAPI_PROBLEM = "&cAn error occured while trying to connect to mcapi.ca for skin.";
	public String SKIN_FETCH_FAILED_ERROR = "&cAn error has occured";
	public String SKIN_DATA_DROPPED = "&9Skin data for player %player dropped";
	public String SKIN_DATA_SAVED = "&9Skin data saved successfully.";
	public String SKIN_DATA_UPDATED = "&9Skin data updated";
	public String SKIN_FETCH_FAILED = "&4Skin fetch failed: ";
	public String MCAPI_FAILED_ERROR = "&cTried to use mcapi.ca, but it failed.";
	public String TRYING_TO_USE_NCAPI = "[SkinsRestorer] Getting skin from Mojang failed. Using mcapi.ca!";

	public String USE_SKIN_HELP = "&9Use '/skin help' for help.";
	public String PLAYER_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Help &8]&7&m-------------*r&8[\n&9/skin set <skinname> &9-&a Sets your skin.\n&9/skin clear &9-&a Clears your skin.";

	public String ADMIN_USE_SKIN_HELP = "&9Use '/skinsrestorer help' for help.";
	public String ADMIN_SET_SKIN = "You set %player''s skin.";
	public String ADMIN_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Admin Help &8]&7&m-------------*r&8[\n&9/skinsrestorer drop <player> &9-&a Drops player skin data.\n&9/skinsrestorer update <player> &9-&a Updates player skin data.\n&9/skinsrestorer set <player> <skin name> &9-&a Sets Player's skin.\n&9/skinsrestorer info &9- &aShows some info.";

	private static final LocaleStorage instance = new LocaleStorage();
	private static DataFiles locale = new DataFiles("plugins" + File.separator + "SkinsRestorer" + File.separator + "",
			"locale");

	public static final LocaleStorage getInstance() {
		return instance;
	}

	public static void init() {
		locale.reload();
		for (Field f : LocaleStorage.class.getFields()) {
			try {
				if (f.getType() != String.class)
					return;

				f.setAccessible(true);

				String s = locale.getString(f.getName());
				if (s != null)
					f.set(instance, s);
				else
					locale.set(f.getName(), f.get(instance));

				f.setAccessible(false);

			} catch (Exception e) {
			}
		}
		locale.save();
	}
}
