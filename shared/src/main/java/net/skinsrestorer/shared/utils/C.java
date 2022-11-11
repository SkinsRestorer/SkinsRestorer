/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
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
 */
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.storage.Config;

public class C {
    private C() {
    }

    public static String c(String msg) {
        char[] b = msg.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    public static boolean validMojangUsername(String username) {
        int len = username.length();
        // Note: there are exceptions to players with under 3 characters, who bought the game early in its development.
        if (len > 16) return false;

        // For some reasons Apache's Lists.charactersOf is faster than character indexing for small strings.
        for (int i = 0; i < len; i++) {
            char c = username.charAt(i);
            // Note: Players who bought the game early in its development can have "-" in usernames.
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && !(c >= 'A' && c <= 'Z') && c != '_' && c != '-')
                return false;
        }
        return true;
    }

    public static boolean validUrl(String url) {
        return (url.startsWith("http://") || url.startsWith("https://"));
    }

    public static boolean allowedSkinUrl(String url) {
        if (Config.RESTRICT_SKIN_URLS_ENABLED) {
            for (String possiblyAllowedUrl : Config.RESTRICT_SKIN_URLS_LIST) {
                if (url.startsWith(possiblyAllowedUrl)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}