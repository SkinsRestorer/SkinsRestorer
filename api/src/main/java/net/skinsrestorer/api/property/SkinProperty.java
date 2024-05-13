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
package net.skinsrestorer.api.property;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Easy way of interacting with properties across multiple platforms.
 */
@Data
@RequiredArgsConstructor(staticName = "of")
public class SkinProperty {
    public static final String TEXTURES_NAME = "textures";
    @NonNull
    private final String value;
    @NonNull
    private final String signature;

    @ApiStatus.Internal
    public static Optional<SkinProperty> tryParse(String name, String value, String signature) {
        if (!TEXTURES_NAME.equals(name) || value == null || signature == null || value.isEmpty() || signature.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SkinProperty(value, signature));
    }
}
