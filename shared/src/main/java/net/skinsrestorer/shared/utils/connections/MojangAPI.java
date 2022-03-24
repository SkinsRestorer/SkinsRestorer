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
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IMojangAPI;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.reflection.exception.ReflectionException;
import net.skinsrestorer.api.serverinfo.Platform;
import net.skinsrestorer.shared.exception.NotPremiumException;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.connections.responses.AshconResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.MinetoolsProfileResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.MojangProfileResponse;
import net.skinsrestorer.shared.utils.connections.responses.profile.PropertyResponse;
import net.skinsrestorer.shared.utils.connections.responses.uuid.MinetoolsUUIDResponse;
import net.skinsrestorer.shared.utils.connections.responses.uuid.MojangUUIDResponse;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class MojangAPI implements IMojangAPI {
    private static final String UUID_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%name%";
    private static final String UUID_MINETOOLS = "https://api.minetools.eu/uuid/%name%";

    private static final String ASHCON = "https://api.ashcon.app/mojang/v2/user/%uuidOrName%";
    private static final String PROFILE_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String PROFILE_MINETOOLS = "https://api.minetools.eu/profile/%uuid%";

    private final SRLogger logger;
    private final Platform platform;
    private final MetricsCounter metricsCounter;
    private final Class<? extends IProperty> propertyClass;

    public MojangAPI(SRLogger logger, Platform platform, MetricsCounter metricsCounter) {
        this.logger = logger;
        this.platform = platform;
        this.metricsCounter = metricsCounter;
        propertyClass = PropertyRegistry.selectPropertyForPlatform(platform);
    }

    /**
     * Generates a platform specific property. (or generic property if sponge)
     *
     * @param name      Name of the property
     * @param value     Value of the property
     * @param signature Signature of the property
     * @return A platform specific property
     */
    public IProperty createProperty(String name, String value, String signature) {
        // use our own property class if we are on sponge
        if (platform == Platform.SPONGE)
            return new GenericProperty(name, value, signature);

        try {
            return (IProperty) ReflectionUtil.invokeConstructor(propertyClass, name, value, signature);
        } catch (ReflectionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the skin property from a single request
     *
     * @param nameOrUuid name or trimmed (without dashes) uuid
     * @return IProperty skin
     * @throws SkinRequestException on not premium or error
     */
    public Optional<IProperty> getSkin(String nameOrUuid) throws SkinRequestException {
        final String finalNameOrUuid = nameOrUuid.trim().toUpperCase();
        if (Arrays.stream(HardcodedSkins.values()).anyMatch(t -> t.name().equals(finalNameOrUuid))) {
            return Optional.of(createProperty("textures", HardcodedSkins.valueOf(finalNameOrUuid).value, HardcodedSkins.valueOf(finalNameOrUuid).signature));
        }

        final Optional<IProperty> skin = getProfileAshcon(nameOrUuid);
        if (skin.isPresent()) {
            return skin;
        } else {
            if (!nameOrUuid.matches("[a-f0-9]{32}"))
                nameOrUuid = getUUIDStartMojang(nameOrUuid);

            return getProfileStartMojang(nameOrUuid);
        }
    }

    public String getUUID(String name) throws SkinRequestException {
        Optional<String> ashcon = getUUIDAshcon(name);

        if (ashcon.isPresent()) {
            return ashcon.get();
        } else {
            return getUUIDStartMojang(name);
        }
    }

    private String getUUIDStartMojang(String name) throws SkinRequestException {
        Optional<String> mojang = getUUIDMojang(name);

        if (mojang.isPresent()) {
            return mojang.get();
        } else {
            Optional<String> minetools = getUUIDMinetools(name);

            return minetools.orElse(null);
        }
    }

    /**
     * @param name Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    protected Optional<String> getUUIDAshcon(String name) throws SkinRequestException {
        try {
            final String output = readURL(ASHCON.replace("%uuidOrName%", name), MetricsCounter.Service.ASHCON);
            final AshconResponse obj = new Gson().fromJson(output, AshconResponse.class);

            if (obj.getCode() != 0) {
                if (obj.getCode() == 404) {
                    throw new NotPremiumException();
                }
                //throw new SkinRequestException(Locale.ALT_API_FAILED); <- WIP (might not be good when there is a 202 mojang down error)
            }

            if (obj.getUuid() != null)
                return Optional.of(obj.getUuid().replace("-", ""));
        } catch (IOException ignored) {
        }

        return Optional.empty();
    }

    public Optional<String> getUUIDMojang(String name) throws SkinRequestException {
        try {
            final String output = readURL(UUID_MOJANG.replace("%name%", name), MetricsCounter.Service.MOJANG);

            //todo get http code instead of checking for isEmpty
            if (output.isEmpty())
                throw new NotPremiumException();

            final MojangUUIDResponse obj = new Gson().fromJson(output, MojangUUIDResponse.class);
            if (obj.getError() != null) {
                return Optional.empty();
            }

            return Optional.of(obj.getId());
        } catch (IOException ignored) {
        }

        return Optional.empty();
    }

    protected Optional<String> getUUIDMinetools(String name) throws SkinRequestException {
        try {
            final String output = readURL(UUID_MINETOOLS.replace("%name%", name), MetricsCounter.Service.MINE_TOOLS, 10000);
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

    private Optional<IProperty> getProfileStartMojang(String name) {
        Optional<IProperty> mojang = getProfileMojang(name);

        if (mojang.isPresent()) {
            return mojang;
        } else {
            return getProfileMinetools(name);
        }
    }

    protected Optional<IProperty> getProfileAshcon(String uuid) {
        try {
            final String output = readURL(ASHCON.replace("%uuidOrName%", uuid), MetricsCounter.Service.ASHCON);
            final AshconResponse obj = new Gson().fromJson(output, AshconResponse.class);

            if (obj.getTextures() != null) {
                final AshconResponse.Textures textures = obj.getTextures();
                final AshconResponse.Textures.Raw rawTextures = textures.getRaw();

                if (!(rawTextures.getValue().isEmpty() || rawTextures.getSignature().isEmpty()))
                    return Optional.of(createProperty("textures", rawTextures.getValue(), rawTextures.getSignature()));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    public Optional<IProperty> getProfileMojang(String uuid) {
        try {
            final String output = readURL(PROFILE_MOJANG.replace("%uuid%", uuid), MetricsCounter.Service.MOJANG);
            final MojangProfileResponse obj = new Gson().fromJson(output, MojangProfileResponse.class);
            if (obj.getProperties() != null) {
                final PropertyResponse property = obj.getProperties()[0];

                if (!(property.getValue().isEmpty() || property.getSignature().isEmpty()))
                    return Optional.of(createProperty("textures", property.getValue(), property.getSignature()));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    protected Optional<IProperty> getProfileMinetools(String uuid) {
        try {
            final String output = readURL(PROFILE_MINETOOLS.replace("%uuid%", uuid), MetricsCounter.Service.MINE_TOOLS, 10000);
            final MinetoolsProfileResponse obj = new Gson().fromJson(output, MinetoolsProfileResponse.class);
            if (obj.getRaw() != null) {
                final MinetoolsProfileResponse.Raw raw = obj.getRaw();
                // Break on ERR
                if (raw.getStatus() != null && raw.getStatus().equalsIgnoreCase("ERR")) {
                    throw new SkinRequestException();
                }

                PropertyResponse property = raw.getProperties()[0];
                if (!(property.getValue().isEmpty() || property.getSignature().isEmpty()))
                    return Optional.of(createProperty("textures", property.getValue(), property.getSignature()));
            }
        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

    private String readURL(String url, MetricsCounter.Service service) throws IOException {
        return readURL(url, service, 5000);
    }

    private String readURL(String url, MetricsCounter.Service service, int timeout) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        metricsCounter.increment(service);

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SkinsRestorer");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setDoOutput(true);

        try (InputStream is = con.getInputStream()) {
            return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining());
        }
    }


    @RequiredArgsConstructor
    public enum HardcodedSkins {
        STEVE("ewogICJ0aW1lc3RhbXAiIDogMTU4Nzc0NTY0NTA2NCwKICAicHJvZmlsZUlkIiA6ICJlNzkzYjJjYTdhMmY0MTI2YTA5ODA5MmQ3Yzk5NDE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVfSG9zdGVyX01hbiIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZDNiMDZjMzg1MDRmZmMwMjI5Yjk0OTIxNDdjNjlmY2Y1OWZkMmVkNzg4NWY3ODUwMjE1MmY3N2I0ZDUwZGUxIgogICAgfQogIH0KfQ", "m4AHOr3btZjX3Rlxkwb5GMf69ZUo60XgFtwpADk92DgX1zz+ZOns+KejAKNpfVZOxRAVpSWwU8+ZNgiEvOdgyTFEW4yVXthQSdBYsKGtpifxOTb8YEXznmq+yVfA1iWZx2P72TbTmbZgG/TyOViMvyqUQsVmaZDCSW/M+ImDTmzrB3KrRW25XY9vaWshNvsaVH8SfrIOm3twtiLc7jRf+sipyxWcbFsw/Kh+6GyCKgID4tgTsydu5nhthm9A5Sa1ZI8LeySSFLzU5VirZeT3LvybHkikART/28sDaTs66N2cjFDNcdtjpWb4y0G9aLdwcWdx8zoYlVXcSWGW5aAFIDLKngtadHxRWnhryydz6YrlrBMflj4s6Qf9meIPI18J6eGWnBC8fhSwsfsJCEq6SKtkeQIHZ9g0sFfqt2YLG3CM6ZOHz2pWedCFUlokqr824XRB/h9FCJIRPIR6kpOK8barZTWwbL9/1lcjwspQ+7+rVHrZD+sgFavQvKyucQqE+IXL7Md5qyC5CYb2WMkXAhjzHp5EUyRq5FiaO6iok93gi6reh5N3ojuvWb1o1cOAwSf4IEaAbc7ej5aCDW5hteZDuVgLvBjPlbSfW9OmA8lbvxxgXR2fUwyfycUVFZUZbtgWzRIjKMOyfgRq5YFY9hhAb3BEAMHeEPqXoSPF5/A="),
        ALEX("ewogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJpZCIgOiAiMWRjODQ3ZGViZTg2NDBhOGEzODExODkwZTk0ZTdmNmIiLAogICAgICAidHlwZSIgOiAiU0tJTiIsCiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAgICJwcm9maWxlSWQiIDogIjc3MjdkMzU2NjlmOTQxNTE4MDIzZDYyYzY4MTc1OTE4IiwKICAgICAgInRleHR1cmVJZCIgOiAiZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfSwKICAic2tpbiIgOiB7CiAgICAiaWQiIDogIjFkYzg0N2RlYmU4NjQwYThhMzgxMTg5MGU5NGU3ZjZiIiwKICAgICJ0eXBlIiA6ICJTS0lOIiwKICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAicHJvZmlsZUlkIiA6ICI3NzI3ZDM1NjY5Zjk0MTUxODAyM2Q2MmM2ODE3NTkxOCIsCiAgICAidGV4dHVyZUlkIiA6ICJmYjlhYjM0ODNmODEwNmVjYzllNzZiZDQ3YzcxMzEyYjBmMTZhNTg3ODRkNjA2ODY0ZjNiM2U5Y2IxZmQ3YjZjIiwKICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgIH0KICB9LAogICJjYXBlIiA6IG51bGwKfQ==", "Bl/hfaMcGIDwYEl1fqSiPxj2zTGrTMJomqEODvB97VbJ8cs7kfLZIC1bRaCzlFHo5BL0bChL8aQRs/DJGkxmfOC5PfQXubxA4/PHgnNq6cqPZvUcC4hjWdTSKAbZKzHDGiH8aQtuEHVpHeb9T+cutsS0i2zEagWeYVquhFFtctSZEh5+JWxQOba+eh7xtwmzlaXfUDYguzHSOSV4q+hGzSU6osxO/ddiy4PhmFX1MZo237Wp1jE5Fjq+HN4J/cpm/gbtGQBfCuTE7NP3B+PKCXAMicQbQRZy+jaJ+ysK8DJP/EulxyERiSLO9h8eYF5kP5BT5Czhm9FoAwqQlpTXkJSllcdAFqiEZaRNYgJqdmRea4AeyCLPz83XApTvnHyodss1lQpJiEJuyntpUy1/xYNv+EdrNvwCnUPS/3/+jA/VKjAiR9ebKTVZL8A5GHR4mKp7uaaL1DouQa2VOJmQHKo3++v6HGsz1Xk6J7n/8qVUp3oS79WqLxlZoZPBIuQ90xt8Yqhxv6e9FXD4egHsabVj5TO/bZE6pEUaVTrKv49ciE0RqjZHxR5P13hFsnMJTXnT5rzAVCkJOvjaPfZ70WiLJL3X4OOt1TrGK0CoBKQt7yLbU5Eap6P+SLusHrZx+oU4Xspimb79splBxOsbhvb+olbRrJhmxIcrhVIqHDY="),
        SKINSRESTORER("ewogICJ0aW1lc3RhbXAiIDogMTU5OTMzNjMxNjYzNSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MTM1NWUwYzU3YmM4NGE2OTQxYjQwN2Q4NDgwMzA3NjkzYWM5ZWJmNzg1NDEyNGVmMTc1NjJhNDVjZTdiMTEwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "x2yGMK0NqTuiqHfKcHphGFk2UXoBHNpxrHvVD5qMhB5ZKB9pftRcuov7GwayUD95S9z8bgdRyujBefMRijYVYA+BHzYqeGX0b2qeTXLiB4iYarnVs2wzxMLL1mNTBjuyuvPe97tAmRQ4N6s+Znjy5vQ491Fgf5CS4G36f86yROKHRFieNsck5vFSZ98mE4q6o8mK94YNc6+gUXo6O0+hKcLQ48otpiA9zx0Ip15AjLxSvHAfFnH/YVTNHSEIem7nChIQAKuDs8dKibZ6inc3LmC2fmNK0YWuzmGVYg5LqdMycRDgc5C3XU5rA80N/VrdDAY4/6X7bFH+Ib5i35J+Lk31prXRcmxjifF+aAI8xuqdrJMQuxVHJc9QVhclUgLWiGrsEzBqLmCSGmc1lz8dE0ycHahZITheXuLEW3b85y+wsG38xJB61TTU1m68ykdzRwO6IXFKZAkbqyXc6p4PCmPeSaD1Y+Jow9CHYMG8Lk7P/uZoUE96sVVgK/GiSvW40AbhuqJRfPZBjlCR4HBJCJLNep9/66IyVFimKTjWWsyxFWs3gLzDW1ULealS/1IzurRpR0/eH9ZyFUxvthf96FHYAyfH1YmjS1evBPZkC3m7NR8JZ+AosYMMnxsZo21YWYDiwWPdv+98d6z5kZwKVPX8H9GBsmXq2xeLfBF/O4M=");

        private final String value;
        private final String signature;
    }
}
