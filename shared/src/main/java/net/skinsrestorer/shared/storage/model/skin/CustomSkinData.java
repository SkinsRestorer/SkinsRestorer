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
package net.skinsrestorer.shared.storage.model.skin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomSkinData {
    private final String skinName;
    private final @Nullable ComponentString displayName;
    private final SkinProperty property;

    public static CustomSkinData of(String skinName, @Nullable ComponentString displayName, SkinProperty property) {
        return new CustomSkinData(sanitizeCustomSkinName(skinName), displayName, property);
    }

    public static String sanitizeCustomSkinName(String skinName) {
        return skinName.toLowerCase(Locale.ROOT);
    }
}
