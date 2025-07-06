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
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.skin.*;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinStorageImpl implements SkinStorage {
    public static final String RECOMMENDATION_PREFIX = "sr-recommendation-";
    private final SRLogger logger;
    private final CacheStorageImpl cacheStorage;
    private final MojangAPIImpl mojangAPI;
    private final MineSkinAPIImpl mineSkinAPI;
    private final SettingsManager settings;
    private final AdapterReference adapterReference;
    private final RecommendationsState recommendationsState;

    public void preloadDefaultSkins() {
        if (!settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED)) {
            return;
        }

        List<String> toRemove = new ArrayList<>();
        List<String> defaultSkins = new ArrayList<>(settings.getProperty(StorageConfig.DEFAULT_SKINS));
        defaultSkins.forEach(skin -> {
            if (skin.equalsIgnoreCase("<random>")) {
                return;
            }

            try {
                findOrCreateSkinData(skin);
            } catch (DataRequestException | MineSkinException e) {
                logger.debug("DefaultSkin '%s' could not be found or requested! Removing from list..".formatted(skin), e);
                toRemove.add(skin);
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
        return updatePlayerSkinData(uuid, mojangAPI::getProfileMojang, false, true);
    }

    private Optional<SkinProperty> updatePlayerSkinData(UUID uuid, ProfileGetter profileGetter, boolean skipDbLookup, boolean ignoreExpiry) throws DataRequestException {
        try {
            Optional<PlayerSkinData> optionalData = skipDbLookup ? Optional.empty() : adapterReference.get().getPlayerSkinData(uuid);
            Optional<SkinProperty> currentSkin = optionalData.map(PlayerSkinData::getProperty);

            long timestamp = -1;
            if (optionalData.isPresent()) {
                PlayerSkinData currentSkinData = optionalData.get();
                if (!ignoreExpiry && !isPlayerSkinExpired(currentSkinData.getTimestamp())) {
                    // We have valid data, let's return it
                    return currentSkin;
                } else {
                    timestamp = PropertyUtils.getSkinProfileData(currentSkinData.getProperty()).getTimestamp();
                }
            }

            Optional<SkinProperty> skinProperty = profileGetter.getProfile(uuid);
            if (skinProperty.isEmpty()) {
                return currentSkin;
            }

            MojangProfileResponse response = PropertyUtils.getSkinProfileData(skinProperty.get());

            if (response.getTimestamp() <= timestamp) {
                return currentSkin; // API even returned older skin data
            }

            setPlayerSkinData(uuid, response.getProfileName(), skinProperty.get(), SRHelpers.getEpochSecond());
            return skinProperty;
        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to update skin data for %s".formatted(uuid), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<MojangSkinDataResult> getPlayerSkin(String nameOrUniqueId, boolean allowExpired) throws DataRequestException {
        return getPlayerSkin(nameOrUniqueId, allowExpired, false);
    }

    private Optional<MojangSkinDataResult> getPlayerSkin(String nameOrUniqueId, boolean allowExpired, boolean skipDbLookup) throws DataRequestException {
        Optional<UUID> uuidParseResult = UUIDUtils.tryParseUniqueId(nameOrUniqueId);
        if (ValidationUtil.invalidMinecraftUsername(nameOrUniqueId) && uuidParseResult.isEmpty()) {
            return Optional.empty();
        }

        try {
            // We already know the UUID, so nothing to do here
            if (uuidParseResult.isEmpty()) {
                Optional<MojangCacheData> cached = cacheStorage.getCachedData(nameOrUniqueId, allowExpired);
                if (cached.isPresent()) {
                    Optional<UUID> optionalUUID = cached.get().getUniqueId();

                    // User does not exist
                    if (optionalUUID.isEmpty()) {
                        return Optional.empty();
                    }

                    UUID uuid = optionalUUID.get();
                    return updatePlayerSkinData(uuid, mojangAPI::getProfile, skipDbLookup, false)
                            .map(skinProperty -> MojangSkinDataResult.of(uuid, skinProperty));
                }
            }

            Optional<MojangSkinDataResult> optional = mojangAPI.getSkin(nameOrUniqueId);

            // Only cache name -> UUID if this is a name and not a UUID
            if (uuidParseResult.isEmpty()) {
                adapterReference.get().setCachedUUID(nameOrUniqueId,
                        MojangCacheData.of(optional.map(MojangSkinDataResult::getUniqueId).orElse(null),
                                SRHelpers.getEpochSecond()));
            }

            // Cache the skin data
            if (optional.isPresent()) {
                MojangSkinDataResult result = optional.get();
                return updatePlayerSkinData(result.getUniqueId(), uuid -> Optional.of(result.getSkinProperty()), skipDbLookup, false)
                        .map(skinProperty -> MojangSkinDataResult.of(result.getUniqueId(), skinProperty));
            }

            return optional;
        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to get skin from cache for %s".formatted(nameOrUniqueId), e);
            return Optional.empty();
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, String lastKnownName, SkinProperty property, long timestamp) {
        adapterReference.get().setPlayerSkinData(uuid, PlayerSkinData.of(uuid, lastKnownName, property, timestamp));
    }

    @Override
    public void setURLSkinData(String url, String mineSkinId, SkinProperty property, SkinVariant skinVariant) {
        adapterReference.get().setURLSkinData(url, URLSkinData.of(url, mineSkinId, property, skinVariant));
    }

    @Override
    public void setURLSkinIndex(String url, SkinVariant skinVariant) {
        adapterReference.get().setURLSkinIndex(url, URLIndexData.of(url, skinVariant));
    }

    @Override
    public void setCustomSkinData(String skinName, SkinProperty property) {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);

        adapterReference.get().setCustomSkinData(skinName, CustomSkinData.of(skinName, null, property));
    }

    public void setCustomSkinDisplayName(String skinName, ComponentString displayName) throws StorageAdapter.StorageException {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);

        CustomSkinData customSkinData = adapterReference.get().getCustomSkinData(skinName)
                .orElseThrow(() -> new IllegalArgumentException("Skin not found"));

        adapterReference.get().setCustomSkinData(skinName, CustomSkinData.of(skinName, displayName, customSkinData.getProperty()));
    }

    @Override
    public Optional<InputDataResult> findSkinData(String input, SkinVariant skinVariantHint) {
        input = SRHelpers.sanitizeSkinInput(input);

        try {
            if (ValidationUtil.validSkinUrl(input)) {
                SkinVariant skinVariant;
                if (skinVariantHint != null) {
                    skinVariant = skinVariantHint;
                } else {
                    Optional<URLIndexData> variant = adapterReference.get().getURLSkinIndex(input);
                    if (variant.isEmpty()) {
                        return Optional.empty();
                    }

                    skinVariant = variant.get().getSkinVariant();
                }

                return adapterReference.get().getURLSkinData(input, skinVariant).map(data ->
                        InputDataResult.of(SkinIdentifier.ofURL(data.getUrl(), skinVariant),
                                data.getProperty()));
            } else {
                Optional<InputDataResult> result = HardcodedSkins.getHardcodedSkin(input);

                if (result.isPresent()) {
                    return result;
                }

                Optional<CustomSkinData> customSkinData = adapterReference.get().getCustomSkinData(input);

                if (customSkinData.isPresent()) {
                    return customSkinData.map(data ->
                            InputDataResult.of(SkinIdentifier.ofCustom(data.getSkinName()), data.getProperty()));
                }

                Optional<UUID> uuid = cacheStorage.getUUID(input, false);

                if (uuid.isEmpty()) {
                    return Optional.empty();
                }

                Optional<PlayerSkinData> playerSkinData = adapterReference.get().getPlayerSkinData(uuid.get());

                if (playerSkinData.isPresent()) {
                    return playerSkinData.map(data ->
                            InputDataResult.of(SkinIdentifier.ofPlayer(uuid.get()), data.getProperty()));
                }
            }
        } catch (StorageAdapter.StorageException | DataRequestException e) {
            logger.warning("Failed to find skin data for %s".formatted(input), e);
        }

        return Optional.empty();
    }

    public ComponentString resolveSkinName(SkinIdentifier identifier) {
        return switch (identifier.getSkinType()) {
            case PLAYER -> {
                try {
                    yield ComponentHelper.convertPlainToJson(adapterReference.get().getPlayerSkinData(identifier.getPlayerUniqueId())
                            .map(PlayerSkinData::getLastKnownName)
                            .orElse(identifier.getIdentifier()));
                } catch (StorageAdapter.StorageException e) {
                    logger.warning("Failed to get skin data for %s".formatted(identifier), e);
                    yield ComponentHelper.convertPlainToJson(identifier.getIdentifier());
                }
            }
            case URL, LEGACY -> ComponentHelper.convertPlainToJson(identifier.getIdentifier());
            case CUSTOM -> {
                if (identifier.getIdentifier().startsWith(RECOMMENDATION_PREFIX)) {
                    RecommenationResponse.SkinInfo skinInfo = recommendationsState.getRecommendation(identifier.getIdentifier().substring(RECOMMENDATION_PREFIX.length()));
                    if (skinInfo != null) {
                        yield ComponentHelper.convertPlainToJson(skinInfo.getSkinName());
                    }
                }

                try {
                    yield adapterReference.get().getCustomSkinData(identifier.getIdentifier())
                            .flatMap(c -> Optional.ofNullable(c.getDisplayName()))
                            .orElse(ComponentHelper.convertPlainToJson(identifier.getIdentifier()));
                } catch (StorageAdapter.StorageException e) {
                    logger.warning("Failed to get skin data for %s".formatted(identifier), e);
                    yield ComponentHelper.convertPlainToJson(identifier.getIdentifier());
                }
            }
        };
    }

    @Override
    public Optional<InputDataResult> findOrCreateSkinData(String input, SkinVariant skinVariantHint) throws DataRequestException, MineSkinException {
        input = SRHelpers.sanitizeSkinInput(input);

        Optional<InputDataResult> skinData = findSkinData(input, skinVariantHint);

        if (skinData.isPresent()) {
            return skinData;
        }

        // Create new skin data
        if (input.startsWith(RECOMMENDATION_PREFIX)) {
            String skinId = input.substring(RECOMMENDATION_PREFIX.length());
            RecommenationResponse.SkinInfo skinInfo = recommendationsState.getRecommendation(skinId);

            if (skinInfo == null) {
                return Optional.empty();
            }

            SkinProperty skinProperty = skinInfo.getSkinProperty();
            setCustomSkinData(input, skinProperty);

            return Optional.of(InputDataResult.of(SkinIdentifier.ofCustom(input), skinProperty));
        } else if (ValidationUtil.validSkinUrl(input)) {
            MineSkinResponse response = mineSkinAPI.genSkin(input, skinVariantHint);

            setURLSkinByResponse(input, response);

            return Optional.of(InputDataResult.of(SkinIdentifier.ofURL(input, response.getGeneratedVariant()), response.getProperty()));
        } else {
            return getPlayerSkin(input, false, true).map(result ->
                    InputDataResult.of(SkinIdentifier.ofPlayer(result.getUniqueId()), result.getSkinProperty()));
        }
    }

    @Override
    public Optional<SkinProperty> getSkinDataByIdentifier(SkinIdentifier identifier) {
        try {
            return switch (identifier.getSkinType()) {
                case PLAYER -> adapterReference.get().getPlayerSkinData(identifier.getPlayerUniqueId())
                        .map(PlayerSkinData::getProperty);
                case URL ->
                        adapterReference.get().getURLSkinData(identifier.getIdentifier(), identifier.getSkinVariant())
                                .map(URLSkinData::getProperty);
                case CUSTOM -> {
                    if (identifier.getIdentifier().startsWith(RECOMMENDATION_PREFIX)) {
                        String skinId = identifier.getIdentifier().substring(RECOMMENDATION_PREFIX.length());
                        RecommenationResponse.SkinInfo skinInfo = recommendationsState.getRecommendation(skinId);

                        if (skinInfo == null) {
                            yield Optional.empty();
                        }

                        yield Optional.of(skinInfo.getSkinProperty());
                    }

                    Optional<SkinProperty> skinProperty = adapterReference.get().getCustomSkinData(identifier.getIdentifier())
                            .map(CustomSkinData::getProperty);
                    if (skinProperty.isPresent()) {
                        yield skinProperty;
                    } else {
                        yield HardcodedSkins.getHardcodedSkin(identifier.getIdentifier())
                                .map(InputDataResult::getProperty);
                    }
                }
                case LEGACY -> adapterReference.get().getLegacySkinData(identifier.getIdentifier())
                        .map(LegacySkinData::getProperty);
            };

        } catch (StorageAdapter.StorageException e) {
            logger.warning("Failed to get skin data for %s".formatted(identifier), e);
            return Optional.empty();
        }
    }

    @Override
    public void removeSkinData(SkinIdentifier identifier) {
        switch (identifier.getSkinType()) {
            case PLAYER -> adapterReference.get().removePlayerSkinData(identifier.getPlayerUniqueId());
            case URL ->
                    adapterReference.get().removeURLSkinData(identifier.getIdentifier(), identifier.getSkinVariant());
            case CUSTOM -> adapterReference.get().removeCustomSkinData(identifier.getIdentifier());
            case LEGACY -> adapterReference.get().removeLegacySkinData(identifier.getIdentifier());
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

        long now = SRHelpers.getEpochSecond();
        long expiryDate = timestamp + TimeUnit.MINUTES.toSeconds(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER));

        return expiryDate <= now;
    }

    public boolean purgeOldSkins(int days) {
        long targetPurgeTimestamp = Instant.now().minus(days, ChronoUnit.DAYS).getEpochSecond();

        try {
            adapterReference.get().purgeStoredOldSkins(targetPurgeTimestamp);
            return true; // TODO: Do better than true/false return
        } catch (StorageAdapter.StorageException e) {
            logger.severe("Failed to purge old skins", e);
            return false;
        }
    }

    private interface ProfileGetter {
        Optional<SkinProperty> getProfile(UUID uuid) throws DataRequestException;
    }
}
