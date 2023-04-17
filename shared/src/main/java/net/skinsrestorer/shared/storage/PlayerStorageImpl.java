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
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.player.PlayerData;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerStorageImpl implements PlayerStorage {
    private final SRLogger logger;
    private final MojangAPIImpl mojangAPI;
    private final SettingsManager settings;
    private final SkinStorage skinStorage;
    private final CacheStorageImpl cacheStorage;
    @Setter
    private StorageAdapter storageAdapter;

    @Override
    public Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid) {
        try {
            Optional<PlayerData> optional = storageAdapter.getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                return Optional.ofNullable(playerData.getSkinIdentifier());
            }
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public void setSkinIdOfPlayer(UUID uuid, SkinIdentifier identifier) {
        try {
            Optional<PlayerData> optional = storageAdapter.getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setSkinIdentifier(identifier);
                storageAdapter.setPlayerData(uuid, playerData);
            } else {
                storageAdapter.setPlayerData(uuid, PlayerData.of(uuid, identifier));
            }
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeSkinIdOfPlayer(UUID uuid) {
        try {
            Optional<PlayerData> optional = storageAdapter.getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setSkinIdentifier(null);
                storageAdapter.setPlayerData(uuid, playerData);
            }
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<SkinProperty> getSkinOfPlayer(UUID uuid) {
        return getSkinIdOfPlayer(uuid).flatMap(skinStorage::getSkinDataByIdentifier);
    }

    @Override
    public Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName) throws DataRequestException {
        Optional<SkinProperty> setSkin = getSkinOfPlayer(uuid);

        if (setSkin.isPresent()) {
            return setSkin;
        }

        return getDefaultSkinForPlayer(uuid, playerName);
    }

    /**
     * Gets the default skin for a player.
     */
    @Override
    public Optional<SkinProperty> getDefaultSkinForPlayer(UUID uuid, String playerName) throws DataRequestException {
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            return Optional.empty();
        }

        // don't return default skin name for premium players if enabled
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_PREMIUM)) {
            // check if player is premium
            if (cacheStorage.getUUID(playerName).isPresent()) {
                // player is premium, return his skin name instead of default skin
                return Optional.empty();
            }
        }

        // return default skin name if user has no custom skin set, or we want to clear to default
        List<String> skins = settings.getProperty(StorageConfig.DEFAULT_SKINS);

        // return player name if there are no default skins set
        if (skins.isEmpty()) {
            return Optional.empty();
        }

        String selectedSkin = skins.size() == 1 ? skins.get(0) : skins.get(ThreadLocalRandom.current().nextInt(skins.size()));

        return skinStorage.getSkinIdByString(selectedSkin).flatMap(skinStorage::getSkinDataByIdentifier);
    }
}
