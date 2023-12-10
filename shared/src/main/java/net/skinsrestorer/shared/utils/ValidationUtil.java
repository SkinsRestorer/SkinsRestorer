/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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

public class ValidationUtil {
    private ValidationUtil() {
    }

    public static boolean validMojangUsername(String username) {
        // Note: there are exceptions to players with under 3 characters, who bought the game early in its development.
        if (username.length() > 16) {
            return false;
        }

        // For some reason, Apache's Lists.charactersOf is faster than character indexing for small strings.
        for (char c : username.toCharArray()) {
            // Note: Players who bought the game early in its development can have "-" in usernames.
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && !(c >= 'A' && c <= 'Z') && c != '_' && c != '-') {
                return false;
            }
        }

        return true;
    }

    public static boolean validSkinUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
