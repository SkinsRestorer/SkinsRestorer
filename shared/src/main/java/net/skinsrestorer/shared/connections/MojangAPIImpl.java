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
package net.skinsrestorer.shared.connections;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.connections.MojangAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.connections.responses.AshconResponse;
import net.skinsrestorer.shared.connections.responses.profile.MineToolsProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.MojangProfileResponse;
import net.skinsrestorer.shared.connections.responses.profile.PropertyResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MineToolsUUIDResponse;
import net.skinsrestorer.shared.connections.responses.uuid.MojangUUIDResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.MetricsCounter;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MojangAPIImpl implements MojangAPI {
    private static final String ASHCON = "https://api.ashcon.app/mojang/v2/user/%uuidOrName%";
    private static final String UUID_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%playerName%";
    private static final String UUID_MINETOOLS = "https://api.minetools.eu/uuid/%playerName%";
    private static final String PROFILE_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String PROFILE_MINETOOLS = "https://api.minetools.eu/profile/%uuid%";

    private final MetricsCounter metricsCounter;
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final HttpClient httpClient;

    public static UUID convertToDashed(String noDashes) {
        StringBuilder idBuff = new StringBuilder(noDashes);
        idBuff.insert(20, '-');
        idBuff.insert(16, '-');
        idBuff.insert(12, '-');
        idBuff.insert(8, '-');
        return UUID.fromString(idBuff.toString());
    }

    public static String convertToNoDashes(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    @Override
    public Optional<MojangSkinDataResult> getSkin(String playerName) throws DataRequestException {
        if (!C.validMojangUsername(playerName)) {
            return Optional.empty();
        }

        try {
            return getDataAshcon(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
            Optional<UUID> uuidResult = getUUIDStartMojang(playerName);
            if (!uuidResult.isPresent()) {
                return Optional.empty();
            }

            return getProfileStartMojang(uuidResult.get()).flatMap(propertyResponse ->
                    Optional.of(MojangSkinDataResult.of(uuidResult.get(), propertyResponse)));
        }
    }

    /**
     * Get the uuid from a player playerName
     *
     * @param playerName Mojang username of the player
     * @return String uuid trimmed (without dashes)
     */
    public Optional<UUID> getUUID(String playerName) throws DataRequestException {
        if (!C.validMojangUsername(playerName)) {
            return Optional.empty();
        }

        try {
            return getDataAshcon(playerName).map(MojangSkinDataResult::getUniqueId);
        } catch (DataRequestException e) {
            logger.debug(e);
            return getUUIDStartMojang(playerName);
        }
    }

    private Optional<UUID> getUUIDStartMojang(String playerName) throws DataRequestException {
        try {
            return getUUIDMojang(playerName);
        } catch (DataRequestException e) {
            logger.debug(e);
            return getUUIDMineTools(playerName);
        }
    }

    protected Optional<MojangSkinDataResult> getDataAshcon(String uuidOrName) throws DataRequestException {
        HttpResponse httpResponse = readURL(ASHCON.replace("%uuidOrName%", uuidOrName), MetricsCounter.Service.ASHCON);
        AshconResponse response = httpResponse.getBodyAs(AshconResponse.class);

        if (response.getCode() == 404) {
            return Optional.empty();
        }

        if (response.getError() != null) {
            throw new DataRequestExceptionShared("Ashcon error: " + response.getError());
        }

        if (response.getCode() != 0) {
            throw new DataRequestExceptionShared("Ashcon error code: " + response.getCode());
        }

        if (response.getUuid() == null) {
            return Optional.empty();
        }

        AshconResponse.Textures textures = response.getTextures();

        if (textures == null) {
            return Optional.empty();
        }

        AshconResponse.Textures.Raw rawTextures = textures.getRaw();
        if (rawTextures == null) {
            return Optional.empty();
        }

        if (rawTextures.getValue().isEmpty() || rawTextures.getSignature().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(MojangSkinDataResult.of(UUID.fromString(response.getUuid()),
                SkinProperty.of(rawTextures.getValue(), rawTextures.getSignature())));
    }

    public Optional<UUID> getUUIDMojang(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(UUID_MOJANG.replace("%playerName%", playerName), MetricsCounter.Service.MOJANG);

        if (httpResponse.getStatusCode() == 204 || httpResponse.getStatusCode() == 404 || httpResponse.getBody().isEmpty()) {
            return Optional.empty();
        }

        MojangUUIDResponse response = httpResponse.getBodyAs(MojangUUIDResponse.class);
        if (response.getError() != null) {
            throw new DataRequestExceptionShared("Mojang error: " + response.getError());
        }

        return Optional.ofNullable(response.getId())
                .map(MojangAPIImpl::convertToDashed);
    }

    protected Optional<UUID> getUUIDMineTools(String playerName) throws DataRequestException {
        HttpResponse httpResponse = readURL(UUID_MINETOOLS.replace("%playerName%", playerName), MetricsCounter.Service.MINE_TOOLS, 10_000);
        MineToolsUUIDResponse response = httpResponse.getBodyAs(MineToolsUUIDResponse.class);

        if (response.getStatus() != null && response.getStatus().equals("ERR")) {
            throw new DataRequestExceptionShared("MineTools error: " + response.getStatus());
        }

        return Optional.ofNullable(response.getId())
                .map(MojangAPIImpl::convertToDashed);
    }

    public Optional<SkinProperty> getProfile(UUID uuid) throws DataRequestException {
        try {
            return getDataAshcon(uuid.toString().replace("-", "")).map(MojangSkinDataResult::getSkinProperty);
        } catch (DataRequestException e) {
            logger.debug(e);
            return getProfileStartMojang(uuid);
        }
    }

    private Optional<SkinProperty> getProfileStartMojang(UUID uuid) throws DataRequestException {
        try {
            return getProfileMojang(uuid);
        } catch (DataRequestException e) {
            logger.debug(e);
            return getProfileMineTools(uuid);
        }
    }

    public Optional<SkinProperty> getProfileMojang(UUID uuid) throws DataRequestException {
        HttpResponse httpResponse = readURL(PROFILE_MOJANG.replace("%uuid%", convertToNoDashes(uuid)), MetricsCounter.Service.MOJANG);
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
        HttpResponse httpResponse = readURL(PROFILE_MINETOOLS.replace("%uuid%", convertToNoDashes(uuid)), MetricsCounter.Service.MINE_TOOLS, 10_000);
        MineToolsProfileResponse response = httpResponse.getBodyAs(MineToolsProfileResponse.class);
        if (response.getRaw() == null) {
            return Optional.empty();
        }

        MineToolsProfileResponse.Raw raw = response.getRaw();
        if (raw.getStatus() != null && raw.getStatus().equals("ERR")) {
            throw new DataRequestExceptionShared("MineTools error: " + raw.getStatus());
        }

        PropertyResponse property = raw.getProperties()[0];
        if (property.getValue().isEmpty() || property.getSignature().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(SkinProperty.of(property.getValue(), property.getSignature()));
    }

    private HttpResponse readURL(String url, MetricsCounter.Service service) throws DataRequestException {
        return readURL(url, service, 5_000);
    }

    private HttpResponse readURL(String url, MetricsCounter.Service service, int timeout) throws DataRequestException {
        metricsCounter.increment(service);

        try {
            return httpClient.execute(
                    url,
                    null,
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.GET,
                    Collections.emptyMap(),
                    timeout
            );
        } catch (IOException e) {
            logger.debug("Error while reading URL: " + url, e);
            throw new DataRequestExceptionShared(e);
        }
    }
}
