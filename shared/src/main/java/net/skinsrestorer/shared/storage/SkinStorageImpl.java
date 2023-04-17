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

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.C;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinStorageImpl implements SkinStorage {
    private final SRLogger logger;
    private final MojangAPIImpl mojangAPI;
    private final MineSkinAPIImpl mineSkinAPI;
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;
    @Setter
    private StorageAdapter storageAdapter;

    public void preloadDefaultSkins() {
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        List<String> defaultSkins = new ArrayList<>(settings.getProperty(StorageConfig.DEFAULT_SKINS));
        defaultSkins.forEach(skin -> {
            // TODO: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    fetchSkinData(skin);
                }
            } catch (DataRequestException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.warning("[WARNING] DefaultSkin '" + skin + "'(.skin) could not be found or requested! Removing from list..");

                logger.debug("[DEBUG] DefaultSkin '" + skin + "' error: ", e);
            }
        });
        if (!toRemove.isEmpty()) {
            defaultSkins.removeAll(toRemove);
            settings.setProperty(StorageConfig.DEFAULT_SKINS, defaultSkins);
        }

        if (defaultSkins.isEmpty()) {
            logger.warning("[WARNING] No more working DefaultSkin left... disabling feature");
            settings.setProperty(StorageConfig.DEFAULT_SKINS_ENABLED, false);
        }
    }

    @Override
    public Optional<SkinProperty> fetchSkinData(String skinName) throws DataRequestException {
        Optional<SkinProperty> textures = getSkinData(skinName, true);
        if (textures.isPresent()) {
            return textures;
        }

        // No cached skin found, get from MojangAPI, save and return
        Optional<SkinProperty> mojangTextures = mojangAPI.getSkin(skinName);

        if (!mojangTextures.isPresent()) {
            return Optional.empty();
        }

        setCustomSkinData(skinName, mojangTextures.get());

        return mojangTextures;
    }

    /**
     * Create a platform specific property and also optionally update cached skin if outdated.
     *
     * @param playerName the players name
     * @param timestamp  time cached property data was created
     * @return Platform-specific property
     */
    private Optional<SkinProperty> updateSkin(String playerName, long timestamp) {
        if (isPlayerSkinExpired(timestamp)) {
            try {
                return mojangAPI.getSkin(playerName).map(skinProperty -> {
                    setCustomSkinData(playerName, skinProperty);
                    return skinProperty;
                });
            } catch (DataRequestException e) {
                logger.debug("Failed to update skin data for player " + playerName, e);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<SkinProperty> fetchPlayerSkinData(UUID uuid) throws DataRequestException {
        return Optional.empty();
    }

    // #getSkinData() also create while we have #getSkinForPlayer()
    @Override
    public Optional<SkinProperty> getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        try {
            String finalSkinName = skinName;
            return storageAdapter.getCustomSkinData(skinName).map(property -> {
                SkinProperty skinProperty = SkinProperty.of(property.getValue(), property.getSignature());
                if (updateOutdated) {
                    return updateSkin(finalSkinName, property.getTimestamp()).orElse(skinProperty);
                }

                return skinProperty;
            });
        } catch (Exception e) {
            logger.info(String.format("Unsupported skin format... removing (%s).", skinName));
            removeSkinData(skinName);
            return Optional.empty();
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, SkinProperty textures, long timestamp) {
        storageAdapter.setPlayerSkinData(uuid, PlayerSkinData.of(uuid, textures, timestamp));
    }

    @Override
    public void setURLSkinData(String url, String mineSkinId, SkinProperty textures) {
        storageAdapter.setURLSkinData(url, URLSkinData.of(url, mineSkinId, textures));
    }

    @Override
    public void setCustomSkinData(String skinName, SkinProperty textures) {
        storageAdapter.setCustomSkinData(skinName, CustomSkinData.of(skinName, textures));
    }

    /**
     * Removes skin data from database
     *
     * @param skinName Skin name
     */
    public void removeSkinData(String skinName) {
        skinName = skinName.toLowerCase();

        storageAdapter.removeCustomSkinData(skinName);
    }

    // TODO: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, String> getSkins(int offset) {
        return storageAdapter.getStoredSkins(offset);
    }

    /**
     * @param skinName Skin name
     * @return true on updated
     * @throws DataRequestException On updating disabled OR invalid username + api error
     */
    // skin update [include custom skin flag]
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean updateSkinData(String skinName) throws DataRequestException {
        if (!C.validMojangUsername(skinName)) {
            throw new DataRequestExceptionShared(locale, Message.ERROR_UPDATING_CUSTOMSKIN);
        }

        // Check if updating is disabled for skin (by timestamp = 0)
        boolean updateDisabled = storageAdapter.getStoredTimestamp(skinName).map(timestamp -> timestamp == 0).orElse(false);

        if (updateDisabled) {
            throw new DataRequestExceptionShared(locale, Message.ERROR_UPDATING_CUSTOMSKIN);
        }

        // Update Skin
        try {
            Optional<String> mojangUUID = mojangAPI.getUUIDMojang(skinName);

            if (mojangUUID.isPresent()) {
                Optional<SkinProperty> textures = mojangAPI.getProfileMojang(mojangUUID.get());

                if (textures.isPresent()) {
                    setCustomSkinData(skinName, textures.get());
                    return true;
                }
            }
        } catch (DataRequestException e) {
            throw new DataRequestExceptionShared(locale, Message.ERROR_UPDATING_SKIN);
        }

        return false;
    }

    public Optional<SkinIdentifier> getSkinIdByString(String input) {
        try {
            if (C.validUrl(input)) {
                return storageAdapter.getURLSkinData(input).map(data -> SkinIdentifier.of(input, SkinType.URL));
            } else {
                Optional<CustomSkinData> customSkinData = storageAdapter.getCustomSkinData(input);

                if (customSkinData.isPresent()) {
                    return Optional.of(SkinIdentifier.of(input, SkinType.CUSTOM));
                }

                Optional<UUID> uuid = mojangAPI.getUUID(input);

                if (!uuid.isPresent()) {
                    return Optional.empty();
                }

                Optional<PlayerSkinData> playerSkinData = storageAdapter.getPlayerSkinData(uuid.get());

                if (playerSkinData.isPresent()) {
                    return Optional.of(SkinIdentifier.of(input, SkinType.PLAYER));
                }
            }
        } catch (StorageAdapter.StorageException | DataRequestException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Checks if a player skin is expired and should be re-fetched from mojang.
     *
     * @param timestamp in seconds
     * @return true if skin is outdated
     */
    private boolean isPlayerSkinExpired(long timestamp) {
        // Do not update if timestamp is not 0 or update is disabled.
        if (timestamp == -1 || settings.getProperty(StorageConfig.DISALLOW_AUTO_UPDATE_SKIN)) {
            return false;
        }

        long now = Instant.now().getEpochSecond();
        long expiryDate = timestamp + TimeUnit.MINUTES.toSeconds(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER));

        return expiryDate <= now;
    }

    public boolean purgeOldSkins(int days) {
        long targetPurgeTimestamp = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli();

        try {
            storageAdapter.purgeStoredOldSkins(targetPurgeTimestamp);
            return true; // TODO: Do better than true/false return
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
            return false;
        }
    }
}
