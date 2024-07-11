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
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.gui.GUISkinEntry;
import net.skinsrestorer.shared.gui.PageInfo;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.skin.*;
import net.skinsrestorer.shared.utils.GUIUtils;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
            try {
                findOrCreateSkinData(skin);
            } catch (DataRequestException | MineSkinException e) {
                logger.debug(String.format("DefaultSkin '%s' could not be found or requested! Removing from list..", skin), e);
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
            logger.warning("Failed to update skin data for " + uuid, e);
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
            logger.warning("Failed to get skin from cache for " + nameOrUniqueId, e);
            return Optional.empty();
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, String lastKnownName, SkinProperty textures, long timestamp) {
        adapterReference.get().setPlayerSkinData(uuid, PlayerSkinData.of(uuid, lastKnownName, textures, timestamp));
    }

    @Override
    public void setURLSkinData(String url, String mineSkinId, SkinProperty textures, SkinVariant skinVariant) {
        adapterReference.get().setURLSkinData(url, URLSkinData.of(url, mineSkinId, textures, skinVariant));
    }

    @Override
    public void setURLSkinIndex(String url, SkinVariant skinVariant) {
        adapterReference.get().setURLSkinIndex(url, URLIndexData.of(url, skinVariant));
    }

    @Override
    public void setCustomSkinData(String skinName, SkinProperty textures) {
        adapterReference.get().setCustomSkinData(skinName, CustomSkinData.of(skinName, textures));
    }

    public PageInfo getGUIPage(int page) {
        return GUIUtils.getGUIPage(page, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED);
            }

            @Override
            public int getIndex() {
                return settings.getProperty(GUIConfig.CUSTOM_GUI_INDEX);
            }

            @Override
            public int getTotalSkins() {
                return adapterReference.get().getTotalCustomSkins();
            }

            @Override
            public List<GUISkinEntry> getGUISkins(int offset, int limit) {
                return adapterReference.get().getCustomGUISkins(offset, limit);
            }
        }, new GUIUtils.GUIDataSource() {
            @Override
            public boolean isEnabled() {
                return settings.getProperty(GUIConfig.RECOMMENDATIONS_GUI_ENABLED);
            }

            @Override
            public int getIndex() {
                return settings.getProperty(GUIConfig.RECOMMENDATIONS_GUI_INDEX);
            }

            @Override
            public int getTotalSkins() {
                return recommendationsState.getRecommendationsCount();
            }

            @Override
            public List<GUISkinEntry> getGUISkins(int offset, int limit) {
                return Arrays.stream(recommendationsState.getRecommendationsOffset(offset, limit))
                        .map(r -> new GUISkinEntry(
                                RECOMMENDATION_PREFIX + r.getSkinId(),
                                r.getSkinName(),
                                PropertyUtils.getSkinTextureHash(r.getValue())
                        ))
                        .toList();
            }
        });
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
            logger.warning("Failed to find skin data for " + input, e);
        }

        return Optional.empty();
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
                case PLAYER -> adapterReference.get().getPlayerSkinData(UUID.fromString(identifier.getIdentifier()))
                        .map(PlayerSkinData::getProperty);
                case URL ->
                        adapterReference.get().getURLSkinData(identifier.getIdentifier(), identifier.getSkinVariant())
                                .map(URLSkinData::getProperty);
                case CUSTOM -> {
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
            logger.warning("Failed to get skin data for " + identifier, e);
            return Optional.empty();
        }
    }

    @Override
    public void removeSkinData(SkinIdentifier identifier) {
        switch (identifier.getSkinType()) {
            case PLAYER -> adapterReference.get().removePlayerSkinData(UUID.fromString(identifier.getIdentifier()));
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
        long expiryDate = timestamp + TimeUnit.MINUTES.toSeconds(Math.max(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER),5));

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
