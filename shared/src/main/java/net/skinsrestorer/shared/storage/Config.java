/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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

import net.skinsrestorer.shared.exception.YamlException;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Config {
    public static boolean SKIN_WITHOUT_PERM = false;
    public static int SKIN_CHANGE_COOLDOWN = 30;
    public static int SKIN_ERROR_COOLDOWN = 5;
    public static boolean ENABLE_CUSTOM_HELP = false;
    public static boolean DISABLE_PREFIX = false;
    public static boolean DEFAULT_SKINS_ENABLED = false;
    public static boolean DEFAULT_SKINS_PREMIUM = false;
    public static List<String> DEFAULT_SKINS = null;
    public static boolean DISABLED_SKINS_ENABLED = false;
    public static List<String> DISABLED_SKINS = null;
    public static boolean CUSTOM_GUI_ENABLED = false;
    public static boolean CUSTOM_GUI_ONLY = false;
    public static List<String> CUSTOM_GUI_SKINS = null;
    public static boolean PER_SKIN_PERMISSIONS = false;
    public static int SKIN_EXPIRES_AFTER = 20;
    public static boolean MULTI_BUNGEE_ENABLED = false;
    public static boolean MYSQL_ENABLED = false;
    public static String MYSQL_HOST = "localhost";
    public static String MYSQL_PORT = "3306";
    public static String MYSQL_DATABASE = "db";
    public static String MYSQL_USERNAME = "root";
    public static String MYSQL_PASSWORD = "pass";
    public static String MYSQL_SKIN_TABLE = "Skins";
    public static String MYSQL_PLAYER_TABLE = "Players";
    public static String MYSQL_CONNECTION_OPTIONS = "verifyServerCertificate=false&useSSL=false&serverTimezone=UTC";
    public static boolean NO_SKIN_IF_LOGIN_CANCELED = true;
    public static boolean RESTRICT_SKIN_URLS_ENABLED = false;
    public static List<String> RESTRICT_SKIN_URLS_LIST = null;
    public static String MINESKIN_API_KEY = "";
    public static boolean DISMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean REMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean DISMOUNT_PASSENGERS_ON_UPDATE = false;
    public static boolean DISABLE_ON_JOIN_SKINS = false;
    public static boolean DISALLOW_AUTO_UPDATE_SKIN = false;
    public static boolean ENABLE_PROTOCOL_LISTENER = false;
    public static boolean DEBUG = false;

    // UPCOMING MULTIPLE LANGUAGE SUPPORT
    public static String LOCALE_FILE = "english.yml";

    public static void load(File path, InputStream is, SRLogger logger) {
        YamlConfig config = new YamlConfig(path, "config.yml", false, logger);
        config.saveDefaultConfig(is);
        try {
            config.reload();
        } catch (YamlException e) {
            e.printStackTrace();
        }
        SKIN_WITHOUT_PERM = config.getBoolean("SkinWithoutPerm", SKIN_WITHOUT_PERM);
        SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown", SKIN_CHANGE_COOLDOWN);
        SKIN_ERROR_COOLDOWN = config.getInt("SkinErrorCooldown", SKIN_ERROR_COOLDOWN);
        ENABLE_CUSTOM_HELP = config.getBoolean("EnableCustomHelp", ENABLE_CUSTOM_HELP);
        DISABLE_PREFIX = config.getBoolean("DisablePrefix", DISABLE_PREFIX);
        DEFAULT_SKINS_ENABLED = config.getBoolean("DefaultSkins.Enabled", DEFAULT_SKINS_ENABLED);
        DEFAULT_SKINS_PREMIUM = config.getBoolean("DefaultSkins.ApplyForPremium", DEFAULT_SKINS_PREMIUM);
        DEFAULT_SKINS = config.getStringList("DefaultSkins.Names", ".skin");
        DISABLED_SKINS_ENABLED = config.getBoolean("DisabledSkins.Enabled", DISABLED_SKINS_ENABLED);
        DISABLED_SKINS = config.getStringList("DisabledSkins.Names");
        CUSTOM_GUI_ENABLED = config.getBoolean("CustomGUI.Enabled", CUSTOM_GUI_ENABLED);
        CUSTOM_GUI_ONLY = config.getBoolean("CustomGUI.ShowOnlyCustomGUI", CUSTOM_GUI_ONLY);
        CUSTOM_GUI_SKINS = config.getStringList("CustomGUI.Names");
        PER_SKIN_PERMISSIONS = config.getBoolean("PerSkinPermissions", PER_SKIN_PERMISSIONS);
        SKIN_EXPIRES_AFTER = config.getInt("SkinExpiresAfter", SKIN_EXPIRES_AFTER);
        MULTI_BUNGEE_ENABLED = config.getBoolean("MultiBungee.Enabled", MULTI_BUNGEE_ENABLED);
        MYSQL_ENABLED = config.getBoolean("MySQL.Enabled", MYSQL_ENABLED);
        MYSQL_HOST = config.getString("MySQL.Host", MYSQL_HOST);
        MYSQL_PORT = config.getString("MySQL.Port", MYSQL_PORT);
        MYSQL_DATABASE = config.getString("MySQL.Database", MYSQL_DATABASE);
        MYSQL_USERNAME = config.getString("MySQL.Username", MYSQL_USERNAME);
        MYSQL_PASSWORD = config.getString("MySQL.Password", MYSQL_PASSWORD);
        MYSQL_SKIN_TABLE = config.getString("MySQL.SkinTable", MYSQL_SKIN_TABLE);
        MYSQL_PLAYER_TABLE = config.getString("MySQL.PlayerTable", MYSQL_PLAYER_TABLE);
        MYSQL_CONNECTION_OPTIONS = config.getString("MySQL.ConnectionOptions", MYSQL_CONNECTION_OPTIONS);
        DISABLE_ON_JOIN_SKINS = config.getBoolean("DisableOnJoinSkins", DISABLE_ON_JOIN_SKINS);
        DISALLOW_AUTO_UPDATE_SKIN = config.getBoolean("DisallowAutoUpdateSkin", DISALLOW_AUTO_UPDATE_SKIN); //Note: incorrect name because of default value mistake!
        NO_SKIN_IF_LOGIN_CANCELED = config.getBoolean("NoSkinIfLoginCanceled", NO_SKIN_IF_LOGIN_CANCELED);
        RESTRICT_SKIN_URLS_ENABLED = config.getBoolean("RestrictSkinUrls.Enabled", RESTRICT_SKIN_URLS_ENABLED);
        RESTRICT_SKIN_URLS_LIST = config.getStringList("RestrictSkinUrls.List");
        MINESKIN_API_KEY = config.getString("MineskinAPIKey", MINESKIN_API_KEY);
        DISMOUNT_PLAYER_ON_UPDATE = config.getBoolean("DismountPlayerOnSkinUpdate", DISMOUNT_PLAYER_ON_UPDATE);
        REMOUNT_PLAYER_ON_UPDATE = config.getBoolean("RemountPlayerOnSkinUpdate", REMOUNT_PLAYER_ON_UPDATE);
        DISMOUNT_PASSENGERS_ON_UPDATE = config.getBoolean("DismountPassengersOnSkinUpdate", DISMOUNT_PASSENGERS_ON_UPDATE);
        ENABLE_PROTOCOL_LISTENER = config.getBoolean("EnableProtocolListener", ENABLE_PROTOCOL_LISTENER);
        DEBUG = config.getBoolean("Debug", DEBUG);

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
