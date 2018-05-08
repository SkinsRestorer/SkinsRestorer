package skinsrestorer.shared.utils;

import java.util.regex.Pattern;

public class C {

    private static Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    public static String c(String msg) {
        return msg.replaceAll("&", "§");
    }

    public static boolean validUsername(String username) {
        if (username.length() > 16 )
            return false;

        return namePattern.matcher(username).matches();
    }
}
