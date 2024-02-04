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
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;
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

    public ServiceCheckResponse checkServices() {
        ServiceCheckResponse response = new ServiceCheckResponse();

        Map.Entry<String, UUID> selectedUser = SRHelpers.getRandomEntry(PLAYER_MAP.entrySet());
        String selectedUsername = selectedUser.getKey();
        UUID selectedUUID = selectedUser.getValue();

        // ##### Ashcon request #####
        try {
            Optional<MojangSkinDataResult> uuidAshcon = mojangAPI.getDataAshcon(selectedUsername);
            if (uuidAshcon.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Ashcon", selectedUsername, uuidAshcon.get().getUniqueId()));
                response.incrementWorkingUUID();
            } else response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Ashcon", "UUID", e.getMessage()));
        }

        // ##### UUID requests #####
        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMojang(selectedUsername);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "Mojang", selectedUsername, uuid.get()));
                response.incrementWorkingUUID();
            } else response.addResult(String.format(MESSAGE_ERROR, "Mojang", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "UUID", e.getMessage()));
        }

        try {
            Optional<UUID> uuid = mojangAPI.getUUIDMineTools(selectedUsername);

            if (uuid.isPresent()) {
                response.addResult(String.format(UUID_MESSAGE, "MineTools", selectedUsername, uuid.get()));
                response.incrementWorkingUUID();
            } else response.addResult(String.format(MESSAGE_ERROR, "MineTools", "UUID"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "MineTools", "UUID", e.getMessage()));
        }

        // ##### Profile requests #####
        try {
            Optional<MojangSkinDataResult> nameAshcon = mojangAPI.getDataAshcon(selectedUUID.toString());
            if (nameAshcon.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Ashcon", selectedUUID, nameAshcon.get().getSkinProperty()));
                response.incrementWorkingProfile();
            } else response.addResult(String.format(MESSAGE_ERROR, "Ashcon", "Profile"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Ashcon", "Profile", e.getMessage()));
        }

        try {
            Optional<SkinProperty> mojang = mojangAPI.getProfileMojang(selectedUUID);
            if (mojang.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "Mojang", selectedUUID, mojang.get()));
                response.incrementWorkingProfile();
            } else response.addResult(String.format(MESSAGE_ERROR, "Mojang", "Profile"));
        } catch (DataRequestException e) {
            response.addResult(String.format(MESSAGE_ERROR_EXCEPTION, "Mojang", "Profile", e.getMessage()));
        }

        try {
            Optional<SkinProperty> minetools = mojangAPI.getProfileMineTools(selectedUUID);
            if (minetools.isPresent()) {
                response.addResult(String.format(PROFILE_MESSAGE, "MineTools", selectedUUID, minetools.get()));
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
