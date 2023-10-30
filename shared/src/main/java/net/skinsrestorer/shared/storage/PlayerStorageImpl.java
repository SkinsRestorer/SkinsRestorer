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
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.floodgate.FloodgateUtil;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.player.PlayerData;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerStorageImpl implements PlayerStorage {
    private final SettingsManager settings;
    private final SkinStorage skinStorage;
    private final CacheStorageImpl cacheStorage;
    private final SRLogger logger;
    private final AdapterReference adapterReference;

    @Override
    public Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

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
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setSkinIdentifier(identifier);
                adapterReference.get().setPlayerData(uuid, playerData);
            } else {
                adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, identifier));
            }
        } catch (StorageAdapter.StorageException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeSkinIdOfPlayer(UUID uuid) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setSkinIdentifier(null);
                adapterReference.get().setPlayerData(uuid, playerData);
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
    public Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName, boolean isOnlineMode) throws DataRequestException {
        Optional<SkinProperty> setSkin = getSkinOfPlayer(uuid);

        if (setSkin.isPresent()) {
            return setSkin;
        }

        if (FloodgateUtil.isFloodgateBedrockPlayer(uuid)) {
            logger.debug("Player " + playerName + " is a Floodgate player, not searching for java skin.");
            return Optional.empty();
        }

        if (isOnlineMode && !settings.getProperty(LoginConfig.ALWAYS_APPLY_PREMIUM)) {
            return Optional.empty();
        }

        boolean defaultSkinsEnabled = settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED);
        if (defaultSkinsEnabled && settings.getProperty(StorageConfig.DEFAULT_SKINS_PREMIUM)) {
            return getDefaultSkin();
        }

        Optional<SkinProperty> premiumSkin = cacheStorage.getSkin(playerName, false)
                .map(MojangSkinDataResult::getSkinProperty);

        if (premiumSkin.isPresent()) {
            return premiumSkin;
        }

        if (defaultSkinsEnabled) {
            return getDefaultSkin();
        }

        return Optional.empty();
    }

    private Optional<SkinProperty> getDefaultSkin() {
        // return default skin name if user has no custom skin set, or we want to clear to default
        List<String> skins = settings.getProperty(StorageConfig.DEFAULT_SKINS);

        // return player name if there are no default skins set
        if (skins.isEmpty()) {
            return Optional.empty();
        }

        String selectedSkin = skins.size() == 1 ? skins.get(0) : skins.get(ThreadLocalRandom.current().nextInt(skins.size()));

        return skinStorage.findSkinData(selectedSkin).map(InputDataResult::getProperty);
    }
}
