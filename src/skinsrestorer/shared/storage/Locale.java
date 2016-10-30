package skinsrestorer.shared.storage;

import java.io.File;
import java.lang.reflect.Field;

import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.YamlConfig;

public class Locale {

	private static YamlConfig locale = new YamlConfig(
			"plugins" + File.separator + "SkinsRestorer" + File.separator + "", "messages");

	public static String NOT_PLAYER = "&cYou need to be a player!";
	public static String NOT_ONLINE = "&cPlayer is not online!";
	public static String SKIN_COOLDOWN = "&cYou can change your skin only once per %s seconds.";
	public static String SKIN_CHANGE_SUCCESS = "&aYour skin has been updated.";
	public static String SKIN_CHANGE_SUCCESS_DATABASE = "&aYour skin has been updated from database.";

	public static String PLAYER_HAS_NO_PERMISSION = "&cYou don't have permission to do this.";
	public static String SKIN_DISABLED = "&cThis skin is disabled by an Administrator.";
	public static String UNKNOWN_COMMAND = "&cUnknown command. Type \"/help\" for help.";

	public static String ALT_API_FAILED = "&cSkin Data API is overloaded, please try again later!";
	public static String NOT_PREMIUM = "&cPremium player with that name does not exist.";
	public static String RATE_LIMITED = "&cYou are rate limited for this skin! \n&cPlease wait10 minutes before requesting the same skin again.";
	public static String GENERIC_ERROR = "&cAn error occured while requesting skin data, please try again later!";

	public static String SKIN_DATA_DROPPED = "&aSkin data for player %player dropped.";
	public static String SKIN_DATA_SAVED = "&aSkin data saved successfully.";
	public static String SKIN_DATA_UPDATED = "&aSkin data updated.";

	public static String PLAYER_HELP = "&e&m-------------&r&8[ &aSkinsRestorer &8]&e&m-------------&r\n &9/skin <skinname> &f-&a Sets your skin.\n &9/clearskin &f-&a Removes your skin.";
	public static String ADMIN_SET_SKIN = "&aYou set %player's skin.";
	public static String ADMIN_HELP = "&a&m-------------&r&8[ &9SkinsRestorer Admin &8]&a&m-------------&r\n &9/skinsrestorer set <player> <skin name> &9-&a Sets Player's skin.\n&9/skinsrestorer drop <player> &9-&a Drops player skin data.\n &9/skinsrestorer reload &9- &aReloads the config and locale\n &9/skinsrestorer props [player] &9- &aDisplays the players actual skin as properties";;
	public static String RELOAD = "&aConfig and Locale has been reloaded!";
	public static String RELOAD_SKINS = "&aThe skins menu has been reloaded!";
	public static String NO_SKIN_DATA = "&cNo skin data acquired! Does this player have a skin?";

	public static String OUTDATED = "&4You are running an outdated version of SKINSRESTORER!\n&cPlease update to latest version on Spigot: \n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/";

	public static void load() {
		try {
			locale.reload();

			for (Field f : Locale.class.getFields()) {

				if (f.getType() != String.class)
					continue;

				f.set(null, C.c(locale.getString(f.getName(), f.get(null))));
			}
		} catch (Exception e) {
			System.out.println("[SkinsRestorer] Can't read messages.yml! Try removing it and restarting.");
		}
	}
}