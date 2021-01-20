package skinsrestorer.shared.utils;

import java.util.regex.Pattern;

public class C {
    private C() {}

    private static final Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static final Pattern urlPattern = Pattern.compile("^https?://.*");

    public static String c(String msg) {
        return msg.replace("&", "§");
    }

    public static boolean validUsername(String username) {
        if (username.length() > 16)
            return false;

        return namePattern.matcher(username).matches();
    }

    public static boolean validUrl(String url) {
        return urlPattern.matcher(url).matches();
    }
}
