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

    /**
     * Check if the given string is a valid Minecraft username.
     * Logic is from net.minecraft.util.StringUtil#isValidPlayerName
     *
     * @param str The string to check
     * @return true if the string is a valid Minecraft username
     */
    public static boolean invalidMinecraftUsername(String str) {
        return str.length() > 16 || str.chars().filter(i -> i <= 32 || i >= 127).findAny().isPresent();
    }

    public static boolean validSkinUrl(String str) {
        Optional<URL> uriOptional = SRHelpers.parseURL(str);
        return uriOptional.isPresent() && (uriOptional.get().getProtocol().equals("http") || uriOptional.get().getProtocol().equals("https"));
    }
}
