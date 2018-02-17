/*
 * Copyright (C) 2013-2018 drtshock
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package skinsrestorer.bungee.storage;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * An enum for requesting strings from the language file.
 */
public enum Locale {
    TITLE("TITLE", "&e[&2SkinsRestorer&e]"),
    SR_LINE("SR_LINE", "&7&m----------------------------------------"),
    PLAYER_HAS_NO_PERMISSION("PLAYER_HAS_NO_PERMISSION", "&4Error&8: &cYou don''t have permission to do this."),
    HELP_PLAYER("HELP_PLAYER", "&2&lSkinsRestorer &7- &f&lv%ver%\n\n  &2/skin set <skinname> &7-&f Changes your skin.\n  &2/skin <skinname> &7-&f Shortened version of \"/skin set\".\n  &2/skin clear &7-&f Clears your skin."),
    HELP_SR("HELP_SR", "    &2/sr &7- &fDisplay admin commands."),
    HELP_ADMIN("HELP_ADMIN", "&2&lSkinsRestorer &7- &f&lv%ver% &c&lAdmin\n\n  &2/sr set <player> <skin name> &7- &fChanges the skin of a player.\n  &2/sr drop <player> &7- &fDrops player skin data.\n  &2/sr reload &7- &fReloads the config and locale\n  &2/sr props [player] &7- &fDisplays the players actual skin as properties"),
    NOT_PREMIUM("NOT_PREMIUM", "&4Error&8: &cPremium player with that name does not exist."),
    SKIN_COOLDOWN_NEW("SKIN_COOLDOWN_NEW", "&4Error&8: &cYou can change your skin again in &e%s &cseconds."),
    SKIN_CHANGE_SUCCESS("SKIN_CHANGE_SUCCESS", "&2Your skin has been changed."),
    SKIN_CLEAR_SUCCESS("SKIN_CLEAR_SUCCESS", "&2Your skin has been cleared."),
    ADMIN_SET_SKIN("ADMIN_SET_SKIN", "&2You set %player''s skin."),
    NOT_ONLINE("NOT_ONLINE", "&4Error&8: &cPlayer is not online!"),
    SKIN_DATA_DROPPED("SKIN_DATA_DROPPED", "&2Skin data for player %player dropped."),
    RELOAD("RELOAD", "&2Config has been reloaded!"),
    SKIN_DISABLED("SKIN_DISABLED", "&4Error&8: &cThis skin is disabled by an administrator."),
    ALT_API_FAILED("ALT_API_FAILED", "&4Error&8: &cSkin Data API is overloaded, please try again later!"),
    NO_SKIN_DATA("NO_SKIN_DATA", "&4Error&8: &cNo skin data acquired! Does this player have a skin?"),
    STATUS_OK("STATUS_OK", "&2Mojang API connection successful!"),
    GENERIC_ERROR("GENERIC_ERROR", "&4Error&8: &cAn error occurred while requesting skin data, please try again later!"),
    WAIT_A_MINUTE("WAIT_A_MINUTE", "&4Error&8: &cPlease wait a minute before requesting that skin again. (Rate Limited)"),
    NOT_PLAYER("NOT_PLAYER", "&4Error&8: &cYou need to be a player!"),
    OUTDATED("OUTDATED", "&4You are running an outdated version of SkinsRestorer!\n&cPlease update to the latest version on Spigot:\n&ehttps://www.spigotmc.org/resources/skinsrestorer.2124/"),
    MENU_OPEN("MENU_OPEN", "&2Opening the skins menu..."),
    PLAYERS_ONLY("PLAYERS_ONLY", "&4These commands are only for players!"),
    NEXT_PAGE("NEXT_PAGE", "&a&l»&7 Next Page&a&l »"),
    PREVIOUS_PAGE("PREVIOUS_PAGE", "&e&l»&7 Previous Page&e&l «"),
    REMOVE_SKIN("REMOVE_SKIN", "&c&l»&7 Remove Skin&c&l »"),
    SELECT_SKIN("SELECT_SKIN", "&2Click to select this skin");

    private static YamlConfiguration LANG;
    private final String path;
    private final String def;

    /**
     * Lang enum constructor.
     *
     * @param path  The string path.
     * @param start The default string.
     */
    Locale(String path, String start) {
        this.path = path;
        this.def = start;
    }

    /**
     * Set the {@code YamlConfiguration} to use.
     *
     * @param config The config to set.
     */
    public static void setFile(YamlConfiguration config) {
        LANG = config;
    }

    @Override
    public String toString() {
        if (this == TITLE) {
            return ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def)) + " ";
        }
        return ChatColor.translateAlternateColorCodes('&', LANG.getString(this.path, def));
    }

    /**
     * Get the default value of the path.
     *
     * @return The default value of the path.
     */
    public String getDefault() {
        return this.def;
    }

    /**
     * Get the path to the string.
     *
     * @return The path to the string.
     */
    public String getPath() {
        return this.path;
    }
}
