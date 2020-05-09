package skinsrestorer.shared.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;


public class MojangAPI {
    private static final String uuidurl = "https://api.minetools.eu/uuid/%name%";
    private static final String uuidurl_mojang = "https://api.mojang.com/users/profiles/minecraft/%name%";
    private static final String uuidurl_backup = "https://api.ashcon.app/mojang/v2/user/%name%";

    private static final String skinurl = "https://api.minetools.eu/profile/%uuid%";
    private static final String skinurl_mojang = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String skinurl_backup = "https://api.ashcon.app/mojang/v2/user/%uuid%";

    @Getter
    @Setter
    private SkinStorage skinStorage;
    @Setter
    private SRLogger logger;

    public MojangAPI(SRLogger logger) {
        this.logger = logger;
    }

    // TODO Deal with duplicated code

    /**
     * Returned object needs to be casted to either BungeeCord's property or
     * Mojang's property (old or new)
     *
     * @return Property object (New Mojang, Old Mojang or Bungee)
     **/
    public Object getSkinProperty(String uuid, boolean tryNext) {
        String output;
        try {
            output = readURL(skinurl.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (obj.has("raw")) {
                JsonObject raw = obj.getAsJsonObject("raw");

                if (raw.has("status")) {
                    if (raw.get("status").getAsString().equalsIgnoreCase("ERR")) {
                        return getSkinPropertyMojang(uuid);
                    }
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

    public Object getSkinProperty(String uuid) {
        return getSkinProperty(uuid, true);
    }

    public Object getSkinPropertyMojang(String uuid, boolean tryNext) {
        this.logger.log("Trying Mojang API to get skin property for " + uuid + ".");

        String output;
        try {
            output = readURL(skinurl_mojang.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (obj.has("properties")) {
                if (property.valuesFromJson(obj)) {
                    return this.getSkinStorage().createProperty("textures", property.getValue(), property.getSignature());
                }
            }

        } catch (Exception e) {
            if (tryNext)
                return getSkinPropertyBackup(uuid);
        }
        return null;
    }

    public Object getSkinPropertyMojang(String uuid) {
        return getSkinPropertyMojang(uuid, true);
    }

    public Object getSkinPropertyBackup(String uuid) {
        this.logger.log("Trying backup API to get skin property for " + uuid + ".");

        String output;
        try {
            output = readURL(skinurl_backup.replace("%uuid%", uuid), 10000);
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();
            JsonObject textures = obj.get("textures").getAsJsonObject();
            JsonObject rawTextures = textures.get("raw").getAsJsonObject();

            Property property = new Property();
            property.setValue(rawTextures.get("value").getAsString());
            property.setSignature(rawTextures.get("signature").getAsString());

            return this.getSkinStorage().createProperty("textures", property.getValue(), property.getSignature());

        } catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to get skin property from backup API. (" + uuid + ")");
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
            output = readURL(uuidurl.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("status")) {
                if (obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    return getUUIDMojang(name);
                }
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

    public String getUUID(String name) throws SkinRequestException {
        return getUUID(name, true);
    }

    public String getUUIDMojang(String name, boolean tryNext) throws SkinRequestException {
        this.logger.log("Trying Mojang API to get UUID for player " + name + ".");

        String output;
        try {
            output = readURL(uuidurl_mojang.replace("%name%", name));

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

    public String getUUIDMojang(String name) throws SkinRequestException {
        return getUUIDMojang(name, true);
    }

    public String getUUIDBackup(String name) throws SkinRequestException {
        this.logger.log("Trying backup API to get UUID for player " + name + ".");

        String output;
        try {
            output = readURL(uuidurl_backup.replace("%name%", name), 10000);

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
        /*System.out.println("USED STRING URL = " + url);
        System.out.println(output.toString()); // testing */
        return output.toString();
    }

    private class Property {
        private String name;
        private String value;
        private String signature;

        boolean valuesFromJson(JsonObject obj) {
            if (obj.has("properties")) {
                JsonArray properties = obj.getAsJsonArray("properties");
                if (properties.size() > 0) {
                    JsonObject propertiesObject = properties.get(0).getAsJsonObject();

                    String signature = propertiesObject.get("signature").getAsString();
                    String value = propertiesObject.get("value").getAsString();

                    this.setSignature(signature);
                    this.setValue(value);

                    return true;
                }
            }

            return false;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }

        String getSignature() {
            return signature;
        }

        void setSignature(String signature) {
            this.signature = signature;
        }
    }

    private class HTTPResponse {
        private String output;
        private int status;

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }
    }
}
