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
package net.skinsrestorer.shared.storage.adapter.mysql;

import ch.jalu.configme.SettingsManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MySQLAdapter implements StorageAdapter {
    private final MySQLProvider mysql;
    private final SettingsManager settings;
    private final SRLogger logger;

    public void createTable() {
        /*
        mysql.execute("CREATE TABLE IF NOT EXISTS `" + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + "` ("
                + "`Nick` varchar(17) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Skin` varchar(19) COLLATE utf8_unicode_ci NOT NULL,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + "` ("
                + "`Nick` varchar(19) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Value` text COLLATE utf8_unicode_ci,"
                + "`Signature` text COLLATE utf8_unicode_ci,"
                + "`timestamp` text COLLATE utf8_unicode_ci,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");

        if (!columnExists(settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE), "timestamp")) {
            mysql.execute("ALTER TABLE `" + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + "` ADD `timestamp` text COLLATE utf8_unicode_ci;");
        }

        if (columnVarCharLength(settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE), "Nick") < 17) {
            mysql.execute("ALTER TABLE `" + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + "` MODIFY `Nick` varchar(17) COLLATE utf8_unicode_ci NOT NULL;");
        }

        if (columnVarCharLength(settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE), "Skin") < 19) {
            mysql.execute("ALTER TABLE `" + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + "` MODIFY `Skin` varchar(19) COLLATE utf8_unicode_ci NOT NULL;");
        }

        if (columnVarCharLength(settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE), "Nick") < 19) {
            mysql.execute("ALTER TABLE `" + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + "` MODIFY `Nick` varchar(19) COLLATE utf8_unicode_ci NOT NULL;");
        }*/ // TODO
    }

    private boolean columnExists(String tableName, String columnName) {
        try (ResultSet resultSet =mysql.query("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?", tableName, columnName)) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        } catch (SQLException e) {
            logger.severe("Error checking if column exists", e);
            return false;
        }
    }

    private int columnVarCharLength(String tableName, String columnName) {
        try (ResultSet resultSet = mysql.query("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?", tableName, columnName)) {
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            logger.severe("Error checking if column exists", e);
            return -1;
        }
    }

    @Override
    public Optional<String> getStoredSkinNameOfPlayer(String playerName) {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolvePlayerTable() + " WHERE Nick=?", playerName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String skin = crs.getString("Skin");

            return Optional.of(skin);
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void removeStoredSkinNameOfPlayer(String playerName) {
        mysql.execute("DELETE FROM " + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + " WHERE Nick=?", playerName);
    }

    @Override
    public void setStoredSkinNameOfPlayer(String playerName, String skinName) {
        mysql.execute("INSERT INTO " + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + " (Nick, Skin) VALUES (?,?) ON DUPLICATE KEY UPDATE Skin=?",
                playerName, skinName, skinName);
    }

    @Override
    public Optional<PlayerData> getPlayerData(UUID uuid) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void setPlayerData(UUID uuid, PlayerData data) {

    }

    @Override
    public Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void removePlayerSkinData(UUID uuid) {

    }

    @Override
    public void setPlayerSkinData(UUID uuid, PlayerSkinData skinData) {

    }

    @Override
    public Optional<URLSkinData> getURLSkinData(String url) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void removeURLSkinData(String url) {

    }

    @Override
    public void setURLSkinData(String url, URLSkinData skinData) {

    }

    @Override
    public Optional<StoredProperty> getCustomSkinData(String skinName) throws Exception {
        try (ResultSet crs = mysql.query("SELECT * FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick=?", skinName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String value = crs.getString("Value");
            String signature = crs.getString("Signature");
            String timestamp = crs.getString("timestamp");

            return Optional.of(new StoredProperty(value, signature, Long.parseLong(timestamp)));
        }
    }

    @Override
    public void removeCustomSkinData(String skinName) {
        mysql.execute("DELETE FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick=?", skinName);
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {

    }

    @Override
    public void setStoredSkinData(String skinName, StoredProperty storedProperty) {
        mysql.execute("INSERT INTO " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " (Nick, Value, Signature, timestamp) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE Value = VALUES(Value), Signature = VALUES(Signature), timestamp = VALUES(timestamp)",
                skinName, storedProperty.getValue(), storedProperty.getSignature(), String.valueOf(storedProperty.getTimestamp()));
    }

    @SuppressFBWarnings(justification = "SQL injection is not possible here", value = {"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"})
    @Override
    public Map<String, String> getStoredSkins(int offset) {
        Map<String, String> list = new TreeMap<>();
        String filterBy = "";
        String orderBy = "Nick";

        if (settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED)) {
            List<String> customSkinNames = settings.getProperty(GUIConfig.CUSTOM_GUI_SKINS);
            if (settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY)) {
                filterBy = "WHERE Nick RLIKE '" + String.join("|", customSkinNames) + "'";
            } else {
                orderBy = "FIELD(Nick, " + customSkinNames.stream().map(skin -> "'" + skin + "'").collect(Collectors.joining(", ")) + ") DESC, Nick";
            }
        }

        try (ResultSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " " + filterBy + " ORDER BY " + orderBy + " LIMIT " + offset + ", 36")) {
            while (crs.next()) {
                list.put(crs.getString("Nick").toLowerCase(), crs.getString("Value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) {
        // delete if name not start with " " and timestamp below targetPurgeTimestamp
        mysql.execute("DELETE FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick NOT LIKE ' %' AND " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + ".timestamp NOT LIKE 0 AND " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + ".timestamp<=?", targetPurgeTimestamp);
    }

    @Override
    public Optional<MojangCacheData> getCachedUUID(String playerName) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void setCachedUUID(String playerName, MojangCacheData mojangCacheData) {

    }

    private String resolveCustomSkinTable() {

    }

    private String resolveURLSkinTable() {
        return skinsFolder.resolve(hashSHA256(url) + ".urlskin");
    }

    private String resolvePlayerSkinTable() {
        return skinsFolder.resolve(uuid + ".playerskin");
    }

    private String resolvePlayerTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE);
    }

    private String resolveCacheTable() {
        return cacheFolder.resolve(name + ".mojangcache");
    }
}
