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
package net.skinsrestorer.shared.connections;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.connections.responses.AshconResponse;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServiceCheckerService {
    private static final String XKNAT_NAME = "xknat";
    private static final UUID XKNAT_UUID = UUID.fromString("7dcfc130-344a-4719-9fbe-3176bc2075c6");
    private static final String MESSAGE_ERROR = "%s §c✘ Error getting %s";
    private static final String MESSAGE_ERROR_EXCEPTION = "%s §c✘ Error getting %s: %s";
    private static final String UUID_MESSAGE = "%s §a✔ xknat UUID: §b%s";
    private static final String PROFILE_MESSAGE = "%s §a✔ xknat Profile: §b%s";
    private final MojangAPIImpl mojangAPI;

    public ServiceCheckResponse checkServices() {
        ServiceCheckResponse response = new ServiceCheckResponse();

        // ##### Ashcon request #####
        try {
            Optional<AshconResponse> uuidAshcon = mojangAPI.getDataAshcon(XKNAT_NAME);
            if (uuidAshcon.isPresent()) {
                Optional<UUID> uuid = mojangAPI.getUUIDAshcon(uuidAshcon.get());

                if (uuid.isPresent()) {
                    response.addResult(String.format(UUID_MESSAGE, "Ashcon", uuid.get()));
                    response.incrementWorkingUUID();
                } else {
                    response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "UUID"));
                }
            } else response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Ashcon", "UUID", e.getMessage()));
        }

        // ##### UUID requests #####
        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMojang(XKNAT_NAME);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "Mojang", uuid.get()));
                response.incrementWorkingUUID();
            } else response.addResult(String.format(MESSAGE_ERROR, "Mojang", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "UUID", e.getMessage()));
        }

        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMineTools(XKNAT_NAME);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "MineTools", uuid.get()));
                response.incrementWorkingUUID();
            } else response.addResult(String.format(MESSAGE_ERROR, "MineTools", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "MineTools", "UUID", e.getMessage()));
        }

        // ##### Profile requests #####
        try {
            Optional<AshconResponse> nameAshcon = mojangAPI.getDataAshcon(XKNAT_UUID.toString());
            if (nameAshcon.isPresent()) {
                Optional<SkinProperty> property = mojangAPI.getPropertyAshcon(nameAshcon.get());
                if (property.isPresent()) {
                    response.addResult(String.format(PROFILE_MESSAGE, "Ashcon", property.get()));
                    response.incrementWorkingProfile();
                } else {
                    response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "Profile"));
                }
            } else response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "Profile"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Ashcon", "Profile", e.getMessage()));
        }

        try {
            Optional<SkinProperty> mojang = mojangAPI.getProfileMojang(XKNAT_UUID);
            if (mojang.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Mojang", mojang.get()));
                response.incrementWorkingProfile();
            } else response.addResult(String.format(MESSAGE_ERROR, "Mojang", "Profile"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "Profile", e.getMessage()));
        }

        try {
            Optional<SkinProperty> minetools = mojangAPI.getProfileMineTools(XKNAT_UUID);
            if (minetools.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "MineTools", minetools.get()));
                response.incrementWorkingProfile();
            } else response.addResult(String.format(MESSAGE_ERROR, "MineTools", "Profile"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "MineTools", "Profile", e.getMessage()));
        }
        return response;
    }

    @Getter
    public static class ServiceCheckResponse {
        private final List<String> results = new LinkedList<>();
        private int workingUUID = 0;
        private int workingProfile = 0;

        private void addResult(String result) {
            results.add(result);
        }

        private void incrementWorkingUUID() {
            workingUUID++;
        }

        private void incrementWorkingProfile() {
            workingProfile++;
        }
    }
}
