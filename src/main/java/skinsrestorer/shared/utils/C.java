package skinsrestorer.shared.utils;

import java.util.regex.Pattern;

import skinsrestorer.shared.storage.Config;

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
        boolean isValidAndAllowed = false; // if the URL is not valid nor allowed, this will simply be what we return

        if (urlPattern.matcher(url).matches()) {
            for (String possiblyAllowedUrl : Config.ALLOWED_URLS) {
                if (url.startsWith(possiblyAllowedUrl)) {
                    isValidAndAllowed = true;
                    break;
                }
            }
        }

        return isValidAndAllowed;
    }

    public static boolean matchesRegex(String url) {
        return urlPattern.matcher(url).matches();
    }
}
