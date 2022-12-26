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

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IMojangAPI;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.exception.NotPremiumExceptionShared;
import net.skinsrestorer.shared.exception.SkinRequestExceptionShared;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.connections.responses.AshconResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.MinetoolsProfileResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.MojangProfileResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.PropertyResponse;
import net.skinsrestorer.shared.utils.connections.responses.uuid.MinetoolsUUIDResponse;
import net.skinsrestorer.shared.utils.connections.responses.uuid.MojangUUIDResponse;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MojangAPI implements IMojangAPI {
    private static final String UUID_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%playerName%";
    private static final String UUID_MINETOOLS = "https://api.minetools.eu/uuid/%playerName%";

    private static final String ASHCON = "https://api.ashcon.app/mojang/v2/user/%uuidOrName%";
    private static final String PROFILE_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String PROFILE_MINETOOLS = "https://api.minetools.eu/profile/%uuid%";

    private final MetricsCounter metricsCounter;

    /**
     * Get the skin property from a single request
     *
     * @param nameOrUuid name or trimmed (without dashes) uuid
     * @return IProperty skin
     * @throws NotPremiumException  if the player is not premium
     * @throws SkinRequestException or error
     */
    public Optional<IProperty> getSkin(String nameOrUuid) throws SkinRequestException {
        final String upperCaseNameOrUuid = nameOrUuid.trim().toUpperCase();
        if (Arrays.stream(HardcodedSkins.values()).anyMatch(t -> t.name().equals(upperCaseNameOrUuid))) {
            HardcodedSkins hardcodedSkin = HardcodedSkins.valueOf(upperCaseNameOrUuid);
            return Optional.of(SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME, hardcodedSkin.value, hardcodedSkin.signature));
        }

        boolean isUuid = nameOrUuid.matches("[a-f\\d]{32}");
        if (!isUuid && !C.validMojangUsername(nameOrUuid)) {
            throw new NotPremiumExceptionShared();
        }

        final Optional<IProperty> skin = getProfileAshcon(nameOrUuid);
        if (skin.isPresent()) {
            return skin;
        } else {
            if (!isUuid) {
                nameOrUuid = getUUIDStartMojang(nameOrUuid);
            }

            return getProfileStartMojang(nameOrUuid);
        }
    }

    /**
     * Get the uuid from a player playerName
     *
     * @param playerName Mojang username of the player
     * @return String uuid trimmed (without dashes)
     * @throws NotPremiumException  if the player is not premium
     * @throws SkinRequestException or error
     */
    public String getUUID(String playerName) throws SkinRequestException {
        if (!C.validMojangUsername(playerName)) {
            throw new NotPremiumExceptionShared();
        }

        Optional<String> ashcon = getUUIDAshcon(playerName);

        if (ashcon.isPresent()) {
            return ashcon.get();
        } else {
            return getUUIDStartMojang(playerName);
        }
    }

    private String getUUIDStartMojang(String playerName) throws SkinRequestException {
        Optional<String> mojang = getUUIDMojang(playerName);

        if (mojang.isPresent()) {
            return mojang.get();
        } else {
            Optional<String> minetools = getUUIDMinetools(playerName);

            return minetools.orElse(null);
        }
    }

    /**
     * @param playerName Mojang username of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    protected Optional<String> getUUIDAshcon(String playerName) throws SkinRequestException {
        try {
            final String output = readURL(ASHCON.replace("%uuidOrName%", playerName), MetricsCounter.Service.ASHCON).getRight();
            final AshconResponse obj = new Gson().fromJson(output, AshconResponse.class);

            if (obj.getCode() != 0) {
                if (obj.getCode() == 404) {
                    throw new NotPremiumExceptionShared();
                }
                //throw new SkinRequestException(Locale.ALT_API_FAILED); <- WIP (might not be good when there is a 202 mojang down error)
            }

            if (obj.getUuid() != null)
                return Optional.of(obj.getUuid().replace("-", ""));
        } catch (IOException ignored) {
        }

        return Optional.empty();
    }

    public Optional<String> getUUIDMojang(String playerName) throws SkinRequestException {
        try {
            final String output = readURL(UUID_MOJANG.replace("%playerName%", playerName), MetricsCounter.Service.MOJANG).getRight();

            //todo get http code instead of checking for isEmpty
            if (output.isEmpty())
                throw new NotPremiumExceptionShared();

            final MojangUUIDResponse obj = new Gson().fromJson(output, MojangUUIDResponse.class);
            if (obj.getError() != null) {
                return Optional.empty();
            }

            return Optional.of(obj.getId());
        } catch (IOException ignored) {
        }

        return Optional.empty();
    }

    protected Optional<String> getUUIDMinetools(String playerName) throws SkinRequestException {
        try {
            final String output = readURL(UUID_MINETOOLS.replace("%playerName%", playerName), MetricsCounter.Service.MINE_TOOLS, 10000).getRight();
            final MinetoolsUUIDResponse obj = new Gson().fromJson(output, MinetoolsUUIDResponse.class);

            if (obj.getId() != null)
                return Optional.of(obj.getId());
        } catch (IOException ignored) {
        }

        return Optional.empty();
    }

    public Optional<IProperty> getProfile(String uuid) {
        Optional<IProperty> ashcon = getProfileAshcon(uuid);

        if (ashcon.isPresent()) {
            return ashcon;
        } else {
            return getProfileStartMojang(uuid);
        }
    }

    private Optional<IProperty> getProfileStartMojang(String uuid) {
        Optional<IProperty> mojang = getProfileMojang(uuid);

        if (mojang.isPresent()) {
            return mojang;
        } else {
            return getProfileMinetools(uuid);
        }
    }

    protected Optional<IProperty> getProfileAshcon(String uuid) {
        try {
            final String output = readURL(ASHCON.replace("%uuidOrName%", uuid), MetricsCounter.Service.ASHCON).getRight();
            final AshconResponse obj = new Gson().fromJson(output, AshconResponse.class);

            if (obj.getTextures() != null) {
                final AshconResponse.Textures textures = obj.getTextures();
                final AshconResponse.Textures.Raw rawTextures = textures.getRaw();

                if (rawTextures.getValue().isEmpty() || rawTextures.getSignature().isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(SkinsRestorerAPI.getApi().createPlatformProperty(rawTextures));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    public Optional<IProperty> getProfileMojang(String uuid) {
        try {
            final String output = readURL(PROFILE_MOJANG.replace("%uuid%", uuid), MetricsCounter.Service.MOJANG).getRight();
            final MojangProfileResponse obj = new Gson().fromJson(output, MojangProfileResponse.class);
            if (obj.getProperties() != null) {
                final PropertyResponse property = obj.getProperties()[0];

                if (property.getValue().isEmpty() || property.getSignature().isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    protected Optional<IProperty> getProfileMinetools(String uuid) {
        try {
            final String output = readURL(PROFILE_MINETOOLS.replace("%uuid%", uuid), MetricsCounter.Service.MINE_TOOLS, 10000).getRight();
            final MinetoolsProfileResponse obj = new Gson().fromJson(output, MinetoolsProfileResponse.class);
            if (obj.getRaw() != null) {
                final MinetoolsProfileResponse.Raw raw = obj.getRaw();
                // Break on ERR
                if (raw.getStatus() != null && raw.getStatus().equalsIgnoreCase("ERR")) {
                    throw new SkinRequestExceptionShared();
                }

                PropertyResponse property = raw.getProperties()[0];
                if (property.getValue().isEmpty() || property.getSignature().isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(SkinsRestorerAPI.getApi().createPlatformProperty(property));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    private Pair<Integer, String> readURL(String url, MetricsCounter.Service service) throws IOException {
        return readURL(url, service, 5000);
    }

    private Pair<Integer, String> readURL(String url, MetricsCounter.Service service, int timeout) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
        metricsCounter.increment(service);

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SkinsRestorer");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setDoOutput(true);

        try (InputStream is = con.getInputStream()) {
            return Pair.of(con.getResponseCode(), new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining()));
        }
    }

    @RequiredArgsConstructor
    public enum HardcodedSkins {
        STEVE("ewogICJ0aW1lc3RhbXAiIDogMTU4Nzc0NTY0NTA2NCwKICAicHJvZmlsZUlkIiA6ICJlNzkzYjJjYTdhMmY0MTI2YTA5ODA5MmQ3Yzk5NDE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVfSG9zdGVyX01hbiIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZDNiMDZjMzg1MDRmZmMwMjI5Yjk0OTIxNDdjNjlmY2Y1OWZkMmVkNzg4NWY3ODUwMjE1MmY3N2I0ZDUwZGUxIgogICAgfQogIH0KfQ", "m4AHOr3btZjX3Rlxkwb5GMf69ZUo60XgFtwpADk92DgX1zz+ZOns+KejAKNpfVZOxRAVpSWwU8+ZNgiEvOdgyTFEW4yVXthQSdBYsKGtpifxOTb8YEXznmq+yVfA1iWZx2P72TbTmbZgG/TyOViMvyqUQsVmaZDCSW/M+ImDTmzrB3KrRW25XY9vaWshNvsaVH8SfrIOm3twtiLc7jRf+sipyxWcbFsw/Kh+6GyCKgID4tgTsydu5nhthm9A5Sa1ZI8LeySSFLzU5VirZeT3LvybHkikART/28sDaTs66N2cjFDNcdtjpWb4y0G9aLdwcWdx8zoYlVXcSWGW5aAFIDLKngtadHxRWnhryydz6YrlrBMflj4s6Qf9meIPI18J6eGWnBC8fhSwsfsJCEq6SKtkeQIHZ9g0sFfqt2YLG3CM6ZOHz2pWedCFUlokqr824XRB/h9FCJIRPIR6kpOK8barZTWwbL9/1lcjwspQ+7+rVHrZD+sgFavQvKyucQqE+IXL7Md5qyC5CYb2WMkXAhjzHp5EUyRq5FiaO6iok93gi6reh5N3ojuvWb1o1cOAwSf4IEaAbc7ej5aCDW5hteZDuVgLvBjPlbSfW9OmA8lbvxxgXR2fUwyfycUVFZUZbtgWzRIjKMOyfgRq5YFY9hhAb3BEAMHeEPqXoSPF5/A="),
        ALEX("ewogICJ0aW1lc3RhbXAiIDogMTY3MTk3MTA4NzkyNywKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "cu1tMeIHDqiYaEvJSZOU+P7UV2bPSAa8kG93gACVUmUyED5shHJ6Oj1+w0Seq4M+ZDsVfLpcO3AwncvQtaauz32rTgi4lKMYnBNsQClVzgvRSuyCaGPo4OMp9dbLHC4Ll6zBaOVs+wZZ6p7XPZg+Uvl7mBMrIn/PHLzw/OVPihC9CiSiqhh4gKxu4GuPC3KUaeiVI/NKuotoWGrhdvvwAy7E37zRvWlthox5YbKd+fmPOmQ9cwmlrY/fgtWxw3I1jV4TmvUfQyFhB+AIPNCj2WW+u2W5c2pHAK23yUBu/izW1G8LBevcpmloEHtr0Q/wNOzY0muh3nYBNjEWltM3U9feNlVPSVzebSx16GqkTC+ut96HkS3i7PpZXEoPNYeMcq48gjaG62Kuf37+wwChiZhAt98+CfqwvBOGxB3QqRlY01zNjew9AjetSCE4QGxwCtVa6BgJWAuLfqrIlWOS9hCzDVL0dEJv36ADN2SmpKGdsCyd9d5iUJEsiOSowz5OXST7LPVWe6FPpkhId/AztfrfDxxCnj0AV0epmj89JcRndtEZGjqiY1Bw78f+SNlfGZFYnIu1SmNBHVju23MwF4I1MnmLLJUWJxL0MnMZG2Z7PKzoFy1+eKYi9wCwOWdNw6IXI6QwL0IpgEGeMt1kYVHt3C/8iROE2RC18Oqv6Zc="),
        ARI("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTEwNjAxMywKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmFjNmNhMjYyZDY3YmNmYjNkYmM5MjRiYTgyMTVhMTgxOTU0OTdjNzgwMDU4YTU3NDlkZTY3NDIxNzcyMTg5MiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "CzSw/H0Ij58YO1HgGiOCFvc21F/Q98k9etfPs2yvTiSP5CaCSRrdaG5+jMyXaGqifz/5EzWfQeQ9vYPP1HLf1fw2hGy9pFxKsO2VJDO+BUmD8gAVT3imoq+mFvC9Ju6QTYycX7hoDm2e9l6HBHqevyxA72BHBouwYsnsX86UO8ptmwhLa7mzvF6DyjYCs/rU3nGA9AtXA/VSYxCqGahfm5jKsQgVSoHkGSBD1hMym4Ge1CuwbLZ6+9JUwMxusISY2EHrovlhtr9p/9FAMi2KKv5R0daTla+oVzjhmYmxmU+YVACYNSOIO/lh4Li103IdzBDivne7KrweEjb456jrr3NzFiiY3T4b0Zh3LQDpDJxXcSsX0ShKPfKkAZ1/7reX6Ec5CnsHnysDIxda9zlAsP74muGaqx6SjT1OTo42x5Imyo4An7RN8OlumvVoNZKujJ1XgW9D+IqHIxKm0vXLhNGNtJqMQRH7KG4NHDyTTbmISwLAorhyJNtVCxHx4QVyYA5UjsELqGNPdootHzflZvhdgBbJ1L+KidwS09QbwiQ/hy6MJmP4TblaevDrnZjHcJ7ynN4gzZH4J9ozpmgfaD/KSwHZoGGx2ySPfctXaYMP+IhrUosvVb6lE9efXFWViLrOy/Mi3amoymx1xEL84Ms8GaAgFwkuNnjXnQGWVAU="),
        EFE("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTE4OTA5MSwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmVjZTcwMTdiMWJiMTM5MjZkMTE1ODg2NGIyODNiOGI5MzAyNzFmODBhOTA0ODJmMTc0Y2NhNmExN2U4ODIzNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9", "vKzcBFCgRcRHKERehCI67IO6ZYzXRLxqmZ1E8mtbiKUrx+PsHZt08paFujzfIDHRWSBke4bXeLtSY0fpBvEtFOmwgVFVvnGXWSt00EKYBOXmNLT9OE3x2s4ZmMSQPzAQibt915wk3DiAS/xTBDCQnYuob9+sy+HMxMvDcfONltMlRHPyhy62g4re6ST14+nk4cxHYUpgq3i9QQxaJMqo50GgH9+SLFKcZDbpUvtv6N20/dWBR67FS6m5ywjB0oeWiokBNKdTUk+R6CJWLP4P3Fw6BFo3nH7pwY0OnGKsijHtYcWvJjXLmrf/E3dlvlXi7F4aB0t7DWgIxLGe3rjn6ZixDv/Rr/WD08KIGhniw6lwl3lUTkVcqshqSXUNK5Sihzz2c6oNSIJRyGzfkvZnJjHjWH1xj9+T/BqeTHeKtMumqi0OjomURD3+lUJdAECwe9dMD4izJz/1xF1qo15K+VvzDB2ZTJJ3ciykke5Lg8sa8VCa+cA8A8w2HWTu+qj/rs1mJ7ifRCTLDQwXhJhbTVf3uGVWeEzXtsZE8kk/MsQA6VSsBkJwsvMG2bHQ0VC+PD7INzJIAewby7/hYm0b+wiWDP7X23om2sfRLO34aFQFgbHLnrkLU7Vev/Xlh+0WIpGdKme1KphICLvNnDcl1fcleEHaI7TuTdDeGYctkis="),
        KAI("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTMzMzcyOSwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjI2YzYxN2ZkZTViMWJhNTY5YWEwOGJkMmNiNmZkODRjOTMzMzc1MzJhODcyYjNlYjdiZjY2YmRkNWIzOTVmOCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","DG8OGU6IyMeHYYcTdw7M8OSGZMyDUcyHMJ+hBP+Ud1Ptl9LRDS6VkRStG0R5PgsL8sSqfm1ObSs/IAMJek25p1WqSpmCGb86k0xNyjb2lGNTyDCEmTrG3qPYq2BCpS8EUvM3JO+pilPLQb7f8yrTtpH8aBTEc/VRUYOLOZk5Wf5SYhv4jw6Wd/GvbaKFCjgcf15Oj/J2TKKHIv/ELpxsCeVj4LP3onP/61E4/gveDGwQwt4oOKlBewamGGSCV34k7IBlwXTt5ddYHzXEHJ85bsN7aRukjH4xGW1NYxq0bxc8W8b0qy0LIy5TZNbNqIuajBQw91Lww547GdUgJoCY/DoL+9KxnzaQ+cksuC15nJzaZBoFUoW2QjRKIfOLdDWkxDQFxRWL7dGqtyfEN1iCEnxkTjxQxYlodm4u/eRC4hk8sLT/ZElZjI/ia6bJp6SxHIavsU8nzJrxe03bxOx9BWqxina1YG2Mykf4KQFbWdr66y4a5rn21OCeYM6Cg7c4fxjHwig5Zdr4ZNDh7K94XonHaQOTLOQ0vfhsU/U1OYCI649MuJqQYqnGSgq+/nItaf5RONCPiOjA9GGS16w+j/qqxLKF5FW+tllVBjQwT/4SfIIuMG/+wfcswsIcVzGqVT9MlBmATpLWR98iZaWCW/JTd5nbVyszUsVHct/VN/E="),
        MAKENA("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTQwMjU1MCwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NiM2JhNTJkZGQ1Y2M4MmMwYjA1MGMzZjkyMGY4N2RhMzZhZGQ4MDE2NTg0NmY0NzkwNzk2NjM4MDU0MzNkYiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","yPczqGsS1MWwiCqNccm9BfSdDk1ojz2EGKxzLFPYfEOy+rbkL6G+iPO053YGzvtahwA8STQuNNc6cRcR3jcb9/jSZ6zz+Xmc7xaOk8p883N8TITz1s9kOWJwYa4NjCshM71p0iOEShe+s2Outjy4AOFKiOjE1/V7dIE6CvtGCOQs4Cdcv7nTjDr3AOgyrrRULDJUj795tI5ahLuikBwvcTEPDuO+stnhJCKEqPcR4X6be1k+eJ3PCuJznkrGIA8WcHOYdqG2V9gjPUuM6T3RkU4+qfA2tc/RGLosIsKMSGOLrkq7G4IR+6f6t3lg4/PWIMIGI2ZNjTFTvDDmWUlH9XZ8Oh+KMCuzRGSX3RXdPeisy3M7xyQXAvMf52CnMVGttzRmHKcKmbP02RfW0HjdmAViEOPG+u2qCHXmtnJWA79daUnRLZ4ALcMhsylry+mcKJfsLeEZNy2qqvoneVsZAf7Mk7gWGTGgNbCjwxbepr3cR4UevrUzQrinl6SLpGsWCE6QI2jBVH9TiB7+EZe6SBXTwqanRQh6FxnH3KgfR4JxSmks4rBU0mML77OC3eq9PwL2NpG80EyfvRXrh9cyRIl65AmnBm7vtv6HlFkggBzfVrBowRr/l2BjbpWx9uTcdADm/lwRlwGLk3qg1OWsgE3Sf/2cmPKxuUJZ0eZK8Es="),
        NOOR("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTQ2NTg4MCwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmMxNjBmYmQxNmFkYmM0YmZmMjQwOWU3MDE4MGQ5MTEwMDJhZWJjZmE4MTFlYjZlYzNkMTA0MDc2MWFlYTZkZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","mp3JcUIgR1oUahcXrg12eLxcxE5t0ZtQ/7vJfGR8ooa9JXIRfzZesoESsR/0FGGi9++8N7Il2QIWkv+3UB7Gm3WfkVBbCGbtbKL89QKYwALxR3HKqdhIk6C4MDXlLxFK/YhJ13AQ0UIIDLgFQzfzbAP6xP09H/63SbWEcFNGmAqHHEoeQfwN1At0WFnJ13/1/g2kaTRgsc9/VyqpF63hkMCCTEJqyfkGCt6M7iFqnWf9brx4Nb6yK/rSHDyyAQk0wZ0LOGuCpZtS4IEBGmsuw6JaB1d6hXkoUM/TI4r4KsyC5S94QUiOXR1bd867cc7iB/gp/SvHXtrXRxTPUnpD23WqwtkIl8OLb0rt6lmJMmhQXKcr9DBf4/39TlWL/inVTeY06lSPvay3sxZajyh5bFApSHxSH3wddO8oWhsZ3eRm7RSdUtT9mtqshgvz5md8NmSHRea72FImVwVC0tjhsoqvbNLnRqNXxh0Ghc1CeZaicQqD0BhFQk2PAWSnRjz2W+SmDrqmrzfo66DxdJFN5DCMBZPs6L1FcNNoyTtvFTcZug4VOlSYa7Ko4PkeXiO72EOMMjAKfasxA1zeFeGngJJQzxduB2aGqzYiAuXwRFVtdsV5+dKSt5s2PZeBhhM6YAmldQpjm/YqtgMDr8LIbXfk+YK91f4TE58nZl/3ceA="),
        SUNNY("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTUzNDI0NCwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjY2YmM4MGYwMDJiMTAzNzFlMmZhMjNkZTZmMjMwZGQ1ZTJmM2FmZmMyZTE1Nzg2ZjY1YmM5YmU0YzZlYjcxYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","TlTydo5fzV/zgVS34eEqSaR/hnYwYSacLVfA7dm18eef4VvRfSHvimhTOO7DrrQymWqsSq4jp1BMO3Q5NIkl7iTojBKF3fRd57ucfNIwxblhc6q2oPQRHnHzLk5DX5QXuqTovP/VlzSBKx1Gboxx/EQnlDNJsZ0HEERf5HI/ZRv5TXYU5ZrJ5NVm8QTVKJlkqRGkNXFjh44QsgX9LBNchFJRRwj4J60loerSbEtV6ApfzZSeDct8HHw0q34PsFpkN1+8oYM6MGknNsf4H3FOqcOrxQIf0Uxd9GZLyxPIbwBvWZtNYpKYEoo33ZwTHF+Vrjz3+Ro7/xzny1hWhP1PA6pjOoaKeSStHy+/26JcBLl37G3w6MzZTo/usJlVX+C5BDJ5dIRnUQEGRqvPiDUOK+uo1xx9ER87Vv25jLp21TwT+rqmSCUzjt6vLrY5rd0SuXza/iTdOOIn3hMJ9vSS6H1xkZtR3ahnMYSXxnrFsOFLQi/enfFXqp83XQI+37TAkQJWn7+cbK/rvv1sLqLfMaEHEgOZgfELY2+7WTZTWTMRddRC08XTq3e4tKYANqBKh7kxXSo++ntKGiCbJXmKsLthDGQ6cy9/UEBTbkYqX/5eVlfU6WWKqfcqyyQ936h9oVFcIvytEHglmxAjUhucFP9dFwYGy8jzEostMls46NA="),
        ZURI("ewogICJ0aW1lc3RhbXAiIDogMTY3MjA1MTU5MTMyOCwKICAicHJvZmlsZUlkIiA6ICJkZTE0MGFmM2NmMjM0ZmM0OTJiZTE3M2Y2NjA3MzViYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTUlRlYW0iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVlNTIyNjExMDA1YWNmMjU2ZGJkMTUyZTk5MmM2MGMwYmI3OTc4Y2IwZjMxMjc4MDc3MDBlNDc4YWQ5NzY2NCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9","RK396kOUFcmN50tLhfsTiKoGQeVfp4SFvAgyEZaDkKM9tz+CroUOInmGTeqOroTXwoNIxMypjnhlKTWb2NH5MNDtotmY5Nvn9lII4G66I1tx2fFBSiwtGSSpDEUKBb9+jwbdDVyYwe/REJqLbgHJoMdQVd3p4UTw4voOgqEKZ7KnUcU3HEdKYvKPJ1gvPb5lVd7nMBldiEKA71vK28XV7/NuD4yHLMelTpKOcL75vY+eK2r29fD+rOhwTvkentw3QvXkox25uMS+he+9qzjDxsl9m+p2Z2dNTdjm/o7OLw26g3Uv+ydkxeDYe76QfHLS9U7XHSYWEXFWI2AnicIw1QCAHhh9T7XqI+/yOeHxUkJfi2LyhWMVuDfKGsLv883iAFjD+Xr2XcAoutU4zHpvD/TIW8xSg2f3kl5CG1pv5MNqLZsm+f9nCh+3xsj1ccZpVOlfAbmQiKfs23pe0VTW1LSGsgrZ58wGUPv4xUZjYsPkyugdo9GlGeaWKV9hEizzuazYLqQtOeSxaQDptKNETFQC34K+s4g1QyACG9vrPfM7Ac+1Lmz9fJ2hyX0YcGMhpL7wukv5iEP4rzfsIXch/ZQxkGXSoIXdgrEVyEm0WlhxtbKrcD3PLStEqfO4uCU9la7XVOxDTx14AlU4PIJhnVkUjMEfLQVdCWIwrq7e4Fw="),
        SKINSRESTORER("ewogICJ0aW1lc3RhbXAiIDogMTU5OTMzNjMxNjYzNSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MTM1NWUwYzU3YmM4NGE2OTQxYjQwN2Q4NDgwMzA3NjkzYWM5ZWJmNzg1NDEyNGVmMTc1NjJhNDVjZTdiMTEwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "x2yGMK0NqTuiqHfKcHphGFk2UXoBHNpxrHvVD5qMhB5ZKB9pftRcuov7GwayUD95S9z8bgdRyujBefMRijYVYA+BHzYqeGX0b2qeTXLiB4iYarnVs2wzxMLL1mNTBjuyuvPe97tAmRQ4N6s+Znjy5vQ491Fgf5CS4G36f86yROKHRFieNsck5vFSZ98mE4q6o8mK94YNc6+gUXo6O0+hKcLQ48otpiA9zx0Ip15AjLxSvHAfFnH/YVTNHSEIem7nChIQAKuDs8dKibZ6inc3LmC2fmNK0YWuzmGVYg5LqdMycRDgc5C3XU5rA80N/VrdDAY4/6X7bFH+Ib5i35J+Lk31prXRcmxjifF+aAI8xuqdrJMQuxVHJc9QVhclUgLWiGrsEzBqLmCSGmc1lz8dE0ycHahZITheXuLEW3b85y+wsG38xJB61TTU1m68ykdzRwO6IXFKZAkbqyXc6p4PCmPeSaD1Y+Jow9CHYMG8Lk7P/uZoUE96sVVgK/GiSvW40AbhuqJRfPZBjlCR4HBJCJLNep9/66IyVFimKTjWWsyxFWs3gLzDW1ULealS/1IzurRpR0/eH9ZyFUxvthf96FHYAyfH1YmjS1evBPZkC3m7NR8JZ+AosYMMnxsZo21YWYDiwWPdv+98d6z5kZwKVPX8H9GBsmXq2xeLfBF/O4M="),
        TECHNOBLADE("ewogICJ0aW1lc3RhbXAiIDogMTY1NjcxNDEwMDAyOSwKICAicHJvZmlsZUlkIiA6ICJiODc2ZWMzMmUzOTY0NzZiYTExNTg0MzhkODNjNjdkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWNobm9ibGFkZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84YzkwN2E3ZjRmMGMzYTZjOWM5Yjc5MzQ4ZjdiZjE5OTM2OTViNmQyZWFlMmZhYjcwNGExYTRkOWI4Mjg4Y2JlIgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMzQwYzBlMDNkZDI0YTExYjE1YThiMzNjMmE3ZTllMzJhYmIyMDUxYjI0ODFkMGJhN2RlZmQ2MzVjYTdhOTMzIgogICAgfQogIH0KfQ==", "Lr/9e3+5BjQleU+hyuFKrgmQ4oDpSoaulmsgHiDItHFnJL86fSwMGw8L0HjJGU9CknbKNs0UujK2rl1E3jgANOCGfarT2NHL6A/PMpsIvSQcUpJcF9pU8mGURmkcuH79jpxEMARFQDaILLk8lfXkseaMBJn43PTyowZQeVvDC1lphf4xHth3hf7NloIY5aGLYJRjJidV5VEIZhl5N1YRnnRU57cdoQueKv9W6u9/l5JDlK4SwpusR1hxEE/zc4YAJ3n+/Uc2AzIBjMANetnmFEP56lnqszsu0Ja9nWGITtSGY7mgjlpGc5siIneaxEHQgoy6OCwAC3TdPvRoDk7aenkKamDq5B0m7YSt3Zs24EC+MOqWn633ER0zTTX/ASBgIhCwy61dAbBznXh1yzhn4qjxTAb8oaqTEIOs/d35GcdDQk29/RBMcLhFlk4J6MV1OpDk6gn+PaVaUq2EvPxAI5mZLC2i6Ps5QfXUJkT+gQK5ZLC1ZZc8VoQTntC8t5PBlYnTsVPJoQuqUH9QrIBM1Ij85VBln7qmUaTdCzcmc5nxHIirl6wpnyR5n2nxisXERbiFlcdPH1hT1OPpZ5xV3lq3HgpuanI11tpH9IZgkVHm+sDzoU9BP9OJOD+1Itlp0WR3EYYwhCwtrDYHoQ1h3YhASi3NpFH6BxMcFIKnwe4="); // Hardcode skin as Technoblade never dies, not even if minecraft dies.

        private final String value;
        private final String signature;
    }
}
