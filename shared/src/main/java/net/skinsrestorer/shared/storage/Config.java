/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.ListProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.TypeBasedProperty;
import ch.jalu.configme.properties.convertresult.ConvertErrorRecorder;
import ch.jalu.configme.properties.types.PropertyType;
import net.skinsrestorer.shared.utils.LocaleParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.utils.FluentList.listOf;

public class Config implements SettingsHolder {
    @Comment({
            "\n##########",
            "\n# Basics #",
            "\n##########",
            "\n",
            "Core features are here",
            "\n",
            "If true, players can change skins without permission.",
            "see https://github.com/SkinsRestorer/SkinsRestorerX/wiki/cmds-&-perms for perms."
    })
    public static final Property<Boolean> SKIN_WITHOUT_PERM = newProperty("SkinWithoutPerm", true);
    @Comment({
            "Players cooldown in seconds when changing skins (set to 0 to disable).",
            "SkinErrorCooldown is used when an error or invalid url occurs.",
            "Can be bypassed with 'skinsrestorer.bypasscooldown'."
    })
    public static final Property<Integer> SKIN_CHANGE_COOLDOWN = newProperty("SkinChangeCooldown", 30);
    public static final Property<Integer> SKIN_ERROR_COOLDOWN = newProperty("SkinErrorCooldown", 5);
    @Comment({
            "True will make use of the 'CUSTOM_HELP_IF_ENABLED' & `SR_LINE` in messages.yml.",
            "This is more customizable, but at the cost of permission specific help."
    })
    public static final Property<Boolean> ENABLE_CUSTOM_HELP = newProperty("EnableCustomHelp", false);
    @Comment("Disable message prefix in SkinsRestorer messages.")
    public static final Property<Boolean> DISABLE_PREFIX = newProperty("DisablePrefix", false);
    public static final Property<Boolean> DEFAULT_SKINS_ENABLED = newProperty("DefaultSkins.Enabled", false);
    public static final Property<Boolean> DEFAULT_SKINS_PREMIUM = newProperty("DefaultSkins.ApplyForPremium", false);
    @Comment({
            "\n##########",
            "\n# Locale #",
            "\n##########",
            "\n",
            "Translation & message options here",
            "\n",
            "A language code for the language you want to use by default for messages and commands.",
            "Has to a string separated by an underscore."
    })
    public static final Property<Locale> LANGUAGE = new TypeBasedProperty<>("Language", Locale.ENGLISH, new PropertyType<Locale>() {
        @Override
        public @Nullable Locale convert(@Nullable Object object, @NotNull ConvertErrorRecorder errorRecorder) {
            return LocaleParser.parseLocale(object == null ? null : object.toString()).orElse(null);
        }

        @Override
        public @NotNull Object toExportValue(Locale value) {
            return value.toString();
        }
    });
    public static final Property<List<String>> DEFAULT_SKINS = new ListProperty<>("DefaultSkins.Names", new PropertyType<String>() {
        @Override
        public @Nullable String convert(@Nullable Object object, @NotNull ConvertErrorRecorder errorRecorder) {
            return object == null ? null : object.toString().replace(".skin", "");
        }

        @Override
        public @Nullable Object toExportValue(String value) {
            return value;
        }
    }, listOf("xknat", "pistonmaster"));
    public static final Property<Boolean> DISABLED_SKINS_ENABLED = newProperty("DisabledSkins.Enabled", false);
    public static final Property<List<String>> DISABLED_SKINS = newListProperty("DisabledSkins.Names", "steve", "owner");
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ENABLED = newProperty("NotAllowedCommandServers.Enabled", true);
    @Comment("Whether ONLY servers from the list below are allowed to use SkinsRestorer commands.")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST = newProperty("NotAllowedCommandServers.AllowList", false);
    @Comment("Block players from executing SkinsRestorer commands before having joined a server. (RECOMMENDED)")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND = newProperty("NotAllowedCommandServers.IfNoServerBlockCommand", false);
    public static final Property<List<String>> NOT_ALLOWED_COMMAND_SERVERS = newListProperty("NotAllowedCommandServers.List", listOf("auth"));
    public static final Property<Boolean> CUSTOM_GUI_ENABLED = newProperty("CustomGUI.Enabled", false);
    public static final Property<Boolean> CUSTOM_GUI_ONLY = newProperty("CustomGUI.ShowOnlyCustomGUI", true);
    public static final Property<List<String>> CUSTOM_GUI_SKINS = newListProperty("CustomGUI.Names", "xknat", "pistonmaster");
    @Comment({
            "\n############",
            "\n# Advanced #",
            "\n############",
            "\n",
            "Below Config options are OPTIONAL and are irrelevant for small servers.",
            "\n",
            "Allows the usage of per-skin permission.",
            "Example: skinsrestorer.skin.xknat OR skinsrestorer.skin.Pistonmaster",
            "with \"skinsrestorer.ownskin\" players can run /skin set %playerusername%."
    })
    public static final Property<Boolean> PER_SKIN_PERMISSIONS = newProperty("PerSkinPermissions", false);
    @Comment({
            "Time that skins are stored in the database before we request again (in minutes).",
            "[?] A value of 0 will disable auto updating of skins and players will need to manual run /skin update.",
            "[!] Lowering this value will increase the amount of requests which could be a problem on large servers."
    })
    public static final Property<Integer> SKIN_EXPIRES_AFTER = newProperty("SkinExpiresAfter", 15);
    public static final Property<Boolean> MYSQL_ENABLED = newProperty("MySQL.Enabled", false);
    public static final Property<String> MYSQL_HOST = newProperty("MySQL.Host", "localhost");
    public static final Property<Integer> MYSQL_PORT = newProperty("MySQL.Port", 3306);
    public static final Property<String> MYSQL_DATABASE = newProperty("MySQL.Database", "db");
    public static final Property<String> MYSQL_USERNAME = newProperty("MySQL.Username", "root");
    public static final Property<String> MYSQL_PASSWORD = newProperty("MySQL.Password", "pass");
    public static final Property<Integer> MYSQL_MAX_POOL_SIZE = newProperty("MySQL.MaxPoolSize", 10);
    public static final Property<String> MYSQL_SKIN_TABLE = newProperty("MySQL.SkinTable", "Skins");
    public static final Property<String> MYSQL_PLAYER_TABLE = newProperty("MySQL.PlayerTable", "Players");
    public static final Property<String> MYSQL_CONNECTION_OPTIONS = newProperty("MySQL.ConnectionOptions", "sslMode=trust&serverTimezone=UTC");
    @Comment({
            "Stops the process of setting a skin if the LoginEvent was canceled by an AntiBot plugin.",
            "[?] Unsure? leave this true for better performance."
    })
    public static final Property<Boolean> NO_SKIN_IF_LOGIN_CANCELED = newProperty("NoSkinIfLoginCanceled", true);
    @Comment("This will make SkinsRestorer always apply the skin even if the player joins as premium on an online mode server.")
    public static final Property<Boolean> ALWAYS_APPLY_PREMIUM = newProperty("AlwaysApplyPremium", false);
    public static final Property<Boolean> RESTRICT_SKIN_URLS_ENABLED = newProperty("RestrictSkinUrls.Enabled", false);
    public static final Property<List<String>> RESTRICT_SKIN_URLS_LIST = newListProperty("RestrictSkinUrls.List",
            "https://i.imgur.com",
            "http://i.imgur.com",
            "https://storage.googleapis.com",
            "http://storage.googleapis.com",
            "https://cdn.discordapp.com",
            "http://cdn.discordapp.com",
            "https://textures.minecraft.net",
            "http://textures.minecraft.net"
    );
    @Comment({
            "Here you can fill in your APIKey for lower MineSkin request times.",
            "Key can be requested from https://mineskin.org/apikey",
            "[?] A key is not required, but recommended."
    })
    public static final Property<String> MINESKIN_API_KEY = newProperty("MineskinAPIKey", "key");
    @Comment({
            "\n#################",
            "\n# Compatibility #",
            "\n#################",
            "\n",
            "If we break things, you can disable it here.",
            "\n",
            "Disabling this will stop SkinsRestorer from changing skins when a player loads a server resource pack.",
            "When a player loads a server resource pack, their skin is reset. By default, SkinsRestorer reapplies the skin when the player reports that the resource pack has been loaded or an error has occurred."
    })
    public static final Property<Boolean> RESOURCE_PACK_FIX = newProperty("ResourcePackFix", true);
    @Comment({
            "Dismounts a mounted (on a horse, or sitting) player when their skin is updated, preventing players from becoming desynced.",
            "File override = ./plugins/SkinsRestorer/disableDismountPlayer.txt"
    })
    public static final Property<Boolean> DISMOUNT_PLAYER_ON_UPDATE = newProperty("DismountPlayerOnSkinUpdate", true);
    @Comment({
            "Remounts a player that was dismounted after a skin update (above option must be true).",
            "Disabling this is only recommended if you use plugins that allow you ride other players, or use sit. Otherwise you could get errors or players could be kicked for flying.",
            "File override = ./plugins/SkinsRestorer/disableRemountPlayer.txt"
    })
    public static final Property<Boolean> REMOUNT_PLAYER_ON_UPDATE = newProperty("RemountPlayerOnSkinUpdate", true);
    @Comment({
            "Dismounts all passengers mounting a player (such as plugins that let you ride another player), preventing those players from becoming desynced.",
            "File override = ./plugins/SkinsRestorer/enableDismountEntities.txt"
    })
    public static final Property<Boolean> DISMOUNT_PASSENGERS_ON_UPDATE = newProperty("DismountPassengersOnSkinUpdate", false);
    @Comment({
            "\n###############",
            "\n# Danger Zone #",
            "\n###############",
            "\n",
            "ABSOLUTELY DO NOT CHANGE IF YOU DON'T KNOW WHAT YOU DO",
            "\n",
            "<!! Warning !!>",
            "Enabling this will stop SkinsRestorer to change skins on join.",
            "Handy for when you want run /skin apply to apply skin after texturepack popup"
    })
    public static final Property<Boolean> DISABLE_ON_JOIN_SKINS = newProperty("DisableOnJoinSkins", false);
    @Comment({
            "<!! Warning !!>",
            "Enable this will require players to run \"/skin update\" to update their skin."
    })
    public static final Property<Boolean> DISALLOW_AUTO_UPDATE_SKIN = newProperty("DisallowAutoUpdateSkin", false);
    @Comment({
            "<!! Warning Experimental !!>",
            "This enables the experimental PaperMC join event integration that allows instant skins on join.",
            "It is not as tested as the default implementation, but it is smoother and should not lag the server.",
            "It also fixes all resource pack skin apply issues.",
            "If your players are experiencing extremely long loading screens, try disabling this."
    })
    public static final Property<Boolean> ENABLE_PAPER_JOIN_LISTENER = newProperty("EnablePaperJoinListener", true);
    @Comment({
            "<!! Warning !!>",
            "When enabled if a skin gets applied on the proxy, the new texture will be forwarded to the backend as well.",
            "This is optional sometimes as the backend may pick up the new one of the proxy.",
            "It is recommended though to **KEEP THIS ON** because it keeps the backend data in sync.",
            "This feature is required for solutions like RedisBungee and also fixes bugs in some cases."
    })
    public static final Property<Boolean> FORWARD_TEXTURES = newProperty("ForwardTextures", true);
    @Comment({
            "Updater Settings",
            "<!! Warning !!>",
            "Using outdated version void's support, compatibility & stability.",
            "\n",
            "To block all types of automatic updates (which can risk keeping an exploit):",
            "Create a file called 'noupdate.txt' in the plugin directory (./plugins/SkinsRestorer/ )",
            "\n",
            "\n################",
            "\n# DEV's corner #",
            "\n################",
            "\n",
            "Enable these on the dev's request",
            "\n",
            "Enable to start receiving debug messages about api requests & more."
    })
    public static final Property<Boolean> DEBUG = newProperty("Debug", false);

