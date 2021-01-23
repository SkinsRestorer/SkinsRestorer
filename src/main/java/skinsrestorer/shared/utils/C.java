package skinsrestorer.shared.utils;

import java.util.regex.Pattern;

public class C {

    private static Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static Pattern urlPattern = Pattern.compile("^https?://.*");

    public static String c(String msg) {
        return msg.replaceAll("&", "§");
    }

    public static boolean validUsername(String username) {
        if (username.length() > 16)
            return false;

        return namePattern.matcher(username).matches();
    }

    public static boolean validUrl(String url) {
        return urlPattern.matcher(url).matches()
                && (url.startsWith("https://i.imgur.com/")
                || url.startsWith("http://i.imgur.com/")
                || url.startsWith("i.imgur.com/")
                || url.startsWith("https://storage.googleapis.com/")
                || url.startsWith("http://storage.googleapis.com/")
                || url.startsWith("storage.googleapis.com/")
                || url.startsWith("https://cdn.discordapp.com/")
                || url.startsWith("http://cdn.discordapp.com/")
                || url.startsWith("cdn.discordapp.com/")
        );
    }

    public static boolean matchesRegex(String url) {
        return urlPattern.matcher(url).matches();
    }
}
