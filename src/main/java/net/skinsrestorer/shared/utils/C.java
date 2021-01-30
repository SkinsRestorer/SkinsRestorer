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

import net.skinsrestorer.shared.storage.Config;

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
