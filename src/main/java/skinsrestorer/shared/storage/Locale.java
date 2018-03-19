package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.lang.reflect.Field;

public class Locale {

    public static String SR_LINE = "&7&m----------------------------------------";
    public static String PLAYER_HAS_NO_PERMISSION = "&e[&2SkinsRestorer&e] &4Error&8: &cYou don't have permission to do this.";
    public static String HELP_PLAYER = "  &2&lSkinsRestorer &7- &f&lv%ver%"
            + "\n   &2/skin set <skinname> &7-&f Changes your skin."
            + "\n    &2/skin <skinname> &7-&f Shortened version of \"/skin set\"."
            + "\n    &2/skin clear &7-&f Clears your skin.";
    public static String HELP_SR = "    &2/sr &7- &fDisplay admin commands.";
    public static String NOT_PREMIUM = "&e[&2SkinsRestorer&e] &4Error&8: &cPremium player with that name does not exist.";
    public static String SKIN_COOLDOWN_NEW = "&e[&2SkinsRestorer&e] &4Error&8: &cYou can change your skin again in &e%s &cseconds.";
    public static String SKIN_CHANGE_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been changed.";
    public static String SKIN_CLEAR_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been cleared.";
    public static String HELP_ADMIN = "  &2&lSkinsRestorer &7- &f&lv%ver% &c&lAdmin"
            + "\n\n   &2/sr config &7- &fhelp page for usefull in game config settings"
            + "\n    &2/sr set <player> <skin name> &7- &fChanges the skin of a player.."
            + "\n    &2/sr drop <player> &7- &fDrops player skin data."
            + "\n    &2/sr reload &7- &fReloads the config and locale"
            + "\n    &2/sr props [player] &7- &fDisplays the players actual skin as properties";
    public static String ADMIN_SET_SKIN = "&e[&2SkinsRestorer&e] &2You set %player's skin.";
    public static String NOT_ONLINE = "&e[&2SkinsRestorer&e] &4Error&8: &cPlayer is not online!";
    public static String SKIN_DATA_DROPPED = "&e[&2SkinsRestorer&e] &2Skin data for player %player dropped.";
    public static String RELOAD = "&e[&2SkinsRestorer&e] &2Config and Locale has been reloaded!";
    public static String HELP_CONFIG = "  &2&lSkinsRestorer &7- &c&lConfig"
            + "\n\n   &2/sr joinSkins <true/false> &7- &fToggles the skins on join."
            + "\n    &2/sr SkinWithoutPerm <true/false> &7- &fConfigures the DisabledSkins section."
            + "\n    &2/sr SkinExpiresAfter <time> &7- &fHow long the a skin is cached."
            + "\n    &2/sr skinCooldown <time> &7- &f/skin cooldown in minute(s)."
            + "\n    &2/sr defaultSkins <true/false/add [skin]> &7- &fConfigures the DefaultSkins section."
            + "\n    &2/sr updater <true/false> &7- &fToggles the updater";
    public static String SKIN_DISABLED = "&e[&2SkinsRestorer&e] &4Error&8: &cThis skin is disabled by an administrator.";
    public static String ALT_API_FAILED = "&e[&2SkinsRestorer&e] &4Error&8: &cSkin Data API is overloaded, please try again later!";
    public static String NO_SKIN_DATA = "&e[&2SkinsRestorer&e] &4Error&8: &cNo skin data acquired! Does this player have a skin?";
    public static String STATUS_OK = "&e[&2SkinsRestorer&e] &2Mojang API connection successful!";
    public static String GENERIC_ERROR = "&e[&2SkinsRestorer&e] &4Error&8: &cAn error occurred while requesting skin data, please try again later!";
    public static String WAIT_A_MINUTE = "&e[&2SkinsRestorer&e] &4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)";
    public static String NOT_PLAYER = "&e[&2SkinsRestorer&e] &4Error&8: &cYou need to be a player!";
    public static String OUTDATED = "&e[&2SkinsRestorer&e] &4You are running an outdated version of SkinsRestorer!\n&cPlease update to the latest version on Spigot: \n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/";
    public static String MENU_OPEN = "&2Opening the skins menu...";
    public static String PLAYERS_ONLY = "&4These commands are only for players!";
    public static String NEXT_PAGE = "&a&l»&7 Next Page&a&l »";
    public static String PREVIOUS_PAGE = "&e&l»&7 Previous Page&e&l «";
    public static String REMOVE_SKIN = "&c&l»&7 Remove Skin&c&l »";
    public static String SELECT_SKIN = "&2Click to select this skin";

    private static YamlConfig locale = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "messages");

    public static void load() {
        try {
            locale.reload();

            for (Field f : Locale.class.getFields()) {

                if (f.getType() != String.class)
                    continue;

                f.set(null, locale.getString(f.getName(), f.get(null)));
            }
        } catch (Exception e) {
            System.out.println("§e[§2SkinsRestorer§e] §cCan't read messages.yml! Try removing it and restart your server.");
        }
    }
}