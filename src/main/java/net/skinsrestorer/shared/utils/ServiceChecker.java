/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.utils;

import lombok.Setter;
import net.skinsrestorer.shared.exception.SkinRequestException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceChecker {
    private ServiceCheckResponse response;
    @Setter
    private MojangAPI mojangAPI;

    public ServiceCheckResponse getResponse() {
        return response;
    }

    public void setResponse(ServiceCheckResponse response) {
        this.response = response;
    }

    public static class ServiceCheckResponse {
        private final List<String> results = new LinkedList<>();
        private final AtomicInteger workingUUID = new AtomicInteger();
        private final AtomicInteger workingProfile = new AtomicInteger();

        public List<String> getResults() {
            return results;
        }

        public void addResult(String result) {
            this.results.add(result);
        }

        public Integer getWorkingUUID() {
            return workingUUID.get();
        }

        public void incrementWorkingUUID() {
            this.workingUUID.getAndIncrement();
        }

        public Integer getWorkingProfile() {
            return workingProfile.get();
        }

        public void incrementWorkingProfile() {
            this.workingProfile.getAndIncrement();
        }
    }

    public ServiceChecker() {
        this.response = new ServiceCheckResponse();
    }

    public void checkServices() {
        // ##### UUID requests #####
        try {
            String uuid = this.mojangAPI.getUUID("Notch", false);

            if (uuid != null && !uuid.equalsIgnoreCase("null")) {
                response.addResult("MineTools UUID §a✔ Notch UUID: §b" + uuid);
                response.incrementWorkingUUID();
            } else {
                response.addResult("MineTools UUID §c✘ Error getting UUID: null");
            }
        } catch (SkinRequestException e) {
            response.addResult("MineTools UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        try {
            String uuid = this.mojangAPI.getUUIDMojang("Notch", false);

            if (uuid != null && !uuid.equalsIgnoreCase("null")) {
                response.addResult("Mojang-API UUID §a✔ Notch UUID: §b" + uuid);
                response.incrementWorkingUUID();
            } else {
                response.addResult("Mojang-API UUID §c✘ Error getting UUID: null");
            }
        } catch (SkinRequestException e) {
            response.addResult("Mojang-API UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        try {
            String uuid = this.mojangAPI.getUUIDBackup("Notch");
            response.addResult("Mojang-API (Backup) UUID §a✔ Notch UUID: §b" + uuid);
            response.incrementWorkingUUID();
        } catch (Exception e) {
            response.addResult("Mojang-API (Backup) UUID §c✘ Error getting UUID: " + e.getMessage());
        }

        // ##### Profile requests #####
        Object profile = this.mojangAPI.getSkinProperty("069a79f444e94726a5befca90e38aaf5", false);
        if (profile != null) {
            response.addResult("MineTools Profile §a✔ Notch Profile: §b" + profile.toString());
            response.incrementWorkingProfile();
        } else
            response.addResult("MineTools Profile §c✘ Error getting Profile: null");

        profile = this.mojangAPI.getSkinPropertyMojang("069a79f444e94726a5befca90e38aaf5", false);
        if (profile != null) {
            response.addResult("Mojang-API Profile §a✔ Notch Profile: §b" + profile.toString());
            response.incrementWorkingProfile();
        } else
            response.addResult("Mojang-API Profile §c✘ Error getting Profile: null");

        profile = this.mojangAPI.getSkinPropertyBackup("069a79f444e94726a5befca90e38aaf5");
        if (profile != null) {
            response.addResult("Mojang-API (Backup) Profile §a✔ Notch Profile: §b" + profile.toString());
            response.incrementWorkingProfile();
        } else
            response.addResult("Mojang-API (Backup) Profile §c✘ Error getting Profile: null");
    }
}
