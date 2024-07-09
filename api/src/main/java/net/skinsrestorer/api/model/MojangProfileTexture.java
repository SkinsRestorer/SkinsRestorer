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
package net.skinsrestorer.api.model;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class MojangProfileTexture {
    private static final Pattern URL_STRIP_PATTERN = Pattern.compile("^https?://textures\\.minecraft\\.net/texture/");
    private String url;
    private MojangProfileTextureMeta metadata;

    public String getTextureHash() {
        return URL_STRIP_PATTERN.matcher(url).replaceAll("");
    }

    /**
     * @deprecated Use {@link #getTextureHash()} instead.
     */
    @Deprecated(forRemoval = true)
    public String getStrippedUrl() {
        return getTextureHash();
    }
}
