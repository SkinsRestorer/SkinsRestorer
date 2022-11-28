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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceChecker {
    private static final String XKNAT_NAME = "xknat";
    private static final String XKNAT_UUID = "7dcfc130344a47199fbe3176bc2075c6";

    private ServiceChecker() {
    }

    public static ServiceCheckResponse checkServices(MojangAPI mojangAPI) {
        ServiceCheckResponse response = new ServiceCheckResponse();

        // ##### UUID requests #####
        try {
            Optional<String> uuid = mojangAPI.getUUIDAshcon(XKNAT_NAME);

            if (uuid.isPresent() && !uuid.get().equalsIgnoreCase("null")) {
                response.addResult("Ashcon UUID §a✔ xknat UUID: §b" + uuid);
                response.incrementWorkingUUID();
            } else response.addResult("Ashcon UUID §c✘ Error getting UUID: null");
        } catch (SkinRequestException e) {
            response.addResult("Ashcon UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        try {
            Optional<String> uuid = mojangAPI.getUUIDMojang(XKNAT_NAME);

            if (uuid.isPresent() && !uuid.get().equalsIgnoreCase("null")) {
                response.addResult("Mojang API UUID §a✔ xknat UUID: §b" + uuid);
                response.incrementWorkingUUID();
            } else response.addResult("Mojang API UUID §c✘ Error getting UUID: null");
        } catch (SkinRequestException e) {
            response.addResult("Mojang API UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        try {
            Optional<String> uuid = mojangAPI.getUUIDMinetools(XKNAT_NAME);

            if (uuid.isPresent() && !uuid.get().equalsIgnoreCase("null")) {
                response.addResult("Minetools API UUID §a✔ xknat UUID: §b" + uuid);
                response.incrementWorkingUUID();
            } else response.addResult("Minetools API UUID §c✘ Error getting UUID: null");
        } catch (SkinRequestException e) {
            response.addResult("Minetools API UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        // ##### Profile requests #####
        Optional<IProperty> ashcon = mojangAPI.getProfileAshcon(XKNAT_UUID);
        if (ashcon.isPresent()) {
            response.addResult("Ashcon Profile §a✔ xknat Profile: §b" + ashcon);
            response.incrementWorkingProfile();
        } else response.addResult("Ashcon Profile §c✘ Error getting Profile: null");

        Optional<IProperty> mojang = mojangAPI.getProfileMojang(XKNAT_UUID);
        if (mojang.isPresent()) {
            response.addResult("Mojang-API Profile §a✔ xknat Profile: §b" + mojang);
            response.incrementWorkingProfile();
        } else response.addResult("Mojang-API Profile §c✘ Error getting Profile: null");

        Optional<IProperty> minetools = mojangAPI.getProfileMinetools(XKNAT_UUID);
        if (minetools.isPresent()) {
            response.addResult("Minetools Profile §a✔ xknat Profile: §b" + minetools);
            response.incrementWorkingProfile();
        } else response.addResult("Minetools Profile §c✘ Error getting Profile: null");

        return response;
    }

    @Getter
    public static class ServiceCheckResponse {
        private final List<String> results = new LinkedList<>();
        private final AtomicInteger workingUUID = new AtomicInteger();
        private final AtomicInteger workingProfile = new AtomicInteger();

        public void addResult(String result) {
            results.add(result);
        }

        public void incrementWorkingUUID() {
            workingUUID.getAndIncrement();
        }

        public void incrementWorkingProfile() {
            workingProfile.getAndIncrement();
        }
    }
}
