package skinsrestorer.shared.storage;

import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.YamlConfig;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

public class Locale {
    public static String PREFIX = "&e[&2SkinsRestorer&e] ";

    public static String HELP_SKIN_CLEAR = "Clears your skin.";
    public static String HELP_SKIN_CLEAR_OTHER = "Clears the skin of another player.";
    public static String HELP_SKIN_UPDATE = "Updates your skin.";
    public static String HELP_SKIN_UPDATE_OTHER = "Updates the skin of another player.";
    public static String HELP_SKIN_SET = "Set the skin of another player.";
    public static String HELP_SKIN_SET_OTHER = "Sets the skin of another player.";
    public static String HELP_SKIN_SET_OTHER_URL = "Set a skin by Image_url.png";
    public static String HELP_SR_RELOAD = "Reloads the configuration file.";
    public static String HELP_SR_STATUS = "Checks plugin needed API services";
    public static String HELP_SR_DROP = "Removes players or skin data.";
    public static String HELP_SR_PROPS = "Displays the players current skin properties.";

    public static String SYNTAX_DEFAULTCOMMAND = " <skin/url>";
    public static String SYNTAX_SKINSET = " <skin>";
    public static String SYNTAX_SKINSET_OTHER = " <target> <skin/url>";
    public static String SYNTAX_SKINURL = " <url>";
    public static String SYNTAX_SKINUPDATE_OTHER = " <target>";
    public static String SYNTAX_SKINCLEAR_OTHER = " <target>";

    public static String PLAYER_HAS_NO_PERMISSION_SKIN = "&4Error&8: &cYou don't have permission to set this skin.";
    public static String PLAYER_HAS_NO_PERMISSION_URL = "&4Error&8: &cYou don't have permission to set skins by URL.";
    public static String SKIN_DISABLED = "&4Error&8: &cThis skin is disabled by an administrator.";
    public static String NOT_PREMIUM = "&4Error&8: &cPremium player with that name does not exist.";
    public static String INVALID_PLAYER = "&4Error&8: &c%player is not a valid username or URL.";
    public static String SKIN_COOLDOWN = "&4Error&8: &cYou can change your skin again in &e%s &cseconds.";

    public static String SKIN_CHANGE_SUCCESS = "&2Your skin has been changed.";
    public static String SKIN_CLEAR_SUCCESS = "&2Your skin has been cleared.";
    public static String SKIN_CLEAR_ISSUER = "&2Skin cleared for player %player.";

    public static String MS_UPDATING_SKIN = "&2Uploading skin, please wait...(This may take up some time)";
    public static String SUCCESS_UPDATING_SKIN = "&2Your skin has been updated.";
    public static String SUCCESS_UPDATING_SKIN_OTHER = "&2Skin updated for player %player.";

    public static String ERROR_UPDATING_SKIN = "&4Error&8: &cAn error occurred while updating your skin. Please try again later!";
    public static String ERROR_UPDATING_URL = "&4Error&8: &cYou can't update custom url skins! \n&cRequest again using /skin url";
    public static String ERROR_UPDATING_CUSTOMSKIN = "&4Error&8: &cSkin can't be updated because its custom.";
    public static String ERROR_INVALID_URLSKIN_2 = "&4WARNING&8: &cWe are only allowing imgur images. Please make sure it's a valid skin using i.imgur.com/***.png, see &c&oskinsrestorer.net/skinurl &cfor more info";
    public static String ERROR_MS_FULL = "&4MS Error&8: &cAPI timed out while uploading your &cskin. Please try again later. (MineSkin)";
    public static String ERROR_MS_GENERIC = "&4MS Error&8: &c%error%";
    public static String GENERIC_ERROR = "&4Error&8: &cAn error occurred while requesting skin data, please try again later!";
    public static String WAIT_A_MINUTE = "&4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)";
    public static String ERROR_NO_SKIN = "&4Error&8: &cThis player has no skin set.";

    public static String SKINSMENU_OPEN = "&2Opening the skins menu...";
    public static String SKINSMENU_TITLE_NEW = "&9Skins Menu - Page %page";
    public static String SKINSMENU_NEXT_PAGE = "&a&l»&7 Next Page&a&l »";
    public static String SKINSMENU_PREVIOUS_PAGE = "&e&l«&7 Previous Page&e&l «";
    public static String SKINSMENU_REMOVE_SKIN = "&c&l[ &7Remove Skin&c&l ]";
    public static String SKINSMENU_SELECT_SKIN = "&2Click to select this skin";

    public static String ADMIN_SET_SKIN = "&2You set %player's skin.";
    public static String DATA_DROPPED = "&2Data dropped for %playerOrSkin %targets.";
    public static String STATUS_OK = "&2Mojang API connection successful!";
    public static String ALT_API_FAILED = "&4Error&8: &cSkin Data API is overloaded, please try again later!";
    public static String MS_API_FAILED = "&4Error&8: &cMineSkin API is overloaded, please try again later!";
    public static String NO_SKIN_DATA = "&4Error&8: &cNo skin data acquired! Does this player have a skin?";
    public static String RELOAD = "&2Config and Locale has been reloaded!";
    public static String OUTDATED = "&4You are running an outdated version of SkinsRestorer!\n&cPlease update to the latest version on Spigot: \n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/";

    public static String SR_LINE = "&7&m----------------------------------------";
    public static String HELP_PLAYER = "  &2&lSkinsRestorer &7- &f&lv%ver%"
            + "\n   &2/skin <skinname> &7-&f Changes your skin."
            + "\n    &2/skin update &7-&f Updates your skin."
            + "\n    &2/skin clear &7-&f Clears your skin.";

    //private static YamlConfig locale = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "messages", true);
    private static YamlConfig locale;

    public static final String[] IGNORE_PREFIX = {
            "PREFIX",
            "HELP_",
            "SYNTAX_",
            "SKINSMENU_TITLE_NEW",
            "SKINSMENU_NEXT_PAGE",
            "SKINSMENU_PREVIOUS_PAGE",
            "SKINSMENU_REMOVE_SKIN",
            "SKINSMENU_SELECT_SKIN",
            "SR_LINE"
    };

    public static void load(String path) {
        try {
            locale = new YamlConfig(path + File.separator, "messages", true);
            locale.saveDefaultConfig();
            locale.reload();

            for (Field f : Locale.class.getFields()) {

                if (f.getType() != String.class)
                    continue;

                String parsed = C.c(locale.getString(f.getName(), f.get(null)));
                if (!Config.DISABLE_PREFIX) {
                    if(!Arrays.stream(IGNORE_PREFIX).anyMatch(f.getName()::contains))
                        parsed = C.c(locale.getString("PREFIX", null)) + parsed;
                }
                f.set(null, parsed);
            }
        } catch (Exception e) {
            System.out.println("§e[§2SkinsRestorer§e] §cCan't read messages.yml! Try removing it and restart your server.");
        }
    }
}
