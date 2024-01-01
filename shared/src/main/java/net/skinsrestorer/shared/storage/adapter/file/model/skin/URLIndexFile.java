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
package net.skinsrestorer.shared.storage.adapter.file.model.skin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.storage.model.skin.URLIndexData;

@Getter
@NoArgsConstructor
public class URLIndexFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private String url;
    private SkinVariant skinVariant;
    private int dataVersion;

    public static URLIndexFile fromURLIndexData(URLIndexData urlIndexData) {
        URLIndexFile urlSkinFile = new URLIndexFile();
        urlSkinFile.url = urlIndexData.getUrl();
        urlSkinFile.skinVariant = urlIndexData.getSkinVariant();
        urlSkinFile.dataVersion = CURRENT_DATA_VERSION;
        return urlSkinFile;
    }

    public URLIndexData toURLIndexData() {
        return URLIndexData.of(url, skinVariant);
    }
}
