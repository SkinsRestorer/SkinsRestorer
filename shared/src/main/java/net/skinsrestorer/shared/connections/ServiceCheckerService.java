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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.util.*;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServiceCheckerService {
    private static final Map<String, UUID> PLAYER_MAP = Map.ofEntries(
            Map.entry("xknat", UUID.fromString("7dcfc130-344a-4719-9fbe-3176bc2075c6")),
            Map.entry("jeb_", UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6")),
            Map.entry("Dinnerbone", UUID.fromString("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6")),
            Map.entry("Grumm", UUID.fromString("e6b5c088-0680-44df-9e1b-9bf11792291b"))
    );
    private static final String MESSAGE_ERROR = "%s <red>✘ Error getting %s";
    private static final String MESSAGE_ERROR_EXCEPTION = "%s <red>✘ Error getting %s: %s";
    private static final String UUID_MESSAGE = "%s <green>✔ %s UUID: <aqua>%s";
    private static final String PROFILE_MESSAGE = "%s <green>✔ %s Profile: <aqua>%s";
    private final MojangAPIImpl mojangAPI;
    private final SRLogger logger;

    public ServiceCheckResponse checkServices() {
        ServiceCheckResponse response = new ServiceCheckResponse();

        Map.Entry<String, UUID> selectedUser = SRHelpers.getRandomEntry(PLAYER_MAP.entrySet());
        String selectedUsername = selectedUser.getKey();
        UUID selectedUUID = selectedUser.getValue();

        // ##### UUID requests #####
        try {
            Optional<UUID> uuid = mojangAPI.getUUIDEclipse(selectedUsername);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "Eclipse", selectedUsername, uuid.get()), true, ServiceCheckResponse.ServiceCheckType.UUID);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "Eclipse", "UUID"), false, ServiceCheckResponse.ServiceCheckType.UUID);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting Eclipse UUID");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Eclipse", "UUID", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.UUID);
        }

        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMojang(selectedUsername);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "Mojang", selectedUsername, uuid.get()), true, ServiceCheckResponse.ServiceCheckType.UUID);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "Mojang", "UUID"), false, ServiceCheckResponse.ServiceCheckType.UUID);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting Mojang UUID");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "UUID", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.UUID);
        }

        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMineTools(selectedUsername);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "MineTools", selectedUsername, uuid.get()), true, ServiceCheckResponse.ServiceCheckType.UUID);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "MineTools", "UUID"), false, ServiceCheckResponse.ServiceCheckType.UUID);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting MineTools UUID");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "MineTools", "UUID", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.UUID);
        }

        // ##### Profile requests #####
        try {
            Optional<SkinProperty> eclipse = mojangAPI.getProfileEclipse(selectedUUID);
            if (eclipse.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Eclipse", selectedUUID, eclipse.get()), true, ServiceCheckResponse.ServiceCheckType.PROFILE);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "Eclipse", "Profile"), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting Eclipse Profile");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Eclipse", "Profile", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
        }

        try {
            Optional<SkinProperty> mojang = mojangAPI.getProfileMojang(selectedUUID);
            if (mojang.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Mojang", selectedUUID, mojang.get()), true, ServiceCheckResponse.ServiceCheckType.PROFILE);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "Mojang", "Profile"), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting Mojang Profile");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "Profile", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
        }

        try {
            Optional<SkinProperty> mineTools = mojangAPI.getProfileMineTools(selectedUUID);
            if (mineTools.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "MineTools", selectedUUID, mineTools.get()), true, ServiceCheckResponse.ServiceCheckType.PROFILE);
            } else {
                response.addResult(String.format(MESSAGE_ERROR, "MineTools", "Profile"), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
            }
        } catch (DataRequestException e) {
            logger.severe("Error getting MineTools Profile");
            logger.debug(e);
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "MineTools", "Profile", e.getMessage()), false, ServiceCheckResponse.ServiceCheckType.PROFILE);
        }
        return response;
    }

    @Getter
    public static class ServiceCheckResponse {
        private final List<ServiceCheckMessage> results = new LinkedList<>();

        public boolean allFullySuccessful() {
            return results.stream().allMatch(ServiceCheckMessage::success);
        }

        public boolean minOneServiceUnavailable() {
            for (ServiceCheckType type : ServiceCheckType.values()) {
                if (getSuccessCount(type) == 0) {
                    return true;
                }
            }

            return false;
        }

        public int getSuccessCount(ServiceCheckType type) {
            return (int) results.stream().filter(message -> message.type() == type && message.success()).count();
        }

        public int getTotalCount(ServiceCheckType type) {
            return (int) results.stream().filter(message -> message.type() == type).count();
        }

        private void addResult(String message, boolean success, ServiceCheckType type) {
            results.add(new ServiceCheckMessage(message, success, type));
        }

        public enum ServiceCheckType {
            UUID,
            PROFILE
        }

        public record ServiceCheckMessage(String message, boolean success, ServiceCheckType type) {
        }
    }
}
