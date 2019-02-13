package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.lang.reflect.Field;

public class Locale {

    public static String HELP_SKIN_CLEAR = "Clears your skin.";
    public static String HELP_SKIN_CLEAR_OTHER = "Clears the skin of another player.";
    public static String HELP_SKIN_UPDATE = "Updates your skin.";
    public static String HELP_SKIN_UPDATE_OTHER = "Updates the skin of another player.";
    public static String HELP_SKIN_SET = "Sets your skin.";
    public static String HELP_SKIN_SET_OTHER = "Sets the skin of another player.";
    public static String HELP_SR_RELOAD = "Reloads the configuration file.";
    public static String HELP_SR_STATUS = "Checks needed API services for this plugin to work.";
    public static String HELP_SR_DROP = "Drops the players skin data.";
    public static String HELP_SR_PROPS = "Displays the players actual skin as properties.";
    
    public static String PLAYER_HAS_NO_PERMISSION_SKIN = "&e[&2SkinsRestorer&e] &4Error&8: &cYou don't have permission to set this skin.";
    public static String SKIN_DISABLED = "&e[&2SkinsRestorer&e] &4Error&8: &cThis skin is disabled by an administrator.";
    public static String NOT_PREMIUM = "&e[&2SkinsRestorer&e] &4Error&8: &cPremium player with that name does not exist.";
    public static String INVALID_PLAYER = "&e[&2SkinsRestorer&e] &4Error&8: &c%player is not a valid username.";
    public static String SKIN_COOLDOWN_NEW = "&e[&2SkinsRestorer&e] &4Error&8: &cYou can change your skin again in &e%s &cseconds.";

    public static String SKIN_CHANGE_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been changed.";
    public static String SKIN_CLEAR_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been cleared.";
    public static String SKIN_CLEAR_ISSUER = "&e[&2SkinsRestorer&e] &2Skin cleared for player %player.";

    public static String SUCCESS_UPDATING_SKIN = "&e[&2SkinsRestorer&e] &2Your skin has been updated.";
    public static String SUCCESS_UPDATING_SKIN_OTHER = "&e[&2SkinsRestorer&e] &2Skin updated for player %player.";
    
    public static String ERROR_UPDATING_SKIN = "&e[&2SkinsRestorer&e] &4Error&8: &cAn error happened while updating your skin. Please try again later!";
    public static String GENERIC_ERROR = "&e[&2SkinsRestorer&e] &4Error&8: &cAn error occurred while requesting skin data, please try again later!";
    public static String WAIT_A_MINUTE = "&e[&2SkinsRestorer&e] &4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)";

    public static String MENU_OPEN = "&2Opening the skins menu...";
    public static String NEXT_PAGE = "&a&l»&7 Next Page&a&l »";
    public static String PREVIOUS_PAGE = "&e&l»&7 Previous Page&e&l «";
    public static String REMOVE_SKIN = "&c&l»&7 Remove Skin&c&l »";
    public static String SELECT_SKIN = "&2Click to select this skin";

    public static String ADMIN_SET_SKIN = "&e[&2SkinsRestorer&e] &2You set %player's skin.";
    public static String SKIN_DATA_DROPPED = "&e[&2SkinsRestorer&e] &2Skin data for player %player dropped.";
    public static String STATUS_OK = "&e[&2SkinsRestorer&e] &2Mojang API connection successful!";
    public static String ALT_API_FAILED = "&e[&2SkinsRestorer&e] &4Error&8: &cSkin Data API is overloaded, please try again later!";
    public static String NO_SKIN_DATA = "&e[&2SkinsRestorer&e] &4Error&8: &cNo skin data acquired! Does this player have a skin?";
    public static String RELOAD = "&e[&2SkinsRestorer&e] &2Config and Locale has been reloaded!";
    public static String OUTDATED = "&e[&2SkinsRestorer&e] &4You are running an outdated version of SkinsRestorer!\n&cPlease update to the latest version on Spigot: \n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/";

    
    private static YamlConfig locale = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "messages", true);

    public static void load() {
        try {
            locale.saveDefaultConfig();
            locale.reload();

            for (Field f : Locale.class.getFields()) {

                if (f.getType() != String.class)
                    continue;

                f.set(null, C.c(locale.getString(f.getName(), f.get(null))));
            }
        } catch (Exception e) {
            System.out.println("§e[§2SkinsRestorer§e] §cCan't read messages.yml! Try removing it and restart your server.");
        }
    }
}
