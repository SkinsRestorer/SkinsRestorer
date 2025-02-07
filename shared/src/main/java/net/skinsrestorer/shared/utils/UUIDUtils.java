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

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class UUIDUtils {
    public static Optional<UUID> tryParseUniqueId(String str) {
        try {
            return Optional.of(UUID.fromString(str));
        } catch (IllegalArgumentException ignored) {
            // If we have a non-dashed UUID, we can try to convert it to dashed.
            if (str.length() == 32) {
                try {
                    return Optional.of(convertToDashed(str));
                } catch (IllegalArgumentException ignored2) {
                    return Optional.empty();
                }
            }

            return Optional.empty();
        }
    }

    public static @Nullable UUID parseUniqueIdNullable(String str) {
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static UUID convertToDashed(String noDashes) {
        StringBuilder idBuff = new StringBuilder(noDashes);
        idBuff.insert(20, '-');
        idBuff.insert(16, '-');
        idBuff.insert(12, '-');
        idBuff.insert(8, '-');
        return UUID.fromString(idBuff.toString());
    }

    public static String convertToNoDashes(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
