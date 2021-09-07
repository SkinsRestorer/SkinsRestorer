/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.shared.exception.ReflectionException;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class MojangAPI {
    private static final String UUID_URL_ASHCON = "https://api.ashcon.app/mojang/v2/user/%name%";
    private static final String UUID_URL_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%name%";
    private static final String UUID_URL_MINETOOLS = "https://api.minetools.eu/uuid/%name%";

    private static final String SKIN_URL_ASHCON = "https://api.ashcon.app/mojang/v2/user/%uuid%";
    private static final String SKIN_URL_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String SKIN_URL_MINETOOLS = "https://api.minetools.eu/profile/%uuid%";

    private final SRLogger logger;
    private final Platform platform;
    private final MetricsCounter metricsCounter;
    private Class<? extends IProperty> propertyClass;

    public MojangAPI(SRLogger logger, Platform platform, MetricsCounter metricsCounter) {
        this.logger = logger;
        this.platform = platform;
        this.metricsCounter = metricsCounter;

        if (platform == Platform.BUKKIT) {
            propertyClass = BukkitProperty.class;
        } else if (platform == Platform.BUNGEECORD) {
            propertyClass = BungeeProperty.class;
        } else if (platform == Platform.VELOCITY) {
            propertyClass = VelocityProperty.class;
        }
    }

    public IProperty createProperty(String name, String value, String signature) {
        // use our own property class if we are on sponge
        if (platform == Platform.SPONGE)
            return new GenericProperty(name, value, signature);

        try {
            return (IProperty) ReflectionUtil.invokeConstructor(propertyClass, name, value, signature);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get the skin from a single request
     *
     * @param nameOrUuid name or trimmed (-) uuid
     * @return IProperty skin
     * @throws SkinRequestException on not premium or error
     */
    public IProperty getSkin(String nameOrUuid) throws SkinRequestException {
        final String finalNameOrUuid = nameOrUuid.trim().toUpperCase();
        if (Arrays.stream(HardcodedSkins.values()).anyMatch(t -> t.name().equals(finalNameOrUuid))) {
            return createProperty("textures", HardcodedSkins.valueOf(finalNameOrUuid).value, HardcodedSkins.valueOf(finalNameOrUuid).signature);
        }

        final IProperty skin = getProfile(nameOrUuid, false);
        if (skin != null)
            return skin;

        if (!nameOrUuid.matches("[a-f0-9]{32}"))
            nameOrUuid = getUUIDMojang(nameOrUuid, true);

        return getProfileMojang(nameOrUuid, true);
    }

    // TODO: Deal with duplicated code

    public String getUUID(String name) throws SkinRequestException {
        return getUUID(name, true);
    }

    /**
     * @param name Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    protected String getUUID(String name, boolean tryNext) throws SkinRequestException {
        try {
            final String output = readURL(UUID_URL_ASHCON.replace("%name%", name), MetricsCounter.Service.ASHCON);
            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            if (obj.has("code")) {
                if (obj.get("code").getAsInt() == 404) {
                    throw new SkinRequestException(Locale.NOT_PREMIUM);
                }
                //throw new SkinRequestException(Locale.ALT_API_FAILED); <- WIP (might not be good when there is a 202 mojang down error)
            }

            if (obj.has("uuid"))
                return obj.get("uuid").getAsString().replace("-", "");
        } catch (IOException ignored) {
        }
        if (tryNext)
            return getUUIDMojang(name, true);

        return null;
    }

    public String getUUIDMojang(String name, boolean tryNext) throws SkinRequestException {
        if (tryNext)
            logger.debug("Trying Mojang API to get UUID for player " + name + ".");

        try {
            final String output = readURL(UUID_URL_MOJANG.replace("%name%", name), MetricsCounter.Service.MOJANG);

            //todo get http code instead of checking for isEmpty
            if (output.isEmpty())
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);
            if (obj.has("error")) {
                if (tryNext)
                    return getUUIDBackup(name, true);
                return null;
            }

            return obj.get("id").getAsString();
        } catch (IOException ignored) {
        }
        if (tryNext)
            return getUUIDBackup(name, true);

        return null;
    }

    protected String getUUIDBackup(String name, boolean tryNext) throws SkinRequestException {
        if (tryNext)
            logger.debug("Trying backup API to get UUID for player " + name + ".");

        try {
            final String output = readURL(UUID_URL_MINETOOLS.replace("%name%", name), MetricsCounter.Service.MINE_TOOLS, 10000);
            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            /* Depricated code
            if (obj.has("status") && obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                return getUUIDMojang(name, true);
            } */

            if (obj.get("id") != null)
                return obj.get("id").getAsString();
        } catch (IOException ignored) {
        }
        throw new SkinRequestException(Locale.NOT_PREMIUM); // TODO: check flow of code
    }

    public IProperty getProfile(String uuid) {
        return getProfile(uuid, true);
    }

    public IProperty getProfile(String uuid, boolean tryNext) {
        try {
            final String output = readURL(SKIN_URL_ASHCON.replace("%uuid%", uuid), MetricsCounter.Service.ASHCON);
            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            if (obj.has("textures")) {
                final JsonObject textures = obj.get("textures").getAsJsonObject();
                final JsonObject rawTextures = textures.get("raw").getAsJsonObject();

                return createProperty("textures", rawTextures.get("value").getAsString(), rawTextures.get("signature").getAsString());
            }
        } catch (Exception ignored) {
        }
        if (tryNext)
            return getProfileMojang(uuid, true);

        return null;
    }

    public IProperty getProfileMojang(String uuid, boolean tryNext) {
        if (tryNext)
            logger.debug("Trying Mojang API to get skin property for " + uuid + ".");

        try {
            final String output = readURL(SKIN_URL_MOJANG.replace("%uuid%", uuid), MetricsCounter.Service.MOJANG);
            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);
            final GenericProperty property = new GenericProperty();
            if (obj.has("properties") && property.valuesFromJson(obj)) {
                return createProperty("textures", property.getValue(), property.getSignature());
            }
        } catch (Exception ignored) {
        }
        if (tryNext)
            return getProfileBackup(uuid, true);

        return null;
    }

    protected IProperty getProfileBackup(String uuid, boolean tryNext) {
        if (tryNext)
            logger.debug("Trying backup API to get skin property for " + uuid + ".");

        try {
            final String output = readURL(SKIN_URL_MINETOOLS.replace("%uuid%", uuid), MetricsCounter.Service.MINE_TOOLS, 10000);
            final JsonObject obj = new Gson().fromJson(output, JsonObject.class);
            if (obj.has("raw")) {
                final JsonObject raw = obj.getAsJsonObject("raw");
                // Break on ERR
                if (raw.has("status") && raw.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    throw new SkinRequestException("");
                }

                GenericProperty property = new GenericProperty();
                if (property.valuesFromJson(raw)) {
                    return createProperty("textures", property.getValue(), property.getSignature());
                }
            }
        } catch (Exception ignored) {
        }
        if (tryNext)
            logger.debug(SRLogLevel.WARNING, "Failed to get skin property from backup API. (" + uuid + ")");

        return null;
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

        String line;
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = in.readLine()) != null)
            output.append(line);

        in.close();
        return output.toString();
    }


    @RequiredArgsConstructor
    public enum HardcodedSkins {
        STEVE("ewogICJ0aW1lc3RhbXAiIDogMTU4Nzc0NTY0NTA2NCwKICAicHJvZmlsZUlkIiA6ICJlNzkzYjJjYTdhMmY0MTI2YTA5ODA5MmQ3Yzk5NDE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVfSG9zdGVyX01hbiIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZDNiMDZjMzg1MDRmZmMwMjI5Yjk0OTIxNDdjNjlmY2Y1OWZkMmVkNzg4NWY3ODUwMjE1MmY3N2I0ZDUwZGUxIgogICAgfQogIH0KfQ", "m4AHOr3btZjX3Rlxkwb5GMf69ZUo60XgFtwpADk92DgX1zz+ZOns+KejAKNpfVZOxRAVpSWwU8+ZNgiEvOdgyTFEW4yVXthQSdBYsKGtpifxOTb8YEXznmq+yVfA1iWZx2P72TbTmbZgG/TyOViMvyqUQsVmaZDCSW/M+ImDTmzrB3KrRW25XY9vaWshNvsaVH8SfrIOm3twtiLc7jRf+sipyxWcbFsw/Kh+6GyCKgID4tgTsydu5nhthm9A5Sa1ZI8LeySSFLzU5VirZeT3LvybHkikART/28sDaTs66N2cjFDNcdtjpWb4y0G9aLdwcWdx8zoYlVXcSWGW5aAFIDLKngtadHxRWnhryydz6YrlrBMflj4s6Qf9meIPI18J6eGWnBC8fhSwsfsJCEq6SKtkeQIHZ9g0sFfqt2YLG3CM6ZOHz2pWedCFUlokqr824XRB/h9FCJIRPIR6kpOK8barZTWwbL9/1lcjwspQ+7+rVHrZD+sgFavQvKyucQqE+IXL7Md5qyC5CYb2WMkXAhjzHp5EUyRq5FiaO6iok93gi6reh5N3ojuvWb1o1cOAwSf4IEaAbc7ej5aCDW5hteZDuVgLvBjPlbSfW9OmA8lbvxxgXR2fUwyfycUVFZUZbtgWzRIjKMOyfgRq5YFY9hhAb3BEAMHeEPqXoSPF5/A="),
        ALEX("ewogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJpZCIgOiAiMWRjODQ3ZGViZTg2NDBhOGEzODExODkwZTk0ZTdmNmIiLAogICAgICAidHlwZSIgOiAiU0tJTiIsCiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAgICJwcm9maWxlSWQiIDogIjc3MjdkMzU2NjlmOTQxNTE4MDIzZDYyYzY4MTc1OTE4IiwKICAgICAgInRleHR1cmVJZCIgOiAiZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfSwKICAic2tpbiIgOiB7CiAgICAiaWQiIDogIjFkYzg0N2RlYmU4NjQwYThhMzgxMTg5MGU5NGU3ZjZiIiwKICAgICJ0eXBlIiA6ICJTS0lOIiwKICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmI5YWIzNDgzZjgxMDZlY2M5ZTc2YmQ0N2M3MTMxMmIwZjE2YTU4Nzg0ZDYwNjg2NGYzYjNlOWNiMWZkN2I2YyIsCiAgICAicHJvZmlsZUlkIiA6ICI3NzI3ZDM1NjY5Zjk0MTUxODAyM2Q2MmM2ODE3NTkxOCIsCiAgICAidGV4dHVyZUlkIiA6ICJmYjlhYjM0ODNmODEwNmVjYzllNzZiZDQ3YzcxMzEyYjBmMTZhNTg3ODRkNjA2ODY0ZjNiM2U5Y2IxZmQ3YjZjIiwKICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgIH0KICB9LAogICJjYXBlIiA6IG51bGwKfQ==", "Bl/hfaMcGIDwYEl1fqSiPxj2zTGrTMJomqEODvB97VbJ8cs7kfLZIC1bRaCzlFHo5BL0bChL8aQRs/DJGkxmfOC5PfQXubxA4/PHgnNq6cqPZvUcC4hjWdTSKAbZKzHDGiH8aQtuEHVpHeb9T+cutsS0i2zEagWeYVquhFFtctSZEh5+JWxQOba+eh7xtwmzlaXfUDYguzHSOSV4q+hGzSU6osxO/ddiy4PhmFX1MZo237Wp1jE5Fjq+HN4J/cpm/gbtGQBfCuTE7NP3B+PKCXAMicQbQRZy+jaJ+ysK8DJP/EulxyERiSLO9h8eYF5kP5BT5Czhm9FoAwqQlpTXkJSllcdAFqiEZaRNYgJqdmRea4AeyCLPz83XApTvnHyodss1lQpJiEJuyntpUy1/xYNv+EdrNvwCnUPS/3/+jA/VKjAiR9ebKTVZL8A5GHR4mKp7uaaL1DouQa2VOJmQHKo3++v6HGsz1Xk6J7n/8qVUp3oS79WqLxlZoZPBIuQ90xt8Yqhxv6e9FXD4egHsabVj5TO/bZE6pEUaVTrKv49ciE0RqjZHxR5P13hFsnMJTXnT5rzAVCkJOvjaPfZ70WiLJL3X4OOt1TrGK0CoBKQt7yLbU5Eap6P+SLusHrZx+oU4Xspimb79splBxOsbhvb+olbRrJhmxIcrhVIqHDY="),
        SKINSRESTORER("ewogICJ0aW1lc3RhbXAiIDogMTU5OTMzNjMxNjYzNSwKICAicHJvZmlsZUlkIiA6ICJlYTk3YThkMTFmNzE0Y2UwYTc2ZDdjNTI1M2NjN2Y3MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTXJfS2VrcyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MTM1NWUwYzU3YmM4NGE2OTQxYjQwN2Q4NDgwMzA3NjkzYWM5ZWJmNzg1NDEyNGVmMTc1NjJhNDVjZTdiMTEwIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "x2yGMK0NqTuiqHfKcHphGFk2UXoBHNpxrHvVD5qMhB5ZKB9pftRcuov7GwayUD95S9z8bgdRyujBefMRijYVYA+BHzYqeGX0b2qeTXLiB4iYarnVs2wzxMLL1mNTBjuyuvPe97tAmRQ4N6s+Znjy5vQ491Fgf5CS4G36f86yROKHRFieNsck5vFSZ98mE4q6o8mK94YNc6+gUXo6O0+hKcLQ48otpiA9zx0Ip15AjLxSvHAfFnH/YVTNHSEIem7nChIQAKuDs8dKibZ6inc3LmC2fmNK0YWuzmGVYg5LqdMycRDgc5C3XU5rA80N/VrdDAY4/6X7bFH+Ib5i35J+Lk31prXRcmxjifF+aAI8xuqdrJMQuxVHJc9QVhclUgLWiGrsEzBqLmCSGmc1lz8dE0ycHahZITheXuLEW3b85y+wsG38xJB61TTU1m68ykdzRwO6IXFKZAkbqyXc6p4PCmPeSaD1Y+Jow9CHYMG8Lk7P/uZoUE96sVVgK/GiSvW40AbhuqJRfPZBjlCR4HBJCJLNep9/66IyVFimKTjWWsyxFWs3gLzDW1ULealS/1IzurRpR0/eH9ZyFUxvthf96FHYAyfH1YmjS1evBPZkC3m7NR8JZ+AosYMMnxsZo21YWYDiwWPdv+98d6z5kZwKVPX8H9GBsmXq2xeLfBF/O4M=");

        @Getter
        private final String value;
        private final String signature;
    }
}
