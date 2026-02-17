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
package net.skinsrestorer.shared.storage;

import net.skinsrestorer.api.property.SkinType;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a parsed skin input string that may include an explicit type prefix.
 * <p>
 * Supported prefixes:
 * <ul>
 *     <li>{@code player:<name>} — force player skin lookup</li>
 *     <li>{@code custom:<name>} — force custom skin lookup</li>
 *     <li>{@code <name>} (no prefix) — default behavior (custom first, then player)</li>
 * </ul>
 *
 * @param name     The skin name with the prefix stripped.
 * @param typeHint The explicit skin type, or null for default lookup order.
 */
public record SkinInput(String name, @Nullable SkinType typeHint) {
    private static final String PLAYER_PREFIX = "player:";
    private static final String CUSTOM_PREFIX = "custom:";

    /**
     * Parses a raw skin input string, extracting an optional type prefix.
     *
     * @param input The raw input (e.g. "S0yoneyro", "player:S0yoneyro", "custom:myskin").
     * @return A SkinInput with the prefix stripped and the type hint set.
     */
    public static SkinInput parse(String input) {
        if (input.startsWith(PLAYER_PREFIX)) {
            return new SkinInput(input.substring(PLAYER_PREFIX.length()), SkinType.PLAYER);
        } else if (input.startsWith(CUSTOM_PREFIX)) {
            return new SkinInput(input.substring(CUSTOM_PREFIX.length()), SkinType.CUSTOM);
        }
        return new SkinInput(input, null);
    }
}
