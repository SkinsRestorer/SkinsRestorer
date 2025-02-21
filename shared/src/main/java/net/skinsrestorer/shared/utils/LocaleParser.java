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

import java.util.Locale;
import java.util.Optional;

public class LocaleParser {
    public static Optional<Locale> parseLocale(String locale) {
        if (locale == null) {
            return Optional.empty();
        }

        String[] split = locale.split("_");
        if (split.length == 1) {
            return Optional.of(Locale.of(split[0]));
        } else if (split.length == 2) {
            return Optional.of(Locale.of(split[0], split[1]));
        } else {
            return Optional.empty();
        }
    }

    public static Locale parseLocaleStrict(String locale) {
        String[] split = locale.split("_");
        if (split.length == 1) {
            return Locale.of(split[0]);
        } else if (split.length == 2) {
            return Locale.of(split[0], split[1]);
        }

        throw new IllegalArgumentException("Invalid locale: %s".formatted(locale));
    }
}
