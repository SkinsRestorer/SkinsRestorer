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
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.config.LoginConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.floodgate.FloodgateUtil;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerStorageImpl implements PlayerStorage {
    private final SettingsManager settings;
    private final SkinStorageImpl skinStorage;
    private final SRLogger logger;
    private final AdapterReference adapterReference;
    private final RecommendationsState recommendationsState;

    @Override
    public Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                return Optional.ofNullable(playerData.getSkinIdentifier());
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get skin data of player %s".formatted(uuid), e);
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
                adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, identifier, List.of(), List.of()));
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to set skin data of player %s".formatted(uuid), e);
        }
    }

    public Optional<HistoryData> getTopOfHistory(UUID uuid, int skip) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                return playerData.getHistory().stream()
                        .sorted(Comparator.comparing(HistoryData::getTimestamp).reversed())
                        .skip(skip)
                        .findFirst();
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get skin data of player %s".formatted(uuid), e);
        }

        return Optional.empty();
    }

    public int getHistoryCount(UUID uuid) {
        return getHistoryEntries(uuid, 0, Integer.MAX_VALUE).size();
    }

    public List<HistoryData> getHistoryEntries(UUID uuid, int skip, int limit) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                return playerData.getHistory().stream()
                        .sorted(Comparator.comparing(HistoryData::getTimestamp).reversed())
                        .skip(skip)
                        .limit(limit)
                        .toList();
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get skin data of player %s".formatted(uuid), e);
        }

        return List.of();
    }

    public void pushToHistory(UUID uuid, HistoryData historyData) {
        int maxHistoryLength = settings.getProperty(CommandConfig.MAX_HISTORY_LENGTH);
        if (maxHistoryLength <= 0) {
            return;
        }

        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);
            boolean topIsSame = getTopOfHistory(uuid, 0).map(data -> data.getSkinIdentifier().equals(historyData.getSkinIdentifier())).orElse(false);
            if (topIsSame) {
                // don't push the same skin to the top of the history
                return;
            }

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setHistory(Stream.concat(playerData.getHistory().stream(), Stream.of(historyData))
                        .sorted(Comparator.comparing(HistoryData::getTimestamp))
                        .skip(Math.max(0, playerData.getHistory().size() + 1 - maxHistoryLength))
                        .toList());

                adapterReference.get().setPlayerData(uuid, playerData);
            } else {
                adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, null, List.of(historyData), List.of()));
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to push skin data of player %s".formatted(uuid), e);
        }
    }

    public void removeFromHistory(UUID uuid, HistoryData historyData) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setHistory(playerData.getHistory().stream().filter(data -> !data.equals(historyData)).toList());

                adapterReference.get().setPlayerData(uuid, playerData);
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to remove skin data of player %s".formatted(uuid), e);
        }
    }

    public Optional<FavouriteData> getFavouriteData(UUID uuid, SkinIdentifier skinIdentifier) {
        return getFavouriteEntries(uuid, 0, Integer.MAX_VALUE).stream()
                .filter(data -> data.getSkinIdentifier().equals(skinIdentifier))
                .findFirst();
    }

    public int getFavouriteCount(UUID uuid) {
        return getFavouriteEntries(uuid, 0, Integer.MAX_VALUE).size();
    }

    public List<FavouriteData> getFavouriteEntries(UUID uuid, int skip, int limit) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                return playerData.getFavourites().stream()
                        .sorted(Comparator.comparing(FavouriteData::getTimestamp).reversed())
                        .skip(skip)
                        .limit(limit)
                        .toList();
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get skin data of player %s".formatted(uuid), e);
        }

        return List.of();
    }

    public void addFavourite(UUID uuid, FavouriteData favouriteData) {
        int maxFavourites = settings.getProperty(CommandConfig.MAX_FAVOURITE_LENGTH);
        if (maxFavourites <= 0) {
            return;
        }

        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);
            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                boolean alreadyIsFavourite = playerData.getFavourites().stream().anyMatch(data -> data.getSkinIdentifier().equals(favouriteData.getSkinIdentifier()));
                if (alreadyIsFavourite) {
                    // don't add the same skin to the favourites
                    return;
                }

                playerData.setFavourites(Stream.concat(playerData.getFavourites().stream(), Stream.of(favouriteData))
                        .sorted(Comparator.comparing(FavouriteData::getTimestamp))
                        .skip(Math.max(0, playerData.getHistory().size() + 1 - maxFavourites))
                        .toList());

                adapterReference.get().setPlayerData(uuid, playerData);
            } else {
                adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, null, List.of(), List.of(favouriteData)));
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to push skin data of player %s".formatted(uuid), e);
        }
    }

    public void removeFavourite(UUID uuid, SkinIdentifier skinIdentifier) {
        try {
            Optional<PlayerData> optional = adapterReference.get().getPlayerData(uuid);

            if (optional.isPresent()) {
                PlayerData playerData = optional.get();
                playerData.setFavourites(playerData.getFavourites().stream().filter(data -> !data.getSkinIdentifier().equals(skinIdentifier)).toList());

                adapterReference.get().setPlayerData(uuid, playerData);
            }
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to remove skin data of player %s".formatted(uuid), e);
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
            logger.severe("Failed to remove skin data of player %s".formatted(uuid), e);
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
            logger.debug("Player %s is a Floodgate player, not searching for java skin.".formatted(playerName));
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

        String selectedSkin = skins.size() == 1 ? skins.getFirst() : SRHelpers.getRandomEntry(skins);
        if ("<random>".equalsIgnoreCase(selectedSkin)) {
            var randomRecommendation = recommendationsState.getRandomRecommendation();
            if (randomRecommendation.isPresent()) {
                selectedSkin = SkinStorageImpl.RECOMMENDATION_PREFIX + randomRecommendation.get().getSkinId();
            } else {
                logger.warning("No recommendations available for default skin, please ensure SkinsRestorer can reach the internet.");
                return Optional.empty();
            }
        }

        // If the selected skin is a recommendation, we need to use the findOrCreate method
        try {
            return skinStorage.findOrCreateSkinData(selectedSkin)
                    .map(result -> new SkinForResult(result.getIdentifier(), result.getProperty()));
        } catch (DataRequestException | MineSkinException e) {
            logger.warning("Failed to get default skin data for %s".formatted(selectedSkin), e);
            return Optional.empty();
        }
    }

    private record SkinForResult(SkinIdentifier identifier, SkinProperty property) {
    }
}
