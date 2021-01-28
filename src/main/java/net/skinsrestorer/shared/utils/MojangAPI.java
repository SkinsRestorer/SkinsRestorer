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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class MojangAPI {
    private static final String UUID_URL = "https://api.minetools.eu/uuid/%name%";
    private static final String UUID_URL_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%name%";
    private static final String UUID_URL_BACKUP = "https://api.ashcon.app/mojang/v2/user/%name%";

    private static final String SKIN_URL = "https://api.minetools.eu/profile/%uuid%";
    private static final String SKIN_URL_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String SKIN_URL_BACKUP = "https://api.ashcon.app/mojang/v2/user/%uuid%";

    private @Getter @Setter SkinStorage skinStorage;
    private SRLogger logger;

    public MojangAPI(SRLogger logger) {
        this.logger = logger;
    }

    // TODO Deal with duplicated code

    public Object getSkinProperty(String uuid) {
        return getSkinProperty(uuid, true);
    }

    /**
     * Returned object needs to be casted to either BungeeCord's property or
     * Mojang's property (old or new)
     *
     * @return Property object (New Mojang, Old Mojang or Bungee)
     **/
    public Object getSkinProperty(String uuid, boolean tryNext) {
        String output;
        try {
            output = readURL(SKIN_URL.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (obj.has("raw")) {
                JsonObject raw = obj.getAsJsonObject("raw");

                if (raw.has("status") && raw.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    return getSkinPropertyMojang(uuid);
                }

                if (property.valuesFromJson(raw)) {
                    return this.getSkinStorage().createProperty("textures", property.getValue(), property.getSignature());
                }
            }

        } catch (Exception e) {
            if (tryNext)
                return getSkinPropertyMojang(uuid);
        }

        return null;
    }

    public Object getSkinPropertyMojang(String uuid) {
        return getSkinPropertyMojang(uuid, true);
    }

    public Object getSkinPropertyMojang(String uuid, boolean tryNext) {
        logger.log("Trying Mojang API to get skin property for " + uuid + ".");

        String output;
        try {
            output = readURL(SKIN_URL_MOJANG.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (obj.has("properties") && property.valuesFromJson(obj)) {
                return this.getSkinStorage().createProperty("textures", property.getValue(), property.getSignature());
            }

        } catch (Exception e) {
            if (tryNext)
                return getSkinPropertyBackup(uuid);
        }

        return null;
    }

    public Object getSkinPropertyBackup(String uuid) {
        logger.log("Trying backup API to get skin property for " + uuid + ".");

        try {
            String output = readURL(SKIN_URL_BACKUP.replace("%uuid%", uuid), 10000);
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();
            JsonObject textures = obj.get("textures").getAsJsonObject();
            JsonObject rawTextures = textures.get("raw").getAsJsonObject();

            Property property = new Property();
            property.setValue(rawTextures.get("value").getAsString());
            property.setSignature(rawTextures.get("signature").getAsString());

            return this.getSkinStorage().createProperty("textures", property.getValue(), property.getSignature());

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get skin property from backup API. (" + uuid + ")");
        }

        return null;
    }

    /**
     * @param name - Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException - If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    public String getUUID(String name, boolean tryNext) throws SkinRequestException {
        String output;

        try {
            output = readURL(UUID_URL.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("status") && obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                return getUUIDMojang(name);
            }

            if (obj.get("id") == null)
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            return obj.get("id").getAsString();
        } catch (IOException e) {
            if (tryNext)
                return getUUIDMojang(name);
        }

        return null;
    }

    public String getUUIDMojang(String name) throws SkinRequestException {
        return getUUIDMojang(name, true);
    }

    public String getUUIDMojang(String name, boolean tryNext) throws SkinRequestException {
        logger.log("Trying Mojang API to get UUID for player " + name + ".");

        String output;
        try {
            output = readURL(UUID_URL_MOJANG.replace("%name%", name));

            if (output.isEmpty())
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("error")) {
                if (tryNext)
                    return getUUIDBackup(name);
                return null;
            }

            return obj.get("id").getAsString();

        } catch (IOException e) {
            if (tryNext)
                return getUUIDBackup(name);
        }

        return null;
    }

    public String getUUIDBackup(String name) throws SkinRequestException {
        logger.log("Trying backup API to get UUID for player " + name + ".");

        try {
            String output = readURL(UUID_URL_BACKUP.replace("%name%", name), 10000);

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            //System.out.println(output.toString()); //testing
            if (obj.has("code")) {
                if (obj.get("error").getAsString().equalsIgnoreCase("Not Found")) {
                    throw new SkinRequestException(Locale.NOT_PREMIUM);
                }

                throw new SkinRequestException(Locale.ALT_API_FAILED);
            }

            return obj.get("uuid").getAsString().replace("-", "");
        } catch (IOException e) {
            throw new SkinRequestException(Locale.NOT_PREMIUM); //TODO: check flow of code
        }
    }

    private String readURL(String url) throws IOException {
        return readURL(url, 5000);
    }

    private String readURL(String url, int timeout) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        MetricsCounter.incrAPI(url);

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
}
