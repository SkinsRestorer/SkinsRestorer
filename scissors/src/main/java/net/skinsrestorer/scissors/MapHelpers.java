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
package net.skinsrestorer.scissors;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapHelpers {
    private MapHelpers() {
    }

    public static <E extends Enum<E>, V> Map<E, V> allSetTo(Class<E> enumClass, V value) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(Function.identity(), e -> value));
    }

    public static <E extends Enum<E>, V> Map<E, V> mapped(Class<E> enumClass, Function<E, V> function) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(Function.identity(), function));
    }
}
