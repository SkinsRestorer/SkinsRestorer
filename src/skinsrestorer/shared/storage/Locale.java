package skinsrestorer.shared.storage;

import java.io.File;
import java.lang.reflect.Field;

import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.YamlConfig;

public class Locale {

	private static YamlConfig locale = new YamlConfig(
			"plugins" + File.separator + "SkinsRestorer" + File.separator + "", "msgs");

	public static String NOT_PLAYER = "&4Error&8: &cYou need to be a player!";
	public static String NOT_ONLINE = "&4Error&8: &cPlayer is not online!";
	public static String SKIN_COOLDOWN = "&eYou can change your skin only once per %s seconds.";
	public static String SKIN_CHANGE_SUCCESS = "&2Your skin has been changed.";

	public static String PLAYER_HAS_NO_PERMISSION = "&4Error&8: &cYou don't have permission to do this.";
	public static String SKIN_DISABLED = "&4Error&8: &cThis skin is disabled by an Administrator.";
	public static String UNKNOWN_COMMAND = "&cUnknown command. Type \"/help\" for help.";

	public static String ALT_API_FAILED = "&4Error&8: &cSkin Data API is overloaded, please try again later!";
	public static String NOT_PREMIUM = "&4Error&8: &cPremium player with that name does not exist.";
	public static String WAIT_A_MINUTE = "&4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)";
	public static String GENERIC_ERROR = "&4Error&8: &cAn error occured while requesting skin data, please try again later!";

	public static String SKIN_DATA_DROPPED = "&2skin data for player %player dropped.";
	public static String SKIN_DATA_SAVED = "&2Skin data saved successfully.";

	public static String SR_LINE = "&7&m----------------------------------------";
	public static String HELP_PLAYER = "  &2&lSkinsRestorer &7- &f&lv%ver%"
			+ "\n    &2/skin <skinname> &7-&f Changes your skin.";

	public static String ADMIN_SET_SKIN = "&2You set %player's skin.";
	public static String HELP_ADMIN = "  &2&lSkinsRestorer &7- &f&lv%ver% &c&lAdmin"	
			+ "\n\n    &2/sr config &7- &fhelp page for usefull in game config settings"
			+ "\n    &2/sr set <player> <skin name> &7- &fChanges the skin of a player.."
			+ "\n    &2/sr drop <player> &7- &fDrops player skin data."
			+ "\n    &2/sr reload &7- &fReloads the config and locale"
			+ "\n    &2/sr props [player] &7- &fDisplays the players actual skin as properties"
	public static String HELP_CONFIG = "  &2&lSkinsRestorer &7- &c&lConfig"
			+ "\n\n    &2/sr defaultSkins <true/false/add [skin]> &7- &fConfigures the DefaultSkins section."
			+ "\n    &2/sr SkinWithoutPerm <true/false> &7- &fConfigures the DisabledSkins section."		
			+ "\n    &2/sr disabledSkins <true/false/add [skin]> &7- &fToggles /skin without permissions"
			+ "\n    &2/sr joinSkins <true/false> &7- &fToggles the skins on join."
			+ "\n    &2/sr updater <true/false> &7- &fToggles the updater";
	public static String RELOAD = "&2Config and Locale has been reloaded!";
	public static String NO_SKIN_DATA = "&4Error&8: &cNo skin data acquired! Does this player have a skin?";

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
