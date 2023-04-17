/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.api.connections;

import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import org.jetbrains.annotations.Nullable;

public interface MineSkinAPI {
    /**
     * Generates a skin using the <a href="https://mineskin.org/">MineSkin</a> API
     * [WARNING] MineSkin api key might be REQUIRED in the future.
     *
     * @param url         pointing to a skin image url
     * @param skinVariant can be null, steve or slim
     * @return Custom skin property containing "value" and "signature"
     * @throws DataRequestException on error
     */
    MineSkinResponse genSkin(String url, @Nullable SkinVariant skinVariant) throws DataRequestException;
}
