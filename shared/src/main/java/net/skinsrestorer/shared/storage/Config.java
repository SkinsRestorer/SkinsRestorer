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

import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class Config {
    public static boolean SKIN_WITHOUT_PERM;
    public static int SKIN_CHANGE_COOLDOWN;
    public static int SKIN_ERROR_COOLDOWN;
    public static boolean ENABLE_CUSTOM_HELP;
    public static boolean DISABLE_PREFIX;
    public static boolean DEFAULT_SKINS_ENABLED;
    public static boolean DEFAULT_SKINS_PREMIUM;
    public static List<String> DEFAULT_SKINS;
    public static boolean DISABLED_SKINS_ENABLED;
    public static List<String> DISABLED_SKINS;
    public static boolean NOT_ALLOWED_COMMAND_SERVERS_ENABLED;
    public static boolean NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST;
    public static boolean NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND;
    public static List<String> NOT_ALLOWED_COMMAND_SERVERS;
    public static boolean CUSTOM_GUI_ENABLED;
    public static boolean CUSTOM_GUI_ONLY;
    public static List<String> CUSTOM_GUI_SKINS;
    public static boolean PER_SKIN_PERMISSIONS;
    public static int SKIN_EXPIRES_AFTER;
    public static boolean FORWARD_TEXTURES;
    public static boolean MYSQL_ENABLED;
    public static String MYSQL_HOST;
    public static int MYSQL_PORT;
    public static String MYSQL_DATABASE;
    public static String MYSQL_USERNAME;
    public static String MYSQL_PASSWORD;
    public static int MYSQL_MAX_POOL_SIZE;
    public static String MYSQL_SKIN_TABLE;
    public static String MYSQL_PLAYER_TABLE;
    public static String MYSQL_CONNECTION_OPTIONS;
    public static boolean NO_SKIN_IF_LOGIN_CANCELED;
    public static boolean ALWAYS_APPLY_PREMIUM;
    public static boolean RESTRICT_SKIN_URLS_ENABLED;
    public static List<String> RESTRICT_SKIN_URLS_LIST;
    public static String MINESKIN_API_KEY;
    public static boolean RESOURCE_PACK_FIX;
    public static boolean DISMOUNT_PLAYER_ON_UPDATE;
    public static boolean REMOUNT_PLAYER_ON_UPDATE;
    public static boolean DISMOUNT_PASSENGERS_ON_UPDATE;
    public static boolean DISABLE_ON_JOIN_SKINS;
    public static boolean DISALLOW_AUTO_UPDATE_SKIN;
    public static boolean ENABLE_PROTOCOL_LISTENER;
    public static boolean DEBUG;

    public static void load(Path dataFolder, InputStream is, SRLogger logger) {
        YamlConfig config = new YamlConfig(dataFolder.resolve("config.yml"));
        config.loadConfig(is);

        SKIN_WITHOUT_PERM = config.getBoolean("SkinWithoutPerm");
        SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown");
        SKIN_ERROR_COOLDOWN = config.getInt("SkinErrorCooldown");
        ENABLE_CUSTOM_HELP = config.getBoolean("EnableCustomHelp");
        DISABLE_PREFIX = config.getBoolean("DisablePrefix");
        DEFAULT_SKINS_ENABLED = config.getBoolean("DefaultSkins.Enabled");
        DEFAULT_SKINS_PREMIUM = config.getBoolean("DefaultSkins.ApplyForPremium");
        DEFAULT_SKINS = config.getStringList("DefaultSkins.Names", ".skin");
        DISABLED_SKINS_ENABLED = config.getBoolean("DisabledSkins.Enabled");
        DISABLED_SKINS = config.getStringList("DisabledSkins.Names");
        NOT_ALLOWED_COMMAND_SERVERS_ENABLED = config.getBoolean("NotAllowedCommandServers.Enabled");
        NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST = config.getBoolean("NotAllowedCommandServers.AllowList");
        NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND = config.getBoolean("NotAllowedCommandServers.IfNoServerBlockCommand");
        NOT_ALLOWED_COMMAND_SERVERS = config.getStringList("NotAllowedCommandServers.List");
        CUSTOM_GUI_ENABLED = config.getBoolean("CustomGUI.Enabled");
        CUSTOM_GUI_ONLY = config.getBoolean("CustomGUI.ShowOnlyCustomGUI");
        CUSTOM_GUI_SKINS = config.getStringList("CustomGUI.Names");
        PER_SKIN_PERMISSIONS = config.getBoolean("PerSkinPermissions");
        SKIN_EXPIRES_AFTER = config.getInt("SkinExpiresAfter");
        FORWARD_TEXTURES = config.getBoolean("ForwardTextures");
        MYSQL_ENABLED = config.getBoolean("MySQL.Enabled");
        MYSQL_HOST = config.getString("MySQL.Host");
        MYSQL_PORT = config.getInt("MySQL.Port");
        MYSQL_DATABASE = config.getString("MySQL.Database");
        MYSQL_USERNAME = config.getString("MySQL.Username");
        MYSQL_PASSWORD = config.getString("MySQL.Password");
        MYSQL_MAX_POOL_SIZE = config.getInt("MySQL.MaxPoolSize");
        MYSQL_SKIN_TABLE = config.getString("MySQL.SkinTable");
        MYSQL_PLAYER_TABLE = config.getString("MySQL.PlayerTable");
        MYSQL_CONNECTION_OPTIONS = config.getString("MySQL.ConnectionOptions");
        DISABLE_ON_JOIN_SKINS = config.getBoolean("DisableOnJoinSkins");
        DISALLOW_AUTO_UPDATE_SKIN = config.getBoolean("DisallowAutoUpdateSkin"); //Note: incorrect name because of default value mistake!
        NO_SKIN_IF_LOGIN_CANCELED = config.getBoolean("NoSkinIfLoginCanceled");
        ALWAYS_APPLY_PREMIUM = config.getBoolean("AlwaysApplyPremium");
        RESTRICT_SKIN_URLS_ENABLED = config.getBoolean("RestrictSkinUrls.Enabled");
        RESTRICT_SKIN_URLS_LIST = config.getStringList("RestrictSkinUrls.List");
        MINESKIN_API_KEY = config.getString("MineskinAPIKey");
        RESOURCE_PACK_FIX = config.getBoolean("ResourcePackFix");
        DISMOUNT_PLAYER_ON_UPDATE = config.getBoolean("DismountPlayerOnSkinUpdate");
        REMOUNT_PLAYER_ON_UPDATE = config.getBoolean("RemountPlayerOnSkinUpdate");
        DISMOUNT_PASSENGERS_ON_UPDATE = config.getBoolean("DismountPassengersOnSkinUpdate");
        ENABLE_PROTOCOL_LISTENER = config.getBoolean("EnableProtocolListener");
        DEBUG = config.getBoolean("Debug");

        //__Default__Skins
        if (DEFAULT_SKINS_ENABLED && DEFAULT_SKINS.isEmpty()) {
            logger.warning("[Config] no DefaultSkins found! Disabling DefaultSkins.");
            DEFAULT_SKINS_ENABLED = false;
        }

        //__Disabled__Skins
        if (DISABLED_SKINS_ENABLED && DISABLED_SKINS.isEmpty()) {
            logger.warning("[Config] no DisabledSkins found! Disabling DisabledSkins.");
            DISABLED_SKINS_ENABLED = false;
        }

        if (RESTRICT_SKIN_URLS_ENABLED && RESTRICT_SKIN_URLS_LIST.isEmpty()) {
            logger.warning("[Config] no RestrictSkinUrls found! Disabling RestrictSkinUrls.");
            RESTRICT_SKIN_URLS_ENABLED = false;
        }

        if (!CUSTOM_GUI_ENABLED)
            CUSTOM_GUI_ONLY = false;

        if (!DISMOUNT_PLAYER_ON_UPDATE)
            REMOUNT_PLAYER_ON_UPDATE = false;

        try {
            if (config.getBoolean("UseOldSkinHelp")) {
                logger.warning("[Config] UseOldSkinHelp has been renamed! use \"EnableCustomHelp\"");
                ENABLE_CUSTOM_HELP = true;
            }
        } catch (Exception ignored) {
        }

        if (MINESKIN_API_KEY.equals("key"))
            MINESKIN_API_KEY = "";
    }
}
