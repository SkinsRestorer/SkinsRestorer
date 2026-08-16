/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;
import net.skinsrestorer.shared.gui.SharedGUI;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.config.ConfigHelpers.newCappedProperty;

public class CommandConfig implements SettingsHolder {

    @Comment({
            "The 'skinsrestorer.player' permission grants access to player commands.",
            "When enabled, SkinsRestorer grants this permission to all players.",
            "When disabled, grant the permission through your permission plugin.",
            "SkinsRestorer ignores this option on platforms that support default permissions."
    })
    public static final Property<Boolean> FORCE_DEFAULT_PERMISSIONS = newProperty("commands.forceDefaultPermissions", true);
    @Comment({
            "The cooldown, in seconds, between skin changes.",
            "A value of 0 disables the cooldown.",
            "Skin errors use commands.skinErrorCooldown instead.",
            "The 'skinsrestorer.bypasscooldown' permission bypasses this cooldown."
    })
    public static final Property<Integer> SKIN_CHANGE_COOLDOWN = newCappedProperty("commands.skinChangeCooldown", 30, 0, Integer.MAX_VALUE);
    @Comment({
            "The cooldown, in seconds, between skull requests.",
            "A value of 0 disables the cooldown.",
            "Skull errors use commands.skullErrorCooldown instead.",
            "The 'skinsrestorer.bypasscooldown' permission bypasses this cooldown."
    })
    public static final Property<Integer> SKULL_GET_COOLDOWN = newCappedProperty("commands.skullGetCooldown", 30, 0, Integer.MAX_VALUE);

    public static final Property<Integer> SKIN_ERROR_COOLDOWN = newCappedProperty("commands.skinErrorCooldown", 5, 0, Integer.MAX_VALUE);
    public static final Property<Integer> SKULL_ERROR_COOLDOWN = newCappedProperty("commands.skullErrorCooldown", 5, 0, Integer.MAX_VALUE);
    public static final Property<Boolean> RESTRICT_SKIN_URLS_ENABLED = newProperty("commands.restrictSkinUrls.enabled", false);
    @SuppressWarnings("HttpUrlsUsage")
    public static final Property<List<String>> RESTRICT_SKIN_URLS_LIST = newListProperty("commands.restrictSkinUrls.list",
            "https://i.imgur.com",
            "http://i.imgur.com",
            "https://storage.googleapis.com",
            "http://storage.googleapis.com",
            "https://cdn.discordapp.com",
            "http://cdn.discordapp.com",
            "https://textures.minecraft.net",
            "http://textures.minecraft.net"
    );
    public static final Property<Boolean> DISABLED_SKINS_ENABLED = newProperty("commands.disabledSkins.enabled", false);
    public static final Property<List<String>> DISABLED_SKINS = newListProperty("commands.disabledSkins.list", "steve", "owner");
    public static final String CONSENT_MESSAGE = "I will follow the rules";
    @Comment({
            "To enable per-skin permissions, you must accept these rules:",
            "- Players must not pay to use their own skin.",
            "- Do not replace player skins with Steve to require payment.",
            "- You can charge for custom skins that you provide.",
            "If you accept these rules, set this value to: '" + CONSENT_MESSAGE + "'"
    })
    public static final Property<String> PER_SKIN_PERMISSIONS_CONSENT = newProperty("commands.perSkinPermissionsConsent", "");
    @Comment({
            "Enables permissions for individual skins.",
            "Examples: skinsrestorer.skin.xknat and skinsrestorer.skin.Pistonmaster",
            "The 'skinsrestorer.ownskin' permission lets players use /skin set <their own name>.",
            "Configure the required permissions before you enable this option.",
            "You must also accept the rules in commands.perSkinPermissionsConsent."
    })
    public static final Property<Boolean> PER_SKIN_PERMISSIONS = newProperty("commands.perSkinPermissions", false);
    @Comment({
            "The maximum number of skins in each player's history.",
            "The /skin undo command uses this history.",
            "A value of 0 disables skin history."
    })
    public static final Property<Integer> MAX_HISTORY_LENGTH = newCappedProperty("commands.maxHistoryLength", SharedGUI.HEAD_COUNT_PER_PAGE, 0, Integer.MAX_VALUE);
    @Comment({
            "The maximum number of favourite skins for each player.",
            "The /skin favourite command uses this limit.",
            "A value of 0 disables favourites."
    })
    public static final Property<Integer> MAX_FAVOURITE_LENGTH = newCappedProperty("commands.maxFavouriteLength", SharedGUI.HEAD_COUNT_PER_PAGE * 5, 0, Integer.MAX_VALUE);
    @Comment({
            "Replaces the translated /skin help message with a custom message.",
            "This option changes only the message for /skin without arguments.",
            "It does not change error messages or subcommand help."
    })
    public static final Property<Boolean> CUSTOM_HELP_ENABLED = newProperty("commands.customHelp.enabled", false);
    @Comment({
            "The custom message for /skin without arguments."
    })
    public static final Property<List<String>> CUSTOM_HELP_MESSAGE = newListProperty("commands.customHelp.message",
            "<yellow>Skin plugin Help",
            "<gray>---------------------",
            "<gray>/skin set <skin> - <yellow>Set your skin"
    );
    @Comment({
            "WARNING:",
            "This option prevents SkinsRestorer from registering the /skin command.",
            "Enable it only when another plugin provides /skin or when you do not want this command.",
            "Restart the server after you change this option."
    })
    public static final Property<Boolean> DISABLE_SKIN_COMMAND = newProperty("commands.disableSkinCommand", false);
    @Comment({
            "WARNING:",
            "This option prevents SkinsRestorer from registering the /skull command.",
            "Enable it only when another plugin provides /skull or when you do not want this command.",
            "Restart the server after you change this option."
    })
    public static final Property<Boolean> DISABLE_SKULL_COMMAND = newProperty("commands.disableSkullCommand", false);
    @Comment({
            "WARNING:",
            "This option prevents SkinsRestorer from registering the /skins command.",
            "Enable it only when another plugin provides /skins or when you do not want the GUI command.",
            "Restart the server after you change this option."
    })
    public static final Property<Boolean> DISABLE_GUI_COMMAND = newProperty("commands.disableGUICommand", false);

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("commands",
                "\n",
                "\n############",
                "\n# Commands #",
                "\n############",
                "\n",
                "Controls command behavior.",
                "Command and permission guide: https://skinsrestorer.net/docs/configuration/commands-permissions"
        );
        conf.setComment("commands.disabledSkins",
                "Players cannot use skins in this list.",
                "The 'skinsrestorer.bypassdisabled' permission bypasses this restriction."
        );
        conf.setComment("commands.restrictSkinUrls",
                "When enabled, /skin url accepts URLs only from domains in this list.",
                "Use this option to restrict skins to trusted image hosts."
        );
    }
}
