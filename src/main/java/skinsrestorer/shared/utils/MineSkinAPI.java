package skinsrestorer.shared.utils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.Setter;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;

public class MineSkinAPI {
    @Getter
    @Setter
    private SkinStorage skinStorage;
    @Setter
    private SRLogger logger;

    public MineSkinAPI(SRLogger logger) {
        this.logger = logger;
    }

    public String guessSkinType(String url) {
        try {
            BufferedImage image = ImageIO.read(new URL(url)).getSubimage(54, 20, 2, 12);
            if (image == null)
                return "steve";
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int clr = image.getRGB(x, y);
                    int alpha = (clr & 0xff000000) >> 24;
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;
                    if (alpha != 0 && (!(red == 255 || red == 0) || !(green == 255 || green == 0) || !(blue == 255 || blue == 0))) {
                        return "steve";
                    }
                }
            }
            return "alex";
        } catch (Exception ignored) {
        }
        return "steve";
    }

    public Object genSkin(String url) throws SkinRequestException {
        return genSkin(url, null);
    }

    public Object genSkin(String url, String isSlim) throws SkinRequestException {
        String err_resp = "";
        if (isSlim == null)
            isSlim = guessSkinType(url);
        try {
            String query = "";
            if (isSlim.equalsIgnoreCase("alex") || isSlim.equalsIgnoreCase("a") || isSlim.equalsIgnoreCase("true") || isSlim.equalsIgnoreCase("yes") || isSlim.equalsIgnoreCase("y") || isSlim.equalsIgnoreCase("slim"))
                query += "model=" + URLEncoder.encode("slim", "UTF-8") + "&";
            query += "url=" + URLEncoder.encode(url, "UTF-8");
            String output;
            try {
                err_resp = "";
                JsonObject obj;

                output = queryURL("https://api.mineskin.org/generate/url/", query, 90000);
                if (output == "") { //when both api time out
                    throw new SkinRequestException(Locale.ERROR_UPDATING_SKIN);
                }
                JsonElement elm = new JsonParser().parse(output);
                obj = elm.getAsJsonObject();

                if (obj.has("data")) {
                    JsonObject dta = obj.get("data").getAsJsonObject();
                    if (dta.has("texture")) {
                        JsonObject tex = dta.get("texture").getAsJsonObject();
                        return this.skinStorage.createProperty("textures", tex.get("value").getAsString(), tex.get("signature").getAsString());
                    }
                } else if (obj.has("error")) {
                    err_resp = obj.get("error").getAsString();
                    if (err_resp.equals("Failed to generate skin data") || err_resp.equals("Too many requests")) {
                        logger.log("[SkinsRestorer] MS API skin generation fail (accountId:"+obj.get("accountId").getAsInt()+"); trying again... ");
                        if (obj.has("delay"))
                            TimeUnit.SECONDS.sleep(obj.get("delay").getAsInt());
                        return genSkin(url, isSlim); // try again if given account fails (will stop if no more accounts)
                    } else if (err_resp.equals("No accounts available")) {
                        logger.log("[ERROR] MS No accounts available " + url);
                        throw new SkinRequestException(Locale.ERROR_MS_FULL);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "[ERROR] MS API Failure IOException (connection/disk): (" + url + ") " + e.getLocalizedMessage());
            } catch (JsonSyntaxException e) {
                logger.log(Level.WARNING, "[ERROR] MS API Failure JsonSyntaxException (encoding): (" + url + ") " + e.getLocalizedMessage());
            }
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, "[ERROR] MS UnsupportedEncodingException");
        } catch (InterruptedException e) {
        }
        // throw exception after all tries have failed
        logger.log("[ERROR] MS:could not generate skin url: " + url);
        logger.log("[ERROR] MS:reason: " + err_resp);
        if (!(err_resp.matches("")))
            throw new SkinRequestException(Locale.ERROR_INVALID_URLSKIN_2); //todo: consider sending err_resp to admins
        else
            throw new SkinRequestException(Locale.MS_API_FAILED);
    }

    private String queryURL(String url, String query, int timeout) throws IOException {
        for (int i = 0; i < 3; i++) { // try 3 times, if server not responding
            try {
                MetricsCounter.incrAPI(url);
                HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-length", String.valueOf(query.length()));
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("User-Agent", "SkinsRestorer");
                con.setConnectTimeout(timeout);
                con.setReadTimeout(timeout);
                con.setDoOutput(true);
                con.setDoInput(true);
                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                String outstr = "";
                InputStream _is;
                try {
                    _is = con.getInputStream();
                } catch (Exception e) {
                    _is = con.getErrorStream();
                }
                DataInputStream input = new DataInputStream(_is);
                for (int c = input.read(); c != -1; c = input.read()) {
                    outstr += (char) c; //todo String concatenation in loop
                }
                input.close();
                return outstr;
            } catch (Exception ignored) {
            }
        }
        return "";
    }
}
