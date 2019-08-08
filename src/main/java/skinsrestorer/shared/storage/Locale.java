package skinsrestorer.shared.storage;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;

import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.YamlConfig;

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
    public static String INVALID_PLAYER = "&e[&2SkinsRestorer&e] &4Error&8: &c%player is not a valid username or URL.";
    public static String SKIN_COOLDOWN_NEW = "&e[&2SkinsRestorer&e] &4Error&8: &cYou can change your skin again in &e%s &cseconds.";

    public static String SKIN_CHANGE_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been changed.";
    public static String SKIN_CLEAR_SUCCESS = "&e[&2SkinsRestorer&e] &2Your skin has been cleared.";
    public static String SKIN_CLEAR_ISSUER = "&e[&2SkinsRestorer&e] &2Skin cleared for player %player.";

    public static String MS_UPDATING_SKIN = "&e[&2SkinsRestorer&e] &2Uploading skin, please wait!";
    public static String SUCCESS_UPDATING_SKIN = "&e[&2SkinsRestorer&e] &2Your skin has been updated.";
    public static String SUCCESS_UPDATING_SKIN_OTHER = "&e[&2SkinsRestorer&e] &2Skin updated for player %player.";

    public static String ERROR_UPDATING_SKIN = "&e[&2SkinsRestorer&e] &4Error&8: &cAn error happened while updating your skin. Please try again later!";
    public static String ERROR_MS_FULL = "&e[&2SkinsRestorer&e] &4MS Error&8: &cNo uploader accounts were available to fulfill your request. Try again later or consider https://mineskin.org/account";
    public static String ERROR_MS_GENERIC = "&e[&2SkinsRestorer&e] &4MS Error&8: &c%error%";
    public static String GENERIC_ERROR = "&e[&2SkinsRestorer&e] &4Error&8: &cAn error occurred while requesting skin data, please try again later!";
    public static String WAIT_A_MINUTE = "&e[&2SkinsRestorer&e] &4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)";

    public static String MENU_OPEN = "&2Opening the skins menu...";
    public static String NEXT_PAGE = "&a&l»&7 Next Page&a&l »";
    public static String PREVIOUS_PAGE = "&e&l«&7 Previous Page&e&l «";
    public static String REMOVE_SKIN = "&c&l»&7 Remove Skin&c&l «";
    public static String SELECT_SKIN = "&2Click to select this skin";

    public static String ADMIN_SET_SKIN = "&e[&2SkinsRestorer&e] &2You set %player's skin.";
    public static String SKIN_DATA_DROPPED = "&e[&2SkinsRestorer&e] &2Skin data for player %player dropped.";
    public static String STATUS_OK = "&e[&2SkinsRestorer&e] &2Mojang API connection successful!";
    public static String ALT_API_FAILED = "&e[&2SkinsRestorer&e] &4Error&8: &cSkin Data API is overloaded, please try again later!";
    public static String MS_API_FAILED = "&e[&2SkinsRestorer&e] &4Error&8: &cMineSkin API is overloaded, please try again later!";
    public static String NO_SKIN_DATA = "&e[&2SkinsRestorer&e] &4Error&8: &cNo skin data acquired! Does this player have a skin?";
    public static String RELOAD = "&e[&2SkinsRestorer&e] &2Config and Locale has been reloaded!";
    public static String OUTDATED = "&e[&2SkinsRestorer&e] &4You are running an outdated version of SkinsRestorer!\n&cPlease update to the latest version on Spigot: \n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/";

    public static String SR_LINE = "&7&m----------------------------------------";
    public static String HELP_PLAYER = "  &2&lSkinsRestorer &7- &f&lv%ver%"
            + "\n   &2/skin <skinname> &7-&f Changes your skin."
            + "\n    &2/skin update &7-&f Updates your skin."
            + "\n    &2/skin clear &7-&f Clears your skin.";

    private static YamlConfig locale;

    public static void load(final String path, final Object plugin) {
        try {
            locale = new YamlConfig(path + File.separator, Config.LOCALE, true);

            if (!locale.getFile().exists()) {
                locale.getFile().createNewFile();
                InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(
                    "languages" + File.separator + Config.LOCALE + ".yml"
                );
                locale.saveResource(in, Config.LOCALE + ".yml", true);
            }
            locale.reload();

            for (Field f : Locale.class.getFields()) {
                if (f.getType() != String.class)
                    continue;

                f.set(null, C.c(locale.getString(formatFieldName(f.getName()), f.get(null))));
            }
        } catch (Exception e) {
            System.out.printf("§e[§2SkinsRestorer§e] §cCan't read %s.yml! Try removing it and restart your server.\n", Config.LOCALE);
        }
    }

    /**
     * Translates conventioned public static constants HELLO_WORLD into
     * camel case helloWorld.
     *
     * @author  NyanGuyMF
     */
    private static String formatFieldName(final String fieldName) {
        StringBuffer buff = new StringBuffer(fieldName);

        for (int c = 0; c < buff.length(); c++) {
            char ch = buff.charAt(c);

            if (ch == '_') {
                if (c+1 == buff.length()) {
                    buff.deleteCharAt(c);
                    continue;
                }

                char next = buff.charAt(c+1);

                if (Character.isUpperCase(next))
                    buff.setCharAt(c, next);
                else
                    buff.setCharAt(c, Character.toUpperCase(next));

                buff.deleteCharAt(c+1);
            } else if (Character.isUpperCase(ch)) {
                buff.setCharAt(c, Character.toLowerCase(ch));
            }
        }

        return buff.toString();
    }
}
