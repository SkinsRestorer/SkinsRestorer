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
package net.skinsrestorer.shared.connections;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.profile.EclipseProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.MineToolsProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.MojangProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.PropertyResponse;
import net.skinsrestorer.shared.connections.responses.uuid.EclipseUUIDResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MineToolsUUIDResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MojangUUIDResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MojangAPIImpl implements MojangAPI {
    private static final String UUID_ECLIPSE = "https://eclipse.skinsrestorer.net/mojang/uuid/%playerName%";
    private static final String UUID_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%playerName%";
    private static final String UUID_MINETOOLS = "https://api.minetools.eu/uuid/%playerName%";
    private static final String PROFILE_ECLIPSE = "https://eclipse.skinsrestorer.net/mojang/skin/%uuid%";
    private static final String PROFILE_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String PROFILE_MINETOOLS = "https://api.minetools.eu/profile/%uuid%";

    private final MetricsCounter metricsCounter;
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final HttpClient httpClient;

    @Override
    public Optional<MojangSkinDataResult> getSkin(String nameOrUniqueId) throws DataRequestException {
        Optional<UUID> uuidParseResult = UUIDUtils.tryParseUniqueId(nameOrUniqueId);
        if (ValidationUtil.invalidMinecraftUsername(nameOrUniqueId) && uuidParseResult.isEmpty()) {
            return Optional.empty();
        }

        Optional<UUID> uuidResult = uuidParseResult.isEmpty()
                ? getUUID(nameOrUniqueId) : uuidParseResult;
        if (uuidResult.isEmpty()) {
            return Optional.empty();
        }

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

        try {
            return getUUIDEclipse(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        // Fall back to Mojang API
        try {
            return getUUIDMojang(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        // Fall back to MineTools API
        try {
            return getUUIDMineTools(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        throw new DataRequestExceptionShared("Failed to get UUID for player: %s".formatted(playerName));
    }

    public Optional<UUID> getUUIDEclipse(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(UUID_ECLIPSE.replace("%playerName%", playerName)), MetricsCounter.Service.ECLIPSE_UUID);
        if (httpResponse.statusCode() != 200) {
            throw new DataRequestExceptionShared("Eclipse error: %d".formatted(httpResponse.statusCode()));
        }

        EclipseUUIDResponse response = httpResponse.getBodyAs(EclipseUUIDResponse.class);
        return Optional.ofNullable(response.uuid());
    }

    public Optional<UUID> getUUIDMojang(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(UUID_MOJANG.replace("%playerName%", playerName)), MetricsCounter.Service.MOJANG);

        // Not found
        if (httpResponse.statusCode() == 204 || httpResponse.statusCode() == 404 || httpResponse.body().isEmpty()) {
            return Optional.empty();
        }

        // Rate limited
        if (httpResponse.statusCode() == 429) {
            // TODO: Return http code to api and translate internally
            throw new DataRequestExceptionShared("Please wait a minute before requesting that skin again. (Rate Limited)");
        }

        MojangUUIDResponse response = httpResponse.getBodyAs(MojangUUIDResponse.class);
        if (response.getError() != null) {
            throw new DataRequestExceptionShared("Mojang error: %s".formatted(response.getError()));
        }

        return Optional.ofNullable(response.getId())
                .map(UUIDUtils::convertToDashed);
    }

    protected Optional<UUID> getUUIDMineTools(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(UUID_MINETOOLS.replace("%playerName%", playerName)), MetricsCounter.Service.MINE_TOOLS, 10_000);
        MineToolsUUIDResponse response = httpResponse.getBodyAs(MineToolsUUIDResponse.class);

        if (response.getStatus() != null && response.getStatus().equals("ERR")) {
            throw new DataRequestExceptionShared("MineTools error: %s".formatted(response.getStatus()));
        }

        return Optional.ofNullable(response.getId())
                .map(UUIDUtils::convertToDashed);
    }

    public Optional<SkinProperty> getProfile(UUID uuid) throws DataRequestException {
        try {
            return getProfileEclipse(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        // Fall back to Mojang API
        try {
            return getProfileMojang(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        // Fall back to MineTools API
        try {
            return getProfileMineTools(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
        }

        throw new DataRequestExceptionShared("Failed to get profile for player: %s".formatted(uuid));
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

    public Optional<SkinProperty> getProfileMojang(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(PROFILE_MOJANG.replace("%uuid%", UUIDUtils.convertToNoDashes(uuid))), MetricsCounter.Service.MOJANG);
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

    protected Optional<SkinProperty> getProfileMineTools(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(URI.create(PROFILE_MINETOOLS.replace("%uuid%", UUIDUtils.convertToNoDashes(uuid))), MetricsCounter.Service.MINE_TOOLS, 10_000);
        MineToolsProfileResponse response = httpResponse.getBodyAs(MineToolsProfileResponse.class);
        if (response.getRaw() == null) {
            return Optional.empty();
        }

        MineToolsProfileResponse.Raw raw = response.getRaw();
        if (raw.getStatus() != null && raw.getStatus().equals("ERR")) {
            throw new DataRequestExceptionShared("MineTools error: %s".formatted(raw.getStatus()));
        }

        PropertyResponse property = raw.getProperties()[0];
        if (property.getValue().isEmpty() || property.getSignature().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(SkinProperty.of(property.getValue(), property.getSignature()));
    }

    private HttpResponse readURL(URI uri, MetricsCounter.Service service) throws DataRequestException {
        return readURL(uri, service, 5_000);
    }

    private HttpResponse readURL(URI uri, MetricsCounter.Service service, int timeout) throws DataRequestException {
        metricsCounter.increment(service);

        try {
            return httpClient.execute(
                    uri,
                    null,
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.GET,
                    Collections.emptyMap(),
                    timeout
            );
        } catch (IOException e) {
            logger.debug("Error while reading URL: %s".formatted(uri), e);
            throw new DataRequestExceptionShared(e);
        }
    }
}
