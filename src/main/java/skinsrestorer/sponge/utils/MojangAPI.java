package skinsrestorer.sponge.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.property.ProfileProperty;
import skinsrestorer.bukkit.SkinsRestorer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojangAPI {

    private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";
    // private static MojangAPI mojangapi = new MojangAPI();

    public static Optional<ProfileProperty> getSkinProperty(String uuid) {
        String output = readURL(skinurl + uuid + "?unsigned=false");

        String sigbeg = "\",\"signature\":\"";
        String mid = "[{\"name\":\"textures\",\"value\":\"";
        String valend = "\"}]";

        String signature = "", value = "";

        value = getStringBetween(output, mid, sigbeg);
        signature = getStringBetween(output, sigbeg, valend);

        return Optional.<ProfileProperty>of(Sponge.getServer().getGameProfileManager().createProfileProperty("textures", value, signature));
    }

    private static String getStringBetween(final String base, final String begin, final String end) {
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

    public static Optional<String> getUUID(String name) {
        String output = readURL(uuidurl + name);
        return Optional.<String>of(output.substring(7, 39));
    }

    private static String readURL(String url) {
        try {
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

            System.out.println(con.toString());
            System.out.println(con.getInputStream());
            in.close();
            System.out.println(output.toString());
            return output.toString();
        } catch (Exception e) {
            return null;
        }
    }
}