package skinsrestorer.shared.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojangAPI {

    private static final String uuidurl = "https://api.minetools.eu/uuid/%name%";
    private static final String uuidurl_mojang = "https://api.mojang.com/users/profiles/minecraft/%name%";

    private static final String skinurl = "https://api.minetools.eu/profile/%uuid%";
    private static final String skinurl_mojang = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";

    private static MojangAPI mojangapi = new MojangAPI();

    // TODO Deal with duplicated code

    /**
     * Returned object needs to be casted to either BungeeCord's property or
     * Mojang's property (old or new)
     *
     * @return Property object (New Mojang, Old Mojang or Bungee)
     **/
    public static Object getSkinProperty(String uuid, boolean tryNext) {
        String output;
        try {
            output = readURL(skinurl.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (obj.has("raw")) {
                JsonObject raw = obj.getAsJsonObject("raw");

                if (property.valuesFromJson(raw)) {
                    return SkinStorage.createProperty("textures", property.getValue(), property.getSignature());
                }
            }
            return null;
        } catch (Exception e) {
            if (tryNext) {
                System.out.println("[SkinsRestorer] Switching to Mojang to get skin property. (" + uuid + ")");
                return getSkinPropertyMojang(uuid);
            }
        }
        return null;
    }

    public static Object getSkinProperty(String uuid) {
        return getSkinProperty(uuid, true);
    }

    public static Object getSkinPropertyMojang(String uuid, boolean tryNext) {
        String output;
        try {
            output = readURL(skinurl_mojang.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (property.valuesFromJson(obj)) {
                return SkinStorage.createProperty("textures", property.getValue(), property.getSignature());
            }
            return null;
        } catch (Exception e) {
            if (tryNext && Config.CUSTOM_PROXIES_ENABLED) {
                System.out.println("[SkinsRestorer] Switching to proxy to get skin property. (" + uuid + ")");
                return getSkinPropertyProxy(uuid);
            }
        }
        return null;
    }

    public static Object getSkinPropertyMojang(String uuid) {
        return getSkinPropertyMojang(uuid, true);
    }

    public static Object getSkinPropertyProxy(String uuid) {
        String output;
        try {
            output = readURLProxy(skinurl_mojang.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (property.valuesFromJson(obj)) {
                return SkinStorage.createProperty("textures", property.getValue(), property.getSignature());
            }
            return null;
        } catch (Exception e) {
            System.out.println("[SkinsRestorer] Failed to get skin property from proxy. (" + uuid + ")");
            return null;
        }
    }

    /**
     * @param name - Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException - If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    public static String getUUID(String name, boolean tryNext) throws SkinRequestException {
        String output;
        try {
            output = readURL(uuidurl.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("status")) {
                if (obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    if (tryNext) {
                        System.out.println("[SkinsRestorer] Switching to Mojang to get UUID. (" + name + ")");
                        return getUUIDMojang(name);
                    }
                    return null;
                }
            }

            if (obj.get("id").getAsString().equalsIgnoreCase("null"))
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            return obj.get("id").getAsString();
        } catch (IOException e) {
            if (tryNext) {
                System.out.println("[SkinsRestorer] Switching to Mojang to get UUID. (" + name + ")");
                return getUUIDMojang(name);
            }
        }
        return null;
    }

    public static String getUUID(String name) throws SkinRequestException {
        return getUUID(name, true);
    }

    public static String getUUIDMojang(String name, boolean tryNext) throws SkinRequestException {
        String output;
        try {
            output = readURL(uuidurl_mojang.replace("%name%", name));

            if (output.isEmpty())
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("error")) {
                if (tryNext && Config.CUSTOM_PROXIES_ENABLED) {
                    System.out.println("[SkinsRestorer] Switching to proxy to get UUID. (" + name + ")");
                    return getUUIDProxy(name);
                }
                return null;
            }

            return obj.get("id").getAsString();

        } catch (IOException e) {
            if (tryNext && Config.CUSTOM_PROXIES_ENABLED) {
                System.out.println("[SkinsRestorer] Switching to proxy to get UUID. (" + name + ")");
                return getUUIDProxy(name);
            }
        }
        return null;
    }

    public static String getUUIDMojang(String name) throws SkinRequestException {
        return getUUIDMojang(name, true);
    }

    public static String getUUIDProxy(String name) throws SkinRequestException {
        String output;
        try {
            output = readURLProxy(uuidurl_mojang.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (output.isEmpty()) {
                throw new SkinRequestException(Locale.NOT_PREMIUM);
            } else if (obj.has("error")) {
                throw new SkinRequestException(Locale.ALT_API_FAILED);
            }

            return obj.get("id").getAsString();
        } catch (IOException e) {
            throw new SkinRequestException(e.getMessage());
        } catch (ProxyReadException e) {
            throw new SkinRequestException(e.getReason());
        }
    }

    public static MojangAPI get() {
        return mojangapi;
    }

    private static int rand(int High) {
        try {
            Random r = new Random();
            return r.nextInt(High - 1) + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String readURL(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SkinsRestorer");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setDoOutput(true);

        String line;
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = in.readLine()) != null)
            output.append(line);

        in.close();
        return output.toString();
    }

    private static String readURLProxy(String url) throws IOException, ProxyReadException {
        HttpURLConnection con;
        String ip;
        int port;
        String proxyStr;
        List<String> list = ProxyManager.getList();
        if (list == null || list.isEmpty())
            throw new ProxyReadException(Locale.GENERIC_ERROR);

        proxyStr = list.get(rand(list.size() - 1));
        String[] realProxy = proxyStr.split(":");
        if (realProxy.length != 2)
            throw new ProxyReadException(Locale.GENERIC_ERROR);

        ip = realProxy[0];
        port = Integer.valueOf(realProxy[1]);
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        con = (HttpURLConnection) new URL(url).openConnection(proxy);

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SkinsRestorer");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setDoOutput(true);

        String line;
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = in.readLine()) != null)
            output.append(line);

        in.close();

        return output.toString();
    }

    public static class SkinRequestException extends Exception {

        private String reason;

        public SkinRequestException(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        public String getMessage() {
            return reason;
        }

    }

    public static class ProxyReadException extends Exception {

        private String reason;

        public ProxyReadException(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        public String getMessage() {
            return reason;
        }

    }

    private static class Property {
        private String name;
        private String value;
        private String signature;

        boolean valuesFromJson(JsonObject obj) {
            if (obj.has("properties")) {
                JsonArray properties = obj.getAsJsonArray("properties");
                JsonObject propertiesObject = properties.get(0).getAsJsonObject();

                String signature = propertiesObject.get("signature").getAsString();
                String value = propertiesObject.get("value").getAsString();

                this.setSignature(signature);
                this.setValue(value);

                return true;
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

    private static class HTTPResponse {
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
