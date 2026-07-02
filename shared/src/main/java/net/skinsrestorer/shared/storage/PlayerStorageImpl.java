/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
import net.skinsrestorer.shared.skinsafety.SkinSafetyDecision;
import net.skinsrestorer.shared.skinsafety.SkinSafetyService;
import net.skinsrestorer.shared.skinsafety.SkinSafetyState;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
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
    private final RecommendationsState recommendationsState;
    private final SkinSafetyService skinSafetyService;
    private final SkinSafetyState skinSafetyState;

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
        adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, identifier));
    }

    public Optional<HistoryData> getTopOfHistory(UUID uuid, int skip) {
        int maxHistoryLength = settings.getProperty(CommandConfig.MAX_HISTORY_LENGTH);
        if (skip >= maxHistoryLength) {
            return Optional.empty();
        }

        try {
            List<HistoryData> result = adapterReference.get().getPlayerHistory(uuid, skip, 1);
            return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get skin data of player %s".formatted(uuid), e);
        }

        return Optional.empty();
    }

    public int getHistoryCount(UUID uuid) {
        int maxHistoryLength = settings.getProperty(CommandConfig.MAX_HISTORY_LENGTH);
        try {
            return Math.min(adapterReference.get().getPlayerHistoryCount(uuid), maxHistoryLength);
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get history count of player %s".formatted(uuid), e);
        }

        return 0;
    }

    public List<HistoryData> getHistoryEntries(UUID uuid, int skip, int limit) {
        int maxHistoryLength = settings.getProperty(CommandConfig.MAX_HISTORY_LENGTH);
        if (skip >= maxHistoryLength) {
            return List.of();
        }

        int effectiveLimit = Math.min(limit, maxHistoryLength - skip);
        try {
            return adapterReference.get().getPlayerHistory(uuid, skip, effectiveLimit);
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

        boolean topIsSame = getTopOfHistory(uuid, 0).map(data -> data.getSkinIdentifier().equals(historyData.getSkinIdentifier())).orElse(false);
        if (topIsSame) {
            // don't push the same skin to the top of the history
            return;
        }

        adapterReference.get().addPlayerHistory(uuid, historyData);
        adapterReference.get().trimPlayerHistory(uuid, maxHistoryLength);
    }

    public void removeFromHistory(UUID uuid, HistoryData historyData) {
        adapterReference.get().removePlayerHistory(uuid, historyData.getTimestamp());
    }

    public Optional<FavouriteData> getFavouriteData(UUID uuid, SkinIdentifier skinIdentifier) {
        int maxFavourites = settings.getProperty(CommandConfig.MAX_FAVOURITE_LENGTH);
        try {
            return adapterReference.get().getPlayerFavourites(uuid, 0, maxFavourites).stream()
                    .filter(data -> data.getSkinIdentifier().equals(skinIdentifier))
                    .findFirst();
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get favourite data of player %s".formatted(uuid), e);
        }

        return Optional.empty();
    }

    public int getFavouriteCount(UUID uuid) {
        int maxFavourites = settings.getProperty(CommandConfig.MAX_FAVOURITE_LENGTH);
        try {
            return Math.min(adapterReference.get().getPlayerFavouriteCount(uuid), maxFavourites);
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to get favourite count of player %s".formatted(uuid), e);
        }

        return 0;
    }

    public List<FavouriteData> getFavouriteEntries(UUID uuid, int skip, int limit) {
        int maxFavourites = settings.getProperty(CommandConfig.MAX_FAVOURITE_LENGTH);
        if (skip >= maxFavourites) {
            return List.of();
        }

        int effectiveLimit = Math.min(limit, maxFavourites - skip);
        try {
            return adapterReference.get().getPlayerFavourites(uuid, skip, effectiveLimit);
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

        boolean alreadyIsFavourite = getFavouriteData(uuid, favouriteData.getSkinIdentifier()).isPresent();
        if (alreadyIsFavourite) {
            // don't add the same skin to the favourites
            return;
        }

        adapterReference.get().addPlayerFavourite(uuid, favouriteData);
        adapterReference.get().trimPlayerFavourites(uuid, maxFavourites);
    }

    public void removeFavourite(UUID uuid, SkinIdentifier skinIdentifier) {
        adapterReference.get().removePlayerFavourite(uuid, skinIdentifier);
    }

    @Override
    public void removeSkinIdOfPlayer(UUID uuid) {
        adapterReference.get().setPlayerData(uuid, PlayerData.of(uuid, null));
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
                return setSkin.flatMap(skinStorage::getSkinDataByIdentifier)
                        .map(property -> new SkinForResult(setSkin.get(), property))
                        .flatMap(result -> applySkinSafety(uuid, playerName, result));
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
            return getDefaultSkin().flatMap(result -> applySkinSafety(uuid, playerName, result));
        }

        Optional<MojangSkinDataResult> premiumSkin = skinStorage.getPlayerSkin(playerName, false);

        if (premiumSkin.isPresent()) {
            return premiumSkin.map(result -> new SkinForResult(SkinIdentifier.ofPlayer(result.getUniqueId()), result.getSkinProperty()))
                    .flatMap(result -> applySkinSafety(uuid, playerName, result));
        }

        if (defaultSkinsEnabled) {
            return getDefaultSkin().flatMap(result -> applySkinSafety(uuid, playerName, result));
        }

        return Optional.empty();
    }

    private Optional<SkinForResult> applySkinSafety(UUID uuid, String playerName, SkinForResult result) {
        SkinSafetyDecision decision = skinSafetyService.checkPlayerSkin(uuid, playerName, result.property());
        if (!decision.matched()) {
            return Optional.of(result);
        }

        skinSafetyService.logMatch("player %s (%s)".formatted(playerName, uuid), decision);
        if (!decision.shouldBlock()) {
            return Optional.of(result);
        }

        var fallback = skinSafetyState.resolveFallbackSkin(skinStorage, skinSafetyService);
        return Optional.of(new SkinForResult(fallback.getIdentifier(), fallback.getProperty()));
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
