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
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AtomicAdapter;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;
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
    private final AtomicAdapter atomicAdapter;

    public void preloadDefaultSkins() {
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        List<String> defaultSkins = new ArrayList<>(settings.getProperty(StorageConfig.DEFAULT_SKINS));
        defaultSkins.forEach(skin -> {
            try {
                findOrCreateSkinData(skin);
            } catch (DataRequestException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.debug(String.format("DefaultSkin '%s' could not be found or requested! Removing from list..", skin));
                logger.debug(String.format("DefaultSkin '%s' error: ", skin), e);
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
    public Optional<SkinProperty> updatePlayerSkinData(UUID uuid) throws DataRequestException {
        try {
            Optional<PlayerSkinData> optional = atomicAdapter.get().getPlayerSkinData(uuid);

            if (!optional.isPresent()) {
                return Optional.empty();
            }

            PlayerSkinData data = optional.get();
            if (isPlayerSkinExpired(data.getTimestamp())) {
                Optional<SkinProperty> skinProperty = mojangAPI.getProfile(uuid); // TODO: Check if returned property is actually newer than the current one
                if (skinProperty.isPresent()) {
                    setPlayerSkinData(uuid, skinProperty.get(), Instant.now().getEpochSecond());
                    return skinProperty;
                }
            }

            return Optional.of(data.getProperty());
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, SkinProperty textures, long timestamp) {
        atomicAdapter.get().setPlayerSkinData(uuid, PlayerSkinData.of(uuid, textures, timestamp));
    }

    @Override
    public void setURLSkinData(String url, String mineSkinId, SkinProperty textures) {
        atomicAdapter.get().setURLSkinData(url, URLSkinData.of(url, mineSkinId, textures));
    }

    @Override
    public void setCustomSkinData(String skinName, SkinProperty textures) {
        atomicAdapter.get().setCustomSkinData(skinName, CustomSkinData.of(skinName, textures));
    }


    // TODO: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, String> getSkins(int offset) {
        return atomicAdapter.get().getStoredSkins(offset);
    }

    public Optional<InputDataResult> findSkinData(String input) {
        try {
            if (C.validUrl(input)) {
                return atomicAdapter.get().getURLSkinData(input).map(data ->
                        InputDataResult.of(SkinIdentifier.of(data.getUrl(), SkinType.URL), data.getProperty()));
            } else {
                Optional<CustomSkinData> customSkinData = atomicAdapter.get().getCustomSkinData(input);

                if (customSkinData.isPresent()) {
                    return customSkinData.map(data ->
                            InputDataResult.of(SkinIdentifier.of(data.getSkinName(), SkinType.CUSTOM), data.getProperty()));
                }

                Optional<UUID> uuid = mojangAPI.getUUID(input);

                if (!uuid.isPresent()) {
                    return Optional.empty();
                }

                Optional<PlayerSkinData> playerSkinData = atomicAdapter.get().getPlayerSkinData(uuid.get());

                if (playerSkinData.isPresent()) {
                    return playerSkinData.map(data ->
                            InputDataResult.of(SkinIdentifier.of(uuid.get().toString(), SkinType.PLAYER), data.getProperty()));
                }
            }
        } catch (StorageAdapter.StorageException | DataRequestException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<InputDataResult> findOrCreateSkinData(String input) throws DataRequestException {
        Optional<InputDataResult> skinData = findSkinData(input);

        if (skinData.isPresent()) {
            return skinData;
        }

        if (C.validUrl(input)) {
            MineSkinResponse response = mineSkinAPI.genSkin(input, null);

            setURLSkinData(input, response.getMineSkinId(), response.getProperty());

            return Optional.of(InputDataResult.of(SkinIdentifier.of(input, SkinType.URL), response.getProperty()));
        } else {
            Optional<UUID> uuid = mojangAPI.getUUID(input);

            if (!uuid.isPresent()) {
                return Optional.empty();
            }

            Optional<SkinProperty> skinProperty = mojangAPI.getProfile(uuid.get());

            if (!skinProperty.isPresent()) {
                return Optional.empty();
            }

            setPlayerSkinData(uuid.get(), skinProperty.get(), Instant.now().getEpochSecond());

            return Optional.of(InputDataResult.of(SkinIdentifier.of(uuid.get().toString(), SkinType.PLAYER), skinProperty.get()));
        }
    }

    @Override
    public Optional<SkinProperty> getSkinDataByIdentifier(SkinIdentifier identifier) {
        return Optional.empty();
    }

    @Override
    public void removeSkinData(SkinIdentifier identifier) {
        switch (identifier.getSkinType()) {
            case PLAYER:
                atomicAdapter.get().removePlayerSkinData(UUID.fromString(identifier.getIdentifier()));
                break;
            case URL:
                atomicAdapter.get().removeURLSkinData(identifier.getIdentifier());
                break;
            case CUSTOM:
                atomicAdapter.get().removeCustomSkinData(identifier.getIdentifier());
                break;
        }
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
            atomicAdapter.get().purgeStoredOldSkins(targetPurgeTimestamp);
            return true; // TODO: Do better than true/false return
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
            return false;
        }
    }
}
