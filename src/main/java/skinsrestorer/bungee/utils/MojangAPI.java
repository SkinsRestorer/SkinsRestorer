package skinsrestorer.bungee.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import skinsrestorer.bungee.storage.Locale;
import skinsrestorer.bungee.storage.SkinStorage;
import skinsrestorer.shared.utils.ProxyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
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
    public static Object getSkinProperty(String uuid) throws SkinRequestException {
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
            return false;
        } catch (Exception e) {
            System.out.println("[SkinsRestorer] Switching to Mojang to get skin property.");
            return getSkinPropertyMojang(uuid);
        }
    }

    public static Object getSkinPropertyMojang(String uuid) throws SkinRequestException {
        String output;
        try {
            output = readURL(skinurl_mojang.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (property.valuesFromJson(obj)) {
                return SkinStorage.createProperty("textures", property.getValue(), property.getSignature());
            }
            return false;
        } catch (Exception e) {
            System.out.println("[SkinsRestorer] Switching to proxy to get skin property.");
            return getSkinPropertyProxy(uuid);
        }
    }

    public static Object getSkinPropertyProxy(String uuid) throws SkinRequestException {
        String output;
        try {
            output = readURLProxy(skinurl_mojang.replace("%uuid%", uuid));
            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            Property property = new Property();

            if (property.valuesFromJson(obj)) {
                return SkinStorage.createProperty("textures", property.getValue(), property.getSignature());
            }
            return false;
        } catch (Exception e) {
            System.out.println("[SkinsRestorer] Failed to get skin property from proxy.");
            return false;
        }
    }

    public static String getStringBetween(final String base, final String begin, final String end) {
        try {
            Pattern patbeg = Pattern.compile(Pattern.quote(begin));
            Pattern patend = Pattern.compile(Pattern.quote(end));

            int resbeg = 0;
            int resend = base.length() - 1;

            Matcher matbeg = patbeg.matcher(base);

            while (matbeg.find())
                resbeg = matbeg.end();

            Matcher matend = patend.matcher(base);

            while (matend.find())
                resend = matend.start();

            return base.substring(resbeg, resend);
        } catch (Exception e) {
            return base;
        }
    }

    /**
     * @param name - Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException - If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    public static String getUUID(String name) throws SkinRequestException {
        String output;
        try {
            output = readURL(uuidurl.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("status")) {
                if (obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    System.out.println("[SkinsRestorer] Switching to Mojang to get UUID.");
                    return getUUIDMojang(name);
                }
            }

            if (obj.get("id").getAsString().equalsIgnoreCase("null"))
                throw new SkinRequestException(Locale.TITLE.toString() + Locale.NOT_PREMIUM);

            return obj.get("id").getAsString();
        } catch (IOException e) {
            System.out.println("[SkinsRestorer] Switching to Mojang to get UUID.");
            return getUUIDMojang(name);
        }
    }

    public static String getUUIDMojang(String name) throws SkinRequestException {
        String output;
        try {
            output = readURL(uuidurl_mojang.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (output.isEmpty()) {
                throw new SkinRequestException(Locale.TITLE.toString() + Locale.NOT_PREMIUM);
            } else if (obj.has("error")) {
                return getUUIDProxy(name);
            }

            return obj.get("id").getAsString();
        } catch (IOException e) {
            System.out.println("[SkinsRestorer] Switching to proxy to get UUID.");
            return getUUIDProxy(name);
        }
    }

    public static String getUUIDProxy(String name) throws SkinRequestException {
        String output;
        try {
            output = readURLProxy(uuidurl_mojang.replace("%name%", name));

            JsonElement element = new JsonParser().parse(output);
            JsonObject obj = element.getAsJsonObject();

            if (output.isEmpty()) {
                throw new SkinRequestException(Locale.TITLE.toString() + Locale.NOT_PREMIUM);
            } else if (obj.has("error")) {
                throw new SkinRequestException(Locale.TITLE.toString() + Locale.ALT_API_FAILED);
            }

            return obj.get("id").getAsString();
        } catch (IOException e) {
            throw new SkinRequestException(e.getMessage());
        }
    }

    public static MojangAPI get() {
        return mojangapi;
    }

    public static int rand(int High) {
        Random r = new Random();
        return r.nextInt(High - 1) + 1;
    }

    private static String readURL(String url) throws MalformedURLException, IOException, SkinRequestException {
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

    private static String readURLProxy(String url) throws MalformedURLException, IOException, SkinRequestException {
        HttpURLConnection con = null;
        String ip = null;
        int port = 0;
        String proxyStr = null;
        List<String> list = ProxyManager.getList();
        proxyStr = list.get(rand(list.size() - 1));
        String[] realProxy = proxyStr.split(":");
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

    }

    private static class Property {
        private String name;
        private String value;
        private String signature;

        public boolean valuesFromJson(JsonObject obj) {
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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
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