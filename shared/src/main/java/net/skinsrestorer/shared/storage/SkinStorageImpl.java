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
import net.skinsrestorer.api.interfaces.SkinStorage;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
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
    public Optional<SkinProperty> getSkinOfPlayer(String playerName) throws DataRequestException {
        Optional<String> skinName = getSkinNameOfPlayer(playerName);

        if (skinName.isPresent()) {
            return fetchSkinData(skinName.get());
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<SkinProperty> getDefaultSkinForPlayer(String playerName) throws DataRequestException {
        String defaultSkinName = getDefaultSkinNameForPlayer(playerName).orElse(playerName);

        if (C.validUrl(defaultSkinName)) {
            return Optional.of(mineSkinAPI.genSkin(defaultSkinName, null));
        } else {
            return fetchSkinData(defaultSkinName);
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

        setSkinData(skinName, mojangTextures.get());

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
        if (isExpired(timestamp)) {
            try {
                return mojangAPI.getSkin(playerName).map(skinProperty -> {
                    setSkinData(playerName, skinProperty);
                    return skinProperty;
                });
            } catch (DataRequestException e) {
                logger.debug("Failed to update skin data for player " + playerName, e);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getSkinNameOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        return storageAdapter.getStoredSkinNameOfPlayer(playerName).flatMap(skinName -> {
            if (skinName.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(skinName);
        });
    }

    @Override
    public void setSkinNameOfPlayer(String playerName, String skinName) {
        playerName = playerName.toLowerCase();

        storageAdapter.setStoredSkinNameOfPlayer(playerName, skinName);
    }

    @Override
    public void removeSkinNameOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        storageAdapter.removeStoredSkinNameOfPlayer(playerName);
    }

    // #getSkinData() also create while we have #getSkinForPlayer()
    @Override
    public Optional<SkinProperty> getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        try {
            String finalSkinName = skinName;
            return storageAdapter.getStoredSkinData(skinName).map(property -> {
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

    /**
     * Removes skin data from database
     *
     * @param skinName Skin name
     */
    public void removeSkinData(String skinName) {
        skinName = skinName.toLowerCase();

        storageAdapter.removeStoredSkinData(skinName);
    }

    @Override
    public void setSkinData(String skinName, SkinProperty textures, long timestamp) {
        skinName = skinName.toLowerCase();
        String value = textures.getValue();
        String signature = textures.getSignature();

        if (value.isEmpty() || signature.isEmpty())
            return;

        storageAdapter.setStoredSkinData(skinName, new StorageAdapter.StoredProperty(value, signature, timestamp));
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
                    setSkinData(skinName, textures.get());
                    return true;
                }
            }
        } catch (DataRequestException e) {
            throw new DataRequestExceptionShared(locale, Message.ERROR_UPDATING_SKIN);
        }

        return false;
    }

    /**
     * // TODO: Rewrite this comment
     * Filters player name to exclude non [a-z_]
     * Checks and process default skin.
     * IF no default skin:
     * 1: Return player if clear
     * 2: Return skin if found
     * Else: return player
     *
     * @param playerName Player name
     * @return Custom skin or default skin or player name, right side indicates if it is a custom skin
     */
    public Optional<String> getDefaultSkinNameForPlayer(String playerName) {
        // Trim player name
        playerName = playerName.trim();

        if (settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            // don't return default skin name for premium players if enabled
            if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_PREMIUM)) {
                // check if player is premium
                try {
                    if (mojangAPI.getUUID(playerName).isPresent()) {
                        // player is premium, return his skin name instead of default skin
                        return Optional.empty();
                    }
                } catch (DataRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set, or we want to clear to default
            List<String> skins = settings.getProperty(StorageConfig.DEFAULT_SKINS);

            // return player name if there are no default skins set
            if (skins.isEmpty()) {
                return Optional.empty();
            }

            // makes no sense to select a random skin if there is only one
            if (skins.size() == 1) {
                return Optional.of(skins.get(0));
            }

            return Optional.of(skins.get(ThreadLocalRandom.current().nextInt(skins.size())));
        }

        // empty if player has no custom skin, we'll return his name then
        return Optional.empty();
    }

    /**
     * Checks if a skin is expired and should be re-fetched from mojang.
     *
     * @param timestamp in milliseconds
     * @return true if skin is outdated
     */
    private boolean isExpired(long timestamp) {
        // Do not update if timestamp is not 0 or update is disabled.
        if (timestamp == 0 || settings.getProperty(StorageConfig.DISALLOW_AUTO_UPDATE_SKIN)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long expiryDate = timestamp + TimeUnit.MINUTES.toMillis(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER));

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
