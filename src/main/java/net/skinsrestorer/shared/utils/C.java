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
