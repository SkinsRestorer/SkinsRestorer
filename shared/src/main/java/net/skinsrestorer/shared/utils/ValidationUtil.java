/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.utils;

import java.net.URL;
import java.util.Optional;

public class ValidationUtil {
    private ValidationUtil() {
    }

    public static boolean invalidMinecraftUsername(String str) {
        // Note: there are exceptions to players with under 3 characters, who bought the game early in its development.
        if (str.length() > 16) {
            return true;
        }

        // For some reason, Apache's Lists.charactersOf is faster than character indexing for small strings.
        for (char c : str.toCharArray()) {
            // Note: Players who bought the game early in its development can have "-" in usernames.
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && !(c >= 'A' && c <= 'Z') && c != '_' && c != '-') {
                return true;
            }
        }

        return false;
    }

    public static boolean validSkinUrl(String str) {
        Optional<URL> uriOptional = SRHelpers.parseURL(str);
        return uriOptional.isPresent() && (uriOptional.get().getProtocol().equals("http") || uriOptional.get().getProtocol().equals("https"));
    }
}
