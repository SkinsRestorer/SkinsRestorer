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
package net.skinsrestorer.shared.storage.adapter.file.model.skin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;

@Getter
@NoArgsConstructor
public class CustomSkinFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private String skinName;
    private String value;
    private String signature;
    private int dataVersion;

    public static CustomSkinFile fromCustomSkinData(CustomSkinData customSkinData) {
        CustomSkinFile customSkinFile = new CustomSkinFile();
        customSkinFile.skinName = customSkinData.getSkinName();
        customSkinFile.value = customSkinData.getProperty().getValue();
        customSkinFile.signature = customSkinData.getProperty().getSignature();
        customSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return customSkinFile;
    }

    public CustomSkinData toCustomSkinData() {
        return CustomSkinData.of(skinName, SkinProperty.of(value, signature));
    }
}
