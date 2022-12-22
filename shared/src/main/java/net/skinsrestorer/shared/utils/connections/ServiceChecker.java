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
package net.skinsrestorer.shared.utils.connections;

import lombok.Getter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.utils.connections.responses.AshconResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ServiceChecker {
    private static final String XKNAT_NAME = "xknat";
    private static final String XKNAT_UUID = "7dcfc130344a47199fbe3176bc2075c6";

    private ServiceChecker() {
    }

    public static ServiceCheckResponse checkServices(MojangAPI mojangAPI) {
        ServiceCheckResponse response = new ServiceCheckResponse();

        // ##### Ashcon request #####
        Optional<AshconResponse> uuidAshcon = mojangAPI.getDataAshcon(XKNAT_NAME);
        if (uuidAshcon.isPresent()) {
            try {
                Optional<String> uuid = mojangAPI.getUUIDAshcon(uuidAshcon.get());

                if (uuid.isPresent()) {
                    response.addResult("Ashcon UUID §a✔ xknat UUID: §b" + uuid.get());
                    response.incrementWorkingUUID();
                } else {
                    response.addResult("Ashcon UUID §c✘ Error getting UUID");
                }
            } catch (SkinRequestException e) {
                response.addResult("Ashcon UUID §c✘ Error getting UUID: " + e.getMessage());
            }
        } else response.addResult("Ashcon §c✘ Error getting data by name");

        // ##### UUID requests #####
        try {
            Optional<String> uuid = mojangAPI.getUUIDMojang(XKNAT_NAME);

            if (uuid.isPresent() && !uuid.get().equalsIgnoreCase("null")) {
                response.addResult("Mojang API UUID §a✔ xknat UUID: §b" + uuid.get());
                response.incrementWorkingUUID();
            } else response.addResult("Mojang API UUID §c✘ Error getting UUID");
        } catch (SkinRequestException e) {
            response.addResult("Mojang API UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        try {
            Optional<String> uuid = mojangAPI.getUUIDMineTools(XKNAT_NAME);

            if (uuid.isPresent() && !uuid.get().equalsIgnoreCase("null")) {
                response.addResult("MineTools API UUID §a✔ xknat UUID: §b" + uuid.get());
                response.incrementWorkingUUID();
            } else response.addResult("MineTools API UUID §c✘ Error getting UUID");
        } catch (SkinRequestException e) {
            response.addResult("MineTools API UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        // ##### Profile requests #####
        Optional<AshconResponse> nameAshcon = mojangAPI.getDataAshcon(XKNAT_UUID);
        if (nameAshcon.isPresent()) {
            Optional<IProperty> property = mojangAPI.getPropertyAshcon(nameAshcon.get());
            if (property.isPresent()) {
                response.addResult("Ashcon Profile §a✔ xknat Profile: §b" + property.get());
                response.incrementWorkingProfile();
            } else {
                response.addResult("Ashcon Profile §c✘ Error getting Profile");
            }
        } else response.addResult("Ashcon §c✘ Error getting data by uuid");

        Optional<IProperty> mojang = mojangAPI.getProfileMojang(XKNAT_UUID);
        if (mojang.isPresent()) {
            response.addResult("Mojang-API Profile §a✔ xknat Profile: §b" + mojang.get());
            response.incrementWorkingProfile();
        } else response.addResult("Mojang-API Profile §c✘ Error getting Profile");

        Optional<IProperty> minetools = mojangAPI.getProfileMineTools(XKNAT_UUID);
        if (minetools.isPresent()) {
            response.addResult("MineTools Profile §a✔ xknat Profile: §b" + minetools.get());
            response.incrementWorkingProfile();
        } else response.addResult("MineTools Profile §c✘ Error getting Profile");

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
