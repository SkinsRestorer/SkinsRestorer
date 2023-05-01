/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLIndexData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MySQLAdapter implements StorageAdapter {
    private final MySQLProvider mysql;
    private final SettingsManager settings;
    private final SRLogger logger;

    public void createTable() {
        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolveCacheTable() + "` ("
                + "`name` varchar(16) NOT NULL,"
                + "`is_premium` tinyint(1) NOT NULL,"
                + "`uuid` varchar(36),"
                + "`timestamp` bigint(20) NOT NULL,"
                + "PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolvePlayerTable() + "` ("
                + "`uuid` varchar(36) NOT NULL,"
                + "`skin_identifier` VARCHAR(2083),"
                + "`skin_variant` VARCHAR(20),"
                + "`skin_type` varchar(20),"
                + "PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolvePlayerSkinTable() + "` ("
                + "`uuid` varchar(36) NOT NULL,"
                + "`value` text NOT NULL,"
                + "`signature` text NOT NULL,"
                + "`timestamp` bigint(20) NOT NULL,"
                + "PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolveURLSkinTable() + "` ("
                + "`url` varchar(2083) NOT NULL,"
                + "`mine_skin_id` varchar(36),"
                + "`value` text NOT NULL,"
                + "`signature` text NOT NULL,"
                + "`skin_variant` VARCHAR(20),"
                + "PRIMARY KEY (`url`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolveURLSkinIndexTable() + "` ("
                + "`url` varchar(2083) NOT NULL,"
                + "`skin_variant` VARCHAR(20),"
                + "PRIMARY KEY (`url`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");

        mysql.execute("CREATE TABLE IF NOT EXISTS `" + resolveCustomSkinTable() + "` ("
                + "`name` varchar(36) NOT NULL,"
                + "`value` text NOT NULL,"
                + "`signature` text NOT NULL,"
                + "PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=utf8");
    }

    @Override
    public Optional<PlayerData> getPlayerData(UUID uuid) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolvePlayerTable() + " WHERE uuid=?", uuid.toString())) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String skinIdentifier = crs.getString("skin_identifier");
            String skinType = crs.getString("skin_type");
            String skinVariant = crs.getString("skin_variant");

            SkinIdentifier identifier = skinIdentifier != null && skinType != null ?
                    SkinIdentifier.of(skinIdentifier,
                            skinVariant == null ? null : SkinVariant.valueOf(skinVariant), SkinType.valueOf(skinType)) : null;

            return Optional.of(PlayerData.of(uuid, identifier));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setPlayerData(UUID uuid, PlayerData data) {
        String skinIdentifier = data.getSkinIdentifier() != null ? data.getSkinIdentifier().getIdentifier() : null;
        String skinType = data.getSkinIdentifier() != null ? data.getSkinIdentifier().getSkinType().name() : null;
        String skinVariant = data.getSkinIdentifier() != null ? data.getSkinIdentifier().getSkinVariant().name() : null;
        mysql.execute("INSERT INTO " + resolvePlayerTable() + " (uuid, skin_identifier, skin_type, skin_variant) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE skin_identifier=?, skin_type=?, skin_variant=?",
                uuid.toString(),
                skinIdentifier,
                skinType,
                skinVariant,
                skinIdentifier,
                skinType,
                skinVariant);
    }

    @Override
    public Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolvePlayerSkinTable() + " WHERE uuid=?", uuid.toString())) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String value = crs.getString("value");
            String signature = crs.getString("signature");
            long timestamp = crs.getLong("timestamp");

            return Optional.of(PlayerSkinData.of(uuid, SkinProperty.of(value, signature), timestamp));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removePlayerSkinData(UUID uuid) {
        mysql.execute("DELETE FROM " + resolvePlayerSkinTable() + " WHERE uuid=?", uuid.toString());
    }

    @Override
    public void setPlayerSkinData(UUID uuid, PlayerSkinData skinData) {
        mysql.execute("INSERT INTO " + resolvePlayerSkinTable() + " (uuid, value, signature, timestamp) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE value=?, signature=?, timestamp=?",
                uuid.toString(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getTimestamp(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getTimestamp());
    }

    @Override
    public Optional<URLSkinData> getURLSkinData(String url, SkinVariant skinVariant) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolveURLSkinTable() + " WHERE url=? AND skin_variant=?", url, skinVariant.name())) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String mineSkinId = crs.getString("mine_skin_id");
            String value = crs.getString("value");
            String signature = crs.getString("signature");
            SkinVariant variant = SkinVariant.valueOf(crs.getString("skin_variant"));

            return Optional.of(URLSkinData.of(url, mineSkinId, SkinProperty.of(value, signature), variant));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeURLSkinData(String url, SkinVariant skinVariant) {
        mysql.execute("DELETE FROM " + resolveURLSkinTable() + " WHERE url=? AND skin_variant=?", url, skinVariant.name());
    }

    @Override
    public void setURLSkinData(String url, URLSkinData skinData) {
        mysql.execute("INSERT INTO " + resolveURLSkinTable() + " (url, mine_skin_id, value, signature, skin_variant) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE mine_skin_id=?, value=?, signature=?, skin_variant=?",
                url,
                skinData.getMineSkinId(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getSkinVariant().name(),
                skinData.getMineSkinId(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getSkinVariant().name());
    }

    @Override
    public Optional<URLIndexData> getURLSkinIndex(String url) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolveURLSkinIndexTable() + " WHERE url=?", url)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            SkinVariant variant = SkinVariant.valueOf(crs.getString("skin_variant"));

            return Optional.of(URLIndexData.of(url, variant));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeURLSkinIndex(String url) {
        mysql.execute("DELETE FROM " + resolveURLSkinIndexTable() + " WHERE url=?", url);
    }

    @Override
    public void setURLSkinIndex(String url, URLIndexData skinData) {
        mysql.execute("INSERT INTO " + resolveURLSkinIndexTable() + " (url, skin_variant) VALUES (?, ?) ON DUPLICATE KEY UPDATE skin_variant=?",
                url,
                skinData.getSkinVariant().name(),
                skinData.getSkinVariant().name());
    }

    @Override
    public Optional<CustomSkinData> getCustomSkinData(String skinName) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolveCustomSkinTable() + " WHERE name=?", skinName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String value = crs.getString("value");
            String signature = crs.getString("signature");

            return Optional.of(CustomSkinData.of(skinName, SkinProperty.of(value, signature)));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeCustomSkinData(String skinName) {
        mysql.execute("DELETE FROM " + resolveCustomSkinTable() + " WHERE name=?", skinName);
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {
        mysql.execute("INSERT INTO " + resolveCustomSkinTable() + " (name, value, signature) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?, signature=?",
                skinName,
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature());
    }

    @SuppressFBWarnings(justification = "SQL injection is not possible here", value = {"SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING"})
    @Override
    public Map<String, String> getStoredSkins(int offset) {
        throw new UnsupportedOperationException("MySQL storage does not support this operation YET"); // TODO
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) {
        mysql.execute("DELETE FROM " + resolvePlayerSkinTable() + " WHERE timestamp NOT LIKE 0 AND timestamp<=?", targetPurgeTimestamp);
    }

    @Override
    public Optional<MojangCacheData> getCachedUUID(String playerName) throws StorageException {
        try (ResultSet crs = mysql.query("SELECT * FROM " + resolveCacheTable() + " WHERE name=?", playerName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            boolean isPremium = crs.getBoolean("is_premium");
            UUID uuid = isPremium ? UUID.fromString(crs.getString("uuid")) : null;
            long timestamp = crs.getLong("timestamp");

            return Optional.of(MojangCacheData.of(isPremium, uuid, timestamp));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setCachedUUID(String playerName, MojangCacheData mojangCacheData) {
        String uuid = mojangCacheData.getUniqueId() != null ? mojangCacheData.getUniqueId().toString() : null;
        mysql.execute("INSERT INTO " + resolveCacheTable() + " (name, is_premium, uuid, timestamp) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE is_premium=?, uuid=?, timestamp=?",
                playerName,
                mojangCacheData.isPremium(),
                uuid,
                mojangCacheData.getTimestamp(),
                mojangCacheData.isPremium(),
                uuid,
                mojangCacheData.getTimestamp());
    }

    private String resolveCustomSkinTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "custom_skins";
    }

    private String resolveURLSkinTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "url_skins";
    }

    private String resolveURLSkinIndexTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "url_index";
    }

    private String resolvePlayerSkinTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "player_skins";
    }

    private String resolvePlayerTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "players";
    }

    private String resolveCacheTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "cache";
    }
}