    private Config() {
    }

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("",
                "\n##################################",
                "\n#    SkinsRestorer Config.yml    #",
                "\n##################################",
                "\n",
                "We from SRTeam thank you for using our plugin!",
                "For more information        -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "For installation            -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Installing-SkinsRestorer",
                "For Configuration Help      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "Commands & Permissions      -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/cmds-&-perms",
                "For locale & messages       -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Locale-and-Translations",
                "Not working or get error?  -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Troubleshoot",
                "For advanced help or other, go to our Discord: https://discord.me/SkinsRestorer/",
                "\n",
                "(?) update config? -> https://raw.githubusercontent.com/SkinsRestorer/SkinsRestorerX/stable/shared/src/main/resources/config.yml",
                "(?) Step by step config guide -> https://github.com/SkinsRestorer/SkinsRestorerX/wiki/Configuration",
                "\n",
                "(!) IF YOU ARE USING A PROXY (Bungee, Waterfall or Velocity), Check & set on every BACKEND server spigot.yml -> bungeecord: true.  (!)",
                "(!) & Install Skinsrestorer.jar on ALL SERVERS!!! (BOTH Backend & Proxy).                      (!)"
        );
        conf.setComment("DefaultSkins",
                "\n#################",
                "\n# Customization #",
                "\n#################",
                "\n",
                "Here you can design the plugin the way you want it.",
                "\n",
                "Enable or disable default skins",
                "ApplyForPremium: false will only put a skin on skinless/steve players.",
                "If there is more than one, the plugin will choose a random one.",
                "[?] Supports custom & url.png skins, read SkinFile Generator below. [?]"
        );
        conf.setComment("DisabledSkins",
                "Skins in this list will be disabled, so users can't set them.",
                "Can be bypassed with 'skinsrestorer.bypassdisabled'."
        );
        conf.setComment("NotAllowedCommandServers",
                "Disable all SkinsRestorer commands on specific backend servers.",
                "[!] This only works & is relevant if you're using proxies like bungee / waterfall"
        );
        conf.setComment("CustomGUI",
                "Custom list for the /skins GUI.",
                "ShowOnlyCustomGUI will only show CustomGUI.Names in the gui."
        );
        conf.setComment("MySQL",
                "Settings for MySQL skin storage (recommended for big BungeeCord networks)",
                "[!] IF YOU USE BUNGEE, DO NOT ENABLE MYSQL in the Spigot / backend config.yml [!]",
                "[!] Non-root users: MySQL 8's new default authentication is not supported, use mysql_native_password [!]",
                "[!] Make sure you have the correct permissions set for your MySQL user. [!]",
                "[!] Make sure to fill in MySQL.ConnectionOptions if you're using certificate / ssl authentication. [!]"
        );
        conf.setComment("RestrctSkinUrls",
                "When enabled, only websites from the list below is allowed to be set using /skin url <url>",
                "[?] this is useful if you host your own image server."
        );
        conf.setFooter(
                "\n",
                "\n# End #",
                "\n",
                "Useful tools:",
                "Vectier Thailand has made some super cool \"Custom Skin\" tools that you can use!",
                "",
                "SkinFile Generator:",
                "With SkinFile Generator, you can upload your own custom skin to get a unique .skin file that you can put in your skins folder, to use with SkinsRestorer.",
                "Check it out here: https://skinsrestorer.github.io/SkinFile-Generator/",
                "",
                "SkinSystem :",
                "With SkinSystem, you, as a server owner, can connect AuthMe (and forum) with the SkinSystem website that you can host, to give your players the ability to upload custom skins.",
                "Check it out here: https://github.com/SkinsRestorer/SkinSystem",
                "",
                "\n# Useful Links #",
                "Website: https://skinsrestorer.net/",
                "Download: https://github.com/SkinsRestorer/SkinsRestorerX/releases",
                "Wiki https://github.com/SkinsRestorer/SkinsRestorerX/wiki/",
                "Spigot: https://www.spigotmc.org/resources/skinsrestorer.2124/",
                "Github: https://github.com/SkinsRestorer/SkinsRestorerX/",
                "Discord: https://discord.me/SkinsRestorer/"
        );
    }


}
