package skinsrestorer.shared.utils;

import java.util.regex.Pattern;

public class C {

//    private static Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static Pattern namePattern = Pattern.compile("^\\W?[a-zA-Z0-9_]{3,16}$");
    private static Pattern urlPattern = Pattern.compile("^https?://.*");

    public static String c(String msg) {
        return msg.replaceAll("&", "§");
    }

    public static boolean validUsername(String username) {
        //if (username.length() > 16)
        //    return false; //won't be needed as the regex allows only 16 characters + one Non-WOrd character in front

        return namePattern.matcher(username).matches();
    }

    public static boolean validUrl(String url) {
        return urlPattern.matcher(url).matches();
    }
}
