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

import net.skinsrestorer.shared.gui.CodecHelpers;
import net.skinsrestorer.shared.gui.NetworkCodec;

/**
 * Represents a json string that can safely be shared across platforms.
 * This prevents coding errors like passing a json string to a component that expects a legacy string.
 *
 * @param jsonString The json string.
 */
public record ComponentString(String jsonString) {
    public static final NetworkCodec<ComponentString> CODEC = CodecHelpers.STRING_CODEC.map(ComponentString::jsonString, ComponentString::new);
}
