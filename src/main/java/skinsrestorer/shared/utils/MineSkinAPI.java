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
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MineSkinAPI {
    private @Getter
    @Setter
    SkinStorage skinStorage;
    private SRLogger logger;

    public MineSkinAPI(SRLogger logger) {
        this.logger = logger;
    }

    public Object genSkin(String url) throws SkinRequestException {
        String errResp = "";

        try {
            errResp = "";

            String output = queryURL("https://api.mineskin.org/generate/url/", "url=" + URLEncoder.encode(url, "UTF-8"), 90000);
            if (output.isEmpty()) //when both api time out
                throw new SkinRequestException(Locale.ERROR_UPDATING_SKIN);

            JsonElement elm = new JsonParser().parse(output);
            JsonObject obj = elm.getAsJsonObject();

            if (obj.has("data")) {
                JsonObject dta = obj.get("data").getAsJsonObject();

                if (dta.has("texture")) {
                    JsonObject tex = dta.get("texture").getAsJsonObject();
                    return this.skinStorage.createProperty("textures", tex.get("value").getAsString(), tex.get("signature").getAsString());
                }
            } else if (obj.has("error")) {
                errResp = obj.get("error").getAsString();

                if (errResp.equals("Failed to generate skin data") || errResp.equals("Too many requests")) {
                    logger.log("[SkinsRestorer] MS API skin generation fail (accountId:" + obj.get("accountId").getAsInt() + "); trying again... ");

                    if (obj.has("delay"))
                        TimeUnit.SECONDS.sleep(obj.get("delay").getAsInt());

                    return genSkin(url); // try again if given account fails (will stop if no more accounts)
                } else if (errResp.equals("No accounts available")) {
                    logger.log("[ERROR] MS No accounts available " + url);
                    throw new SkinRequestException(Locale.ERROR_MS_FULL);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "[ERROR] MS API Failure IOException (connection/disk): (" + url + ") " + e.getLocalizedMessage());
        } catch (JsonSyntaxException e) {
            logger.log(Level.WARNING, "[ERROR] MS API Failure JsonSyntaxException (encoding): (" + url + ") " + e.getLocalizedMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // throw exception after all tries have failed
        logger.log("[ERROR] MS:could not generate skin url: " + url);
        logger.log("[ERROR] MS:reason: " + errResp);

        if (!errResp.isEmpty())
            throw new SkinRequestException(Locale.ERROR_INVALID_URLSKIN); //todo: consider sending err_resp to admins
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
