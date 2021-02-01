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
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.Setter;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MineSkinAPI {
    private @Getter
    @Setter
    SkinStorage skinStorage;
    private final SRLogger logger;

    public MineSkinAPI(SRLogger logger) {
        this.logger = logger;
    }

    public Object genSkin(String url) throws SkinRequestException {
        String errResp = "";

        try {
            errResp = "";

            String output = queryURL(URLEncoder.encode(url, "UTF-8"));
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

    private String queryURL(String query) throws IOException {
        for (int i = 0; i < 3; i++) { // try 3 times, if server not responding
            try {
                MetricsCounter.incrAPI("https://api.mineskin.org/generate/url/");
                HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.mineskin.org/generate/url/").openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Content-length", String.valueOf(query.length()));
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("User-Agent", "SkinsRestorer");
                con.setConnectTimeout(90000);
                con.setReadTimeout(90000);
                con.setDoOutput(true);
                con.setDoInput(true);

                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                StringBuilder outstr = new StringBuilder();
                InputStream is;

                try {
                    is = con.getInputStream();
                } catch (Exception e) {
                    is = con.getErrorStream();
                }

                DataInputStream input = new DataInputStream(is);
                for (int c = input.read(); c != -1; c = input.read())
                    outstr.append((char) c);

                input.close();
                return outstr.toString();
            } catch (Exception ignored) {
            }
        }

        return "";
    }
}
