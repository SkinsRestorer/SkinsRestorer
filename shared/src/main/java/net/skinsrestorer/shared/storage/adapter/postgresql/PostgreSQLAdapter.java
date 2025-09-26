/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
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
package net.skinsrestorer.shared.storage.adapter.postgresql;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.gui.GUIUtils;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.storage.model.player.LegacyPlayerData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.*;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.ComponentString;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PostgreSQLAdapter implements StorageAdapter {
    private final PostgreSQLProvider postgres;
    private final SettingsManager settings;
    private final SRLogger logger;

    @Override
    public void init() {
        postgres.update("CREATE TABLE IF NOT EXISTS " + resolveCacheTable() + " ("
                + "name VARCHAR(16) NOT NULL,"
                + "uuid VARCHAR(36),"
                + "timestamp BIGINT NOT NULL,"
                + "PRIMARY KEY (name))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolveCooldownTable() + " ("
                + "uuid VARCHAR(36),"
                + "group_name VARCHAR(36) NOT NULL,"
                + "creation_time BIGINT NOT NULL,"
                + "duration BIGINT NOT NULL,"
                + "PRIMARY KEY (uuid, group_name))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolvePlayerTable() + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "skin_identifier VARCHAR(2083),"
                + "skin_variant VARCHAR(20),"
                + "skin_type VARCHAR(20),"
                + "PRIMARY KEY (uuid))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolvePlayerHistoryTable() + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "timestamp BIGINT NOT NULL,"
                + "skin_identifier VARCHAR(2083) NOT NULL,"
                + "skin_variant VARCHAR(20),"
                + "skin_type VARCHAR(20) NOT NULL,"
                + "PRIMARY KEY (uuid, timestamp))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolvePlayerFavouritesTable() + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "timestamp BIGINT NOT NULL,"
                + "skin_identifier VARCHAR(2083) NOT NULL,"
                + "skin_variant VARCHAR(20),"
                + "skin_type VARCHAR(20) NOT NULL,"
                + "PRIMARY KEY (uuid, timestamp))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolvePlayerSkinTable() + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "last_known_name VARCHAR(16),"
                + "value TEXT NOT NULL,"
                + "signature TEXT NOT NULL,"
                + "timestamp BIGINT NOT NULL,"
                + "PRIMARY KEY (uuid))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolveURLSkinTable() + " ("
                + "url VARCHAR(266) NOT NULL," // Max chatbox command length
                + "mine_skin_id VARCHAR(36),"
                + "value TEXT NOT NULL,"
                + "signature TEXT NOT NULL,"
                + "skin_variant VARCHAR(20),"
                + "PRIMARY KEY (url))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolveURLSkinIndexTable() + " ("
                + "url VARCHAR(266) NOT NULL," // Max chatbox command length
                + "skin_variant VARCHAR(20),"
                + "PRIMARY KEY (url))");

        postgres.update("CREATE TABLE IF NOT EXISTS " + resolveCustomSkinTable() + " ("
                + "name VARCHAR(36) NOT NULL,"
                + "display_name TEXT,"
                + "value TEXT NOT NULL,"
                + "signature TEXT NOT NULL,"
                + "PRIMARY KEY (name))");
    }

    @Override
    public Optional<PlayerData> getPlayerData(UUID uuid) throws StorageException {
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolvePlayerTable() + " WHERE uuid=?", uuid.toString());
             ResultSet historyCrs = postgres.query("SELECT * FROM " + resolvePlayerHistoryTable() + " WHERE uuid=?", uuid.toString());
             ResultSet favouritesCrs = postgres.query("SELECT * FROM " + resolvePlayerFavouritesTable() + " WHERE uuid=?", uuid.toString())) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String skinIdentifier = crs.getString("skin_identifier");
            String skinType = crs.getString("skin_type");
            String skinVariant = crs.getString("skin_variant");

            SkinIdentifier identifier = skinIdentifier != null && skinType != null ?
                    SkinIdentifier.of(skinIdentifier,
                            skinVariant == null ? null : SkinVariant.valueOf(skinVariant), SkinType.valueOf(skinType)) : null;

            List<HistoryData> history = new ArrayList<>();
            while (historyCrs.next()) {
                String historySkinIdentifier = historyCrs.getString("skin_identifier");
                String historySkinType = historyCrs.getString("skin_type");
                String historySkinVariant = historyCrs.getString("skin_variant");

                SkinIdentifier historyIdentifier = SkinIdentifier.of(
                        historySkinIdentifier,
                        historySkinVariant == null ? null : SkinVariant.valueOf(historySkinVariant),
                        SkinType.valueOf(historySkinType)
                );

                history.add(HistoryData.of(historyCrs.getLong("timestamp"), historyIdentifier));
            }

            List<FavouriteData> favourites = new ArrayList<>();
            while (favouritesCrs.next()) {
                String favouriteSkinIdentifier = favouritesCrs.getString("skin_identifier");
                String favouriteSkinType = favouritesCrs.getString("skin_type");
                String favouriteSkinVariant = favouritesCrs.getString("skin_variant");

                SkinIdentifier favouriteIdentifier = SkinIdentifier.of(
                        favouriteSkinIdentifier,
                        favouriteSkinVariant == null ? null : SkinVariant.valueOf(favouriteSkinVariant),
                        SkinType.valueOf(favouriteSkinType)
                );

                favourites.add(FavouriteData.of(favouritesCrs.getLong("timestamp"), favouriteIdentifier));
            }

            return Optional.of(PlayerData.of(uuid, identifier, history, favourites));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setPlayerData(UUID uuid, PlayerData data) {
        boolean hasSkin = data.getSkinIdentifier() != null;
        SkinIdentifier identifier = data.getSkinIdentifier();
        String skinIdentifierString = hasSkin ? identifier.getIdentifier() : null;
        String skinType = hasSkin ? identifier.getSkinType().name() : null;

        // Variant is only present on url skins
        String skinVariant = hasSkin && identifier.getSkinVariant() != null ? identifier.getSkinVariant().name() : null;
        postgres.update("INSERT INTO " + resolvePlayerTable() + " (uuid, skin_identifier, skin_type, skin_variant) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (uuid) DO UPDATE SET skin_identifier=EXCLUDED.skin_identifier, skin_type=EXCLUDED.skin_type, skin_variant=EXCLUDED.skin_variant",
                uuid.toString(),
                skinIdentifierString,
                skinType,
                skinVariant);

        postgres.update("DELETE FROM " + resolvePlayerHistoryTable() + " WHERE uuid=? AND timestamp NOT IN (" +
                (data.getHistory().isEmpty() ? "NULL" : data.getHistory().stream().map(HistoryData::getTimestamp).map(String::valueOf).collect(Collectors.joining(", ")))
                + ")", uuid.toString());
        for (HistoryData historyData : data.getHistory()) {
            SkinIdentifier historyIdentifier = historyData.getSkinIdentifier();
            String historySkinIdentifier = historyIdentifier.getIdentifier();
            String historySkinType = historyIdentifier.getSkinType().name();
            String historySkinVariant = historyIdentifier.getSkinVariant() != null ? historyIdentifier.getSkinVariant().name() : null;

            postgres.update("INSERT INTO " + resolvePlayerHistoryTable() + " (uuid, timestamp, skin_identifier, skin_type, skin_variant) VALUES (?, ?, ?, ?, ?) "
                            + "ON CONFLICT (uuid, timestamp) DO UPDATE SET skin_identifier=EXCLUDED.skin_identifier, skin_type=EXCLUDED.skin_type, skin_variant=EXCLUDED.skin_variant",
                    uuid.toString(),
                    historyData.getTimestamp(),
                    historySkinIdentifier,
                    historySkinType,
                    historySkinVariant);
        }

        postgres.update("DELETE FROM " + resolvePlayerFavouritesTable() + " WHERE uuid=? AND timestamp NOT IN (" +
                (data.getFavourites().isEmpty() ? "NULL" : data.getFavourites().stream().map(FavouriteData::getTimestamp).map(String::valueOf).collect(Collectors.joining(", ")))
                + ")", uuid.toString());
        for (FavouriteData favouriteData : data.getFavourites()) {
            SkinIdentifier favouriteIdentifier = favouriteData.getSkinIdentifier();
            String favouriteSkinIdentifier = favouriteIdentifier.getIdentifier();
            String favouriteSkinType = favouriteIdentifier.getSkinType().name();
            String favouriteSkinVariant = favouriteIdentifier.getSkinVariant() != null ? favouriteIdentifier.getSkinVariant().name() : null;

            postgres.update("INSERT INTO " + resolvePlayerFavouritesTable() + " (uuid, timestamp, skin_identifier, skin_type, skin_variant) VALUES (?, ?, ?, ?, ?) "
                            + "ON CONFLICT (uuid, timestamp) DO UPDATE SET skin_identifier=EXCLUDED.skin_identifier, skin_type=EXCLUDED.skin_type, skin_variant=EXCLUDED.skin_variant",
                    uuid.toString(),
                    favouriteData.getTimestamp(),
                    favouriteSkinIdentifier,
                    favouriteSkinType,
                    favouriteSkinVariant);
        }
    }

    @Override
    public Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException {
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolvePlayerSkinTable() + " WHERE uuid=?", uuid.toString())) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String lastKnownName = crs.getString("last_known_name");
            String value = crs.getString("value");
            String signature = crs.getString("signature");
            long timestamp = crs.getLong("timestamp");

            return Optional.of(PlayerSkinData.of(uuid, lastKnownName, SkinProperty.of(value, signature), timestamp));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removePlayerSkinData(UUID uuid) {
        postgres.update("DELETE FROM " + resolvePlayerSkinTable() + " WHERE uuid=?", uuid.toString());
    }

    @Override
    public void setPlayerSkinData(UUID uuid, PlayerSkinData skinData) {
        postgres.update("INSERT INTO " + resolvePlayerSkinTable() + " (uuid, last_known_name, value, signature, timestamp) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (uuid) DO UPDATE SET last_known_name=EXCLUDED.last_known_name, value=EXCLUDED.value, signature=EXCLUDED.signature, timestamp=EXCLUDED.timestamp",
                uuid.toString(),
                skinData.getLastKnownName(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getTimestamp());
    }

    @Override
    public Optional<URLSkinData> getURLSkinData(String url, SkinVariant skinVariant) throws StorageException {
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolveURLSkinTable() + " WHERE url=? AND skin_variant=?", url, skinVariant.name())) {
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
        postgres.update("DELETE FROM " + resolveURLSkinTable() + " WHERE url=? AND skin_variant=?", url, skinVariant.name());
    }

    @Override
    public void setURLSkinData(String url, URLSkinData skinData) {
        postgres.update("INSERT INTO " + resolveURLSkinTable() + " (url, mine_skin_id, value, signature, skin_variant) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (url) DO UPDATE SET mine_skin_id=EXCLUDED.mine_skin_id, value=EXCLUDED.value, signature=EXCLUDED.signature, skin_variant=EXCLUDED.skin_variant",
                url,
                skinData.getMineSkinId(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature(),
                skinData.getSkinVariant().name());
    }

    @Override
    public Optional<URLIndexData> getURLSkinIndex(String url) throws StorageException {
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolveURLSkinIndexTable() + " WHERE url=?", url)) {
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
        postgres.update("DELETE FROM " + resolveURLSkinIndexTable() + " WHERE url=?", url);
    }

    @Override
    public void setURLSkinIndex(String url, URLIndexData skinData) {
        postgres.update("INSERT INTO " + resolveURLSkinIndexTable() + " (url, skin_variant) VALUES (?, ?) "
                        + "ON CONFLICT (url) DO UPDATE SET skin_variant=EXCLUDED.skin_variant",
                url,
                skinData.getSkinVariant().name());
    }

    @Override
    public Optional<CustomSkinData> getCustomSkinData(String skinName) throws StorageException {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolveCustomSkinTable() + " WHERE name=?", skinName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String displayName = crs.getString("display_name");
            String value = crs.getString("value");
            String signature = crs.getString("signature");

            return Optional.of(CustomSkinData.of(skinName, displayName == null ? null : new ComponentString(displayName), SkinProperty.of(value, signature)));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeCustomSkinData(String skinName) {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
        postgres.update("DELETE FROM " + resolveCustomSkinTable() + " WHERE name=?", skinName);
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
        postgres.update("INSERT INTO " + resolveCustomSkinTable() + " (name, display_name, value, signature) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (name) DO UPDATE SET display_name=EXCLUDED.display_name, value=EXCLUDED.value, signature=EXCLUDED.signature",
                skinName,
                skinData.getDisplayName() == null ? null : skinData.getDisplayName().jsonString(),
                skinData.getProperty().getValue(),
                skinData.getProperty().getSignature());
    }

    @Override
    public Optional<LegacySkinData> getLegacySkinData(String skinName) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void removeLegacySkinData(String skinName) {
        // Legacy storage is not supported for PostgreSQL
    }

    @Override
    public Optional<LegacyPlayerData> getLegacyPlayerData(String playerName) throws StorageException {
        return Optional.empty();
    }

    @Override
    public void removeLegacyPlayerData(String playerName) {
        // Legacy storage is not supported for PostgreSQL
    }

    @Override
    public int getTotalCustomSkins() {
        try (ResultSet crs = postgres.query("SELECT COUNT(*) FROM (" + getCustomSkinQuery(0, -1) + ") AS subquery")) {
            if (crs.next()) {
                return crs.getInt(1);
            }
        } catch (SQLException e) {
            logger.warning("Failed to get total custom skins", e);
        }

        return 0;
    }

    /**
     * @param limit -1 for no limit
     */
    @Override
    public List<GUIUtils.GUIRawSkinEntry> getCustomGUISkins(int offset, int limit) {
        List<GUIUtils.GUIRawSkinEntry> skins = new ArrayList<>();
        try (ResultSet crs = postgres.query(getCustomSkinQuery(offset, limit))) {
            while (crs.next()) {
                String name = crs.getString("name");
                String displayName = crs.getString("display_name");
                String value = crs.getString("value");

                skins.add(new GUIUtils.GUIRawSkinEntry(
                        SkinIdentifier.ofCustom(name),
                        displayName == null ? ComponentHelper.convertPlainToJson(name) : new ComponentString(displayName),
                        PropertyUtils.getSkinTextureHash(value),
                        List.of()
                ));
            }
        } catch (SQLException e) {
            logger.warning("Failed to get stored skins", e);
        }

        return skins;
    }

    private String getCustomSkinQuery(int offset, int limit) {
        StringBuilder query = new StringBuilder("SELECT name, display_name, value")
                .append(" FROM ")
                .append(resolveCustomSkinTable())
                .append(" WHERE name NOT LIKE '" + SkinStorageImpl.RECOMMENDATION_PREFIX + "%'");

        if (settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY_LIST)) {
            List<String> onlyListSkins = settings.getProperty(GUIConfig.CUSTOM_GUI_LIST);
            if (!onlyListSkins.isEmpty()) {
                query.append(" AND name IN (");
                query.append(onlyListSkins.stream()
                        .map(CustomSkinData::sanitizeCustomSkinName)
                        .map(s -> "'" + s + "'")
                        .collect(Collectors.joining(", ")));
                query.append(")");
            }
        }

        query.append(" ORDER BY name ASC");

        if (limit > 0) {
            query.append(" LIMIT ").append(limit);
            if (offset > 0) {
                query.append(" OFFSET ").append(offset);
            }
        }

        return query.toString();
    }

    @Override
    public int getTotalPlayerSkins() {
        try (ResultSet crs = postgres.query("SELECT COUNT(*) FROM (" + getPlayerSkinQuery(0, -1) + ") AS subquery")) {
            if (crs.next()) {
                return crs.getInt(1);
            }
        } catch (SQLException e) {
            logger.warning("Failed to get total custom skins", e);
        }

        return 0;
    }

    /**
     * @param limit -1 for no limit
     */
    @Override
    public List<GUIUtils.GUIRawSkinEntry> getPlayerGUISkins(int offset, int limit) {
        List<GUIUtils.GUIRawSkinEntry> skins = new ArrayList<>();
        try (ResultSet crs = postgres.query(getPlayerSkinQuery(offset, limit))) {
            while (crs.next()) {
                String uuid = crs.getString("uuid");
                String lastKnownName = crs.getString("last_known_name");
                String value = crs.getString("value");

                skins.add(new GUIUtils.GUIRawSkinEntry(
                        SkinIdentifier.ofPlayer(UUID.fromString(uuid)),
                        ComponentHelper.convertPlainToJson(lastKnownName),
                        PropertyUtils.getSkinTextureHash(value),
                        List.of()
                ));
            }
        } catch (SQLException e) {
            logger.warning("Failed to get stored skins", e);
        }

        return skins;
    }

    private String getPlayerSkinQuery(int offset, int limit) {
        StringBuilder query = new StringBuilder("SELECT uuid, last_known_name, value")
                .append(" FROM ")
                .append(resolvePlayerSkinTable());

        if (settings.getProperty(GUIConfig.PLAYERS_GUI_ONLY_LIST)) {
            List<String> onlyListSkins = settings.getProperty(GUIConfig.PLAYERS_GUI_LIST);
            if (!onlyListSkins.isEmpty()) {
                query.append(" WHERE uuid IN (");
                query.append(onlyListSkins.stream()
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .map(s -> "'" + s + "'")
                        .collect(Collectors.joining(", ")));
                query.append(")");
            }
        }

        query.append(" ORDER BY uuid ASC");

        if (limit > 0) {
            query.append(" LIMIT ").append(limit);
            if (offset > 0) {
                query.append(" OFFSET ").append(offset);
            }
        }

        return query.toString();
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) {
        postgres.update("DELETE FROM " + resolvePlayerSkinTable() + " WHERE timestamp <> 0 AND timestamp<=?", targetPurgeTimestamp);
    }

    @Override
    public Optional<MojangCacheData> getCachedUUID(String playerName) throws StorageException {
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolveCacheTable() + " WHERE name=?", playerName)) {
            if (!crs.next()) {
                return Optional.empty();
            }

            String uuidString = crs.getString("uuid");
            UUID uuid = uuidString != null ? UUID.fromString(uuidString) : null;
            long timestamp = crs.getLong("timestamp");

            return Optional.of(MojangCacheData.of(uuid, timestamp));
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setCachedUUID(String playerName, MojangCacheData mojangCacheData) {
        String uuid = mojangCacheData.getUniqueId().map(UUID::toString).orElse(null);
        postgres.update("INSERT INTO " + resolveCacheTable() + " (name, uuid, timestamp) VALUES (?, ?, ?) "
                        + "ON CONFLICT (name) DO UPDATE SET uuid=EXCLUDED.uuid, timestamp=EXCLUDED.timestamp",
                playerName,
                uuid,
                mojangCacheData.getTimestamp());
    }

    @Override
    public List<UUID> getAllCooldownProfiles() throws StorageException {
        List<UUID> profiles = new ArrayList<>();
        try (ResultSet crs = postgres.query("SELECT DISTINCT uuid FROM " + resolveCooldownTable())) {
            while (crs.next()) {
                profiles.add(UUID.fromString(crs.getString("uuid")));
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }

        return profiles;
    }

    @Override
    public List<StorageCooldown> getCooldowns(UUID owner) throws StorageException {
        List<StorageCooldown> cooldowns = new ArrayList<>();
        try (ResultSet crs = postgres.query("SELECT * FROM " + resolveCooldownTable() + " WHERE uuid=?", owner.toString())) {
            while (crs.next()) {
                String groupName = crs.getString("group_name");
                Instant creationTime = Instant.ofEpochSecond(crs.getLong("creation_time"));
                Duration duration = Duration.ofSeconds(crs.getLong("duration"));

                cooldowns.add(new StorageCooldown(owner, groupName, creationTime, duration));
            }
        } catch (SQLException e) {
            throw new StorageException(e);
        }

        return cooldowns;
    }

    @Override
    public void setCooldown(UUID owner, String groupName, Instant creationTime, Duration duration) {
        postgres.update("INSERT INTO " + resolveCooldownTable() + " (uuid, group_name, creation_time, duration) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (uuid, group_name) DO UPDATE SET creation_time=EXCLUDED.creation_time, duration=EXCLUDED.duration",
                owner.toString(),
                groupName,
                creationTime.getEpochSecond(),
                duration.getSeconds());
    }

    @Override
    public void removeCooldown(UUID owner, String groupName) {
        postgres.update("DELETE FROM " + resolveCooldownTable() + " WHERE uuid=? AND group_name=?", owner.toString(), groupName);
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

    private String resolvePlayerHistoryTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "player_history";
    }

    private String resolvePlayerFavouritesTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "player_favourites";
    }

    private String resolveCooldownTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "cooldowns";
    }

    private String resolveCacheTable() {
        return settings.getProperty(DatabaseConfig.MYSQL_TABLE_PREFIX) + "cache";
    }

}
