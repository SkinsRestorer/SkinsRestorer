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

import java.util.Locale;

public class LocaleParser {
    public static Locale parseLocale(String locale) {
        if (locale == null) {
            return getDefaultLocale();
        }
        String[] split = locale.split("_");
        if (split.length == 1) {
            return new Locale(split[0]);
        } else if (split.length == 2) {
            return new Locale(split[0], split[1]);
        } else {
            return getDefaultLocale();
        }
    }

    public static Locale parseLocaleStrict(String locale) {
        String[] split = locale.split("_");
        if (split.length == 1) {
            return new Locale(split[0]);
        } else if (split.length == 2) {
            return new Locale(split[0], split[1]);
        }
        throw new IllegalArgumentException("Invalid locale: " + locale);
    }

    public static Locale getDefaultLocale() {
        Locale locale = Config.LANGUAGE;

        if (locale == null) {
            locale = Locale.getDefault();
        }

        if (locale == null) {
            locale = Locale.ENGLISH;
        }

        return locale;
    }
}
