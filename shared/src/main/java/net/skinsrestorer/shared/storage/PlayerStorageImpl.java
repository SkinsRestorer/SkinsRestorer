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
package net.skinsrestorer.shared.storage;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.floodgate.FloodgateUtil;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerStorageImpl implements PlayerStorage {
    private final SettingsManager settings;
    private final SkinStorageImpl skinStorage;
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
            logger.severe("Failed to get skin data of player " + uuid, e);
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
            logger.severe("Failed to set skin data of player " + uuid, e);
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
            logger.severe("Failed to remove skin data of player " + uuid, e);
        }
    }

    @Override
    public Optional<SkinProperty> getSkinOfPlayer(UUID uuid) {
        return getSkinIdOfPlayer(uuid).flatMap(skinStorage::getSkinDataByIdentifier);
    }

    @Override
    public Optional<SkinIdentifier> getSkinIdForPlayer(UUID uuid, String playerName, boolean isOnlineMode) throws DataRequestException {
        return getSkinForPlayerResult(uuid, playerName, isOnlineMode, false).map(SkinForResult::identifier);
    }

    @Override
    public Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName, boolean isOnlineMode) throws DataRequestException {
        return getSkinForPlayerResult(uuid, playerName, isOnlineMode, true).map(SkinForResult::property);
    }

    private Optional<SkinForResult> getSkinForPlayerResult(UUID uuid, String playerName, boolean isOnlineMode, boolean requireProperty) throws DataRequestException {
        Optional<SkinIdentifier> setSkin = getSkinIdOfPlayer(uuid);

        if (setSkin.isPresent()) {
            if (requireProperty) {
                return setSkin.flatMap(skinStorage::getSkinDataByIdentifier).map(property -> new SkinForResult(setSkin.get(), property));
            } else {
                return Optional.of(new SkinForResult(setSkin.get(), null));
            }
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

        Optional<MojangSkinDataResult> premiumSkin = skinStorage.getPlayerSkin(playerName, false);

        if (premiumSkin.isPresent()) {
            return premiumSkin.map(result -> new SkinForResult(SkinIdentifier.ofPlayer(result.getUniqueId()), result.getSkinProperty()));
        }

        if (defaultSkinsEnabled) {
            return getDefaultSkin();
        }

        return Optional.empty();
    }

    private Optional<SkinForResult> getDefaultSkin() {
        // return default skin name if user has no custom skin set, or we want to clear to default
        List<String> skins = settings.getProperty(StorageConfig.DEFAULT_SKINS);

        // return player name if there are no default skins set
        if (skins.isEmpty()) {
            return Optional.empty();
        }

        String selectedSkin = skins.size() == 1 ? skins.get(0) : SRHelpers.getRandomEntry(skins);

        return skinStorage.findSkinData(selectedSkin).map(result -> new SkinForResult(result.getIdentifier(), result.getProperty()));
    }

    private record SkinForResult(SkinIdentifier identifier, SkinProperty property) {
    }
}
