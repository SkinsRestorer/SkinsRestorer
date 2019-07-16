package skinsrestorer.shared.utils;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

import javax.net.ssl.HttpsURLConnection;

public class MineSkinAPI {
    public static Object genSkin(String url, Boolean isSlim) throws MojangAPI.SkinRequestException {
        try {
            String query = "";
            if (isSlim) { query += "model="+URLEncoder.encode("slim","UTF-8")+"&"; }
            query += "url="+URLEncoder.encode(url,"UTF-8");
            String output;
            try {
//                System.out.println("[SkinsRestorer] using MineSkin API");
                output = queryURL("https://api.mineskin.org/generate/url", query, 5000);
                JsonElement elm = new JsonParser().parse(output);
                JsonObject obj = elm.getAsJsonObject();
                if (obj.has("data")) {
                    JsonObject dta = obj.get("data").getAsJsonObject();
                    if (dta.has("texture")) {
//                        System.out.println("[SkinsRestorer] MS API success: https://mineskin.org/"+obj.get("id").getAsInt());
                        JsonObject tex = dta.get("texture").getAsJsonObject();
                        return SkinStorage.createProperty("textures", tex.get("value").getAsString(), tex.get("signature").getAsString());
                    }
                }
                if (obj.has("error")) {
                    if (obj.get("error").getAsString().equals("Failed to generate skin data")) {
//                        System.out.println("[SkinsRestorer] MS API skin generation fail (accountId:"+obj.get("accountId").getAsInt()+"); trying again. ");
                        if (obj.has("nextRequest"))
                            TimeUnit.SECONDS.sleep(obj.get("nextRequest").getAsInt());
                        return genSkin(url, isSlim); // try again if given account fails (will stop if no more accounts)
                    } else if (obj.get("error").getAsString().equals("No accounts available")) {
                        System.out.println(Locale.ERROR_MS_FULL);
                        throw new MojangAPI.SkinRequestException(Locale.ERROR_MS_FULL);
                    }
                }
            } catch (IOException e) {
                System.out.println("[SkinsRestorer] MS API Failure (" + url + ") "+e.getLocalizedMessage());
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("[SkinsRestorer] [ERROR] UnsupportedEncodingException");
        } catch (InterruptedException e) {}
        // throw exception after all tries have failed
        throw new MojangAPI.SkinRequestException(Locale.MS_API_FAILED);
    }

    private static String queryURL(String url, String query, int timeout) throws IOException {
        MetricsCounter.incrAPI(url);
        HttpsURLConnection con = (HttpsURLConnection)new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-length", String.valueOf(query.length()));
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
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
        for( int c = input.read(); c != -1; c = input.read() ) { outstr += (char)c; }
        input.close();
        return outstr;
    }
}
