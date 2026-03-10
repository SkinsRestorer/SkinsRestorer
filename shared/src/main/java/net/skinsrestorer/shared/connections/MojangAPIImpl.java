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
package net.skinsrestorer.shared.connections;

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.config.APIConfig;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.profile.EclipseProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.MojangProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.PropertyResponse;
import net.skinsrestorer.shared.connections.responses.uuid.EclipseUUIDResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MojangUUIDResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.RateLimitBackoff;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MojangAPIImpl implements MojangAPI {
    private static final String UUID_ECLIPSE = "https://eclipse.skinsrestorer.net/mojang/uuid/%playerName%";
    private static final String UUID_ELYBY = "https://account.ely.by/api/mojang/profiles/%playerName%";
    private static final String PROFILE_ECLIPSE = "https://eclipse.skinsrestorer.net/mojang/skin/%uuid%";
    private static final String PROFILE_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String PROFILE_ELYBY = "https://account.ely.by/api/minecraft/session/profile/%uuid%";
    private static final String PROFILE_ELYBY_SIGNED = "https://skinsystem.ely.by/textures/signed/%playerName%";
    private static final String BATCH_UUID_NEW_ENDPOINT = "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname";
    private static final String BATCH_UUID_LEGACY_ENDPOINT = "https://api.mojang.com/profiles/minecraft";

    private final MetricsCounter metricsCounter;
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final HttpClient httpClient;
    private final SettingsManager settings;

    private final MojangBatchAPI newBatchAPI;
    private final MojangBatchAPI legacyBatchAPI;

    @Inject
    public MojangAPIImpl(MetricsCounter metricsCounter, SRLogger logger, SRPlugin plugin, HttpClient httpClient, SettingsManager settings) {
        this.metricsCounter = metricsCounter;
        this.logger = logger;
        this.plugin = plugin;
        this.httpClient = httpClient;
        this.settings = settings;

        RateLimitBackoff rateLimitBackoff = new RateLimitBackoff();

        // Create batch API instances with different endpoints
        this.newBatchAPI = new MojangBatchAPI(
                metricsCounter,
                logger,
                plugin.getAdapter(),
                httpClient,
                settings,
                BATCH_UUID_NEW_ENDPOINT,
                plugin.getUserAgent(),
                rateLimitBackoff
        );

        this.legacyBatchAPI = new MojangBatchAPI(
                metricsCounter,
                logger,
                plugin.getAdapter(),
                httpClient,
                settings,
                BATCH_UUID_LEGACY_ENDPOINT,
                plugin.getUserAgent(),
                rateLimitBackoff
        );
    }

    @Override
    public Optional<MojangSkinDataResult> getSkin(String nameOrUniqueId) throws DataRequestException {
        // If it is not a valid Minecraft username or UUID, return empty
        Optional<UUID> uuidParseResult = UUIDUtils.tryParseUniqueId(nameOrUniqueId);
        if (ValidationUtil.invalidMinecraftUsername(nameOrUniqueId) && uuidParseResult.isEmpty()) {
            return Optional.empty();
        }

        // If it is a valid UUID, we can just use it directly, otherwise we need to get the UUID from the name
        Optional<UUID> uuidResult = uuidParseResult.isEmpty() ? getUUID(nameOrUniqueId) : uuidParseResult;
        if (uuidResult.isEmpty()) {
            return Optional.empty();
        }

        // Get the profile data for the UUID
        return getProfile(uuidResult.get()).flatMap(propertyResponse ->
                Optional.of(MojangSkinDataResult.of(uuidResult.get(), propertyResponse)));
    }

    /**
     * Get the uuid from a player playerName
     *
     * @param playerName Mojang username of the player
     * @return String uuid trimmed (without dashes)
     */
    public Optional<UUID> getUUID(String playerName) throws DataRequestException {
        if (ValidationUtil.invalidMinecraftUsername(playerName)) {
            return Optional.empty();
        }

        List<Throwable> suppressedExceptions = new ArrayList<>();
        if (settings.getProperty(APIConfig.ELYBY_ENABLED)) {
            try {
                Optional<UUID> uuid = getUUIDElyBy(playerName);
                if (uuid.isPresent()) {
                    return uuid;
                }
            } catch (DataRequestException e) {
                logger.debug(e);
                suppressedExceptions.add(e);
            }
        }

        try {
            return getUUIDMojang(playerName, newBatchAPI);
        } catch (DataRequestException e) {
            logger.debug(e);
            suppressedExceptions.add(e);
        }

        // Fallback to Mojang old API
        try {
            return getUUIDMojang(playerName, legacyBatchAPI);
        } catch (DataRequestException e) {
            logger.debug(e);
            suppressedExceptions.add(e);
        }

        // Fall back to Eclipse API
        try {
            return getUUIDEclipse(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
            suppressedExceptions.add(e);
        }

        DataRequestExceptionShared error = new DataRequestExceptionShared("Failed to get UUID for player: %s".formatted(playerName));
        for (Throwable t : suppressedExceptions) {
            error.addSuppressed(t);
        }

        throw error;
    }

    public Optional<UUID> getUUIDMojang(String playerName) throws DataRequestException {
        return getUUIDMojang(playerName, newBatchAPI);
    }

    public Optional<UUID> getUUIDMojang(String playerName, MojangBatchAPI batchAPI) throws DataRequestException {
        try {
            return batchAPI.getUUID(playerName).get(1, TimeUnit.MINUTES);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new DataRequestExceptionShared(e);
        }
    }

    public Optional<UUID> getUUIDEclipse(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(UUID_ECLIPSE.replace("%playerName%", playerName)), MetricsCounter.Service.ECLIPSE_UUID);
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Eclipse error: %d".formatted(httpResponse.statusCode()));
        }

        EclipseUUIDResponse response = httpResponse.getBodyAs(EclipseUUIDResponse.class);
        return Optional.ofNullable(response.uuid());
    }

    public Optional<UUID> getUUIDElyBy(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(UUID_ELYBY.replace("%playerName%", playerName)), MetricsCounter.Service.ELYBY_UUID);
        if (httpResponse.statusCode() == 204) {
            return Optional.empty();
        }
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Ely.by error: %d".formatted(httpResponse.statusCode()));
        }

        MojangUUIDResponse response = httpResponse.getBodyAs(MojangUUIDResponse.class);
        if (response.getId() == null || response.getId().isEmpty()) {
            return Optional.empty();
        }

        return UUIDUtils.tryParseUniqueId(response.getId());
    }

    public Optional<SkinProperty> getProfile(UUID uuid) throws DataRequestException {
        if (settings.getProperty(APIConfig.ELYBY_ENABLED)) {
            try {
                Optional<SkinProperty> elyByProfile = getProfileElyBy(uuid);
                if (elyByProfile.isPresent()) {
                    return elyByProfile;
                }
            } catch (DataRequestException e) {
                logger.debug(e);
            }
        }

        try {
            return getProfileMojang(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        // Fall back to Eclipse API
        try {
            return getProfileEclipse(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        throw new DataRequestExceptionShared("Failed to get profile for player: %s".formatted(uuid));
    }

    /**
     * Get a fresh profile directly from the source, bypassing caching.
     * When Ely.by is enabled, prefer Ely.by and fall back to Mojang when needed.
     * Used by /skin update.
     */
    public Optional<SkinProperty> getProfileFresh(UUID uuid) throws DataRequestException {
        if (settings.getProperty(APIConfig.ELYBY_ENABLED)) {
            Optional<SkinProperty> elyByProfile = getProfileElyBy(uuid);
            if (elyByProfile.isPresent()) {
                return elyByProfile;
            }
        }

        return getProfileMojang(uuid);
    }

    public Optional<SkinProperty> getProfileMojang(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(PROFILE_MOJANG.replace("%uuid%", UUIDUtils.convertToNoDashes(uuid))), MetricsCounter.Service.MOJANG_PROFILE);
        MojangProfileResponse response = httpResponse.getBodyAs(MojangProfileResponse.class);
        if (response.getProperties() == null) {
            return Optional.empty();
        }

        PropertyResponse property = response.getProperties()[0];
        if (property.getValue().isEmpty() || property.getSignature().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(SkinProperty.of(property.getValue(), property.getSignature()));
    }

    public Optional<SkinProperty> getProfileEclipse(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(PROFILE_ECLIPSE.replace("%uuid%", uuid.toString())), MetricsCounter.Service.ECLIPSE_PROFILE);
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Eclipse error: %d".formatted(httpResponse.statusCode()));
        }

        EclipseProfileResponse response = httpResponse.getBodyAs(EclipseProfileResponse.class);
        if (response.skinProperty() == null) {
            return Optional.empty();
        }

        return Optional.of(SkinProperty.of(response.skinProperty().value(), response.skinProperty().signature()));
    }

    public Optional<SkinProperty> getProfileElyBy(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(PROFILE_ELYBY.replace("%uuid%", UUIDUtils.convertToNoDashes(uuid))), MetricsCounter.Service.ELYBY_PROFILE);
        if (httpResponse.statusCode() == 204) {
            return Optional.empty();
        }
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Ely.by error: %d".formatted(httpResponse.statusCode()));
        }

        MojangProfileResponse response = httpResponse.getBodyAs(MojangProfileResponse.class);
        if (response.getName() == null || response.getName().isEmpty()) {
            return Optional.empty();
        }

        return getProfileElyBySigned(response.getName());
    }

    private Optional<SkinProperty> getProfileElyBySigned(String playerName) throws DataRequestException {
        if (ValidationUtil.invalidMinecraftUsername(playerName)) {
            return Optional.empty();
        }

        HttpResponse httpResponse = readURL(URI.create(PROFILE_ELYBY_SIGNED.replace("%playerName%", playerName)), MetricsCounter.Service.ELYBY_PROFILE);
        if (httpResponse.statusCode() == 204) {
            return Optional.empty();
        }
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Ely.by error: %d".formatted(httpResponse.statusCode()));
        }

        MojangProfileResponse response = httpResponse.getBodyAs(MojangProfileResponse.class);
        if (response.getProperties() == null) {
            return Optional.empty();
        }

        for (PropertyResponse property : response.getProperties()) {
            if (!"textures".equals(property.getName())) {
                continue;
            }

            if (property.getValue() == null || property.getValue().isEmpty()
                    || property.getSignature() == null || property.getSignature().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(SkinProperty.of(property.getValue(), property.getSignature()));
        }

        return Optional.empty();
    }

    public Optional<SkinProperty> getProfileElyByName(String playerName) throws DataRequestException {
        return getProfileElyBySigned(playerName);
    }

    private HttpResponse readURL(URI uri, MetricsCounter.Service service) throws DataRequestException {
        metricsCounter.increment(service);

        try {
            return httpClient.execute(
                    uri,
                    null,
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.GET,
                    Collections.emptyMap(),
                    5000
            );
        } catch (IOException e) {
            logger.debug("Error while reading URL: %s".formatted(uri), e);
            throw new DataRequestExceptionShared(e);
        }
    }
}
