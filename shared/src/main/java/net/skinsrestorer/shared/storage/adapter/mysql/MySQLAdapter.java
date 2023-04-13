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
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MySQLAdapter implements StorageAdapter {
    private final MySQLProvider mysql;
    private final SettingsManager settings;

    @Override
    public Optional<String> getStoredSkinNameOfPlayer(String playerName) {
        try (ResultSet crs = mysql.query("SELECT * FROM " + settings.getProperty(DatabaseConfig.MYSQL_PLAYER_TABLE) + " WHERE Nick=?", playerName)) {
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
    public Optional<StoredProperty> getStoredSkinData(String skinName) throws Exception {
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
    public void removeStoredSkinData(String skinName) {
        mysql.execute("DELETE FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick=?", skinName);
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
    public Optional<Long> getStoredTimestamp(String skinName) {
        try (ResultSet crs = mysql.query("SELECT timestamp FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick=?", skinName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String timestampString = crs.getString("timestamp");

            if (timestampString == null)
                return Optional.empty();

            return Optional.of(Long.parseLong(timestampString));
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) {
        // delete if name not start with " " and timestamp below targetPurgeTimestamp
        mysql.execute("DELETE FROM " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + " WHERE Nick NOT LIKE ' %' AND " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + ".timestamp NOT LIKE 0 AND " + settings.getProperty(DatabaseConfig.MYSQL_SKIN_TABLE) + ".timestamp<=?", targetPurgeTimestamp);
    }
}
