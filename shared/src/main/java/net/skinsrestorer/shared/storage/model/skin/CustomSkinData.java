/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.storage.model.skin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Locale;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomSkinData {
    private final String skinName;
    private final SkinProperty property;

    public static CustomSkinData of(String skinName, SkinProperty property) {
        return new CustomSkinData(sanitizeCustomSkinName(skinName), property);
    }

    public static String sanitizeCustomSkinName(String skinName) {
        return skinName.toLowerCase(Locale.ENGLISH);
    }
}
