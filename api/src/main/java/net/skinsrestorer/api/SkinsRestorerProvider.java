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
package net.skinsrestorer.api;

import org.jetbrains.annotations.ApiStatus;

public class SkinsRestorerProvider {
    private static SkinsRestorer api;

    public static SkinsRestorer get() {
        if (SkinsRestorerProvider.api == null) {
            throw new IllegalStateException("SkinsRestorerAPI is not initialized yet!");
        }

        return SkinsRestorerProvider.api;
    }

    @ApiStatus.Internal
    public static void setApi(SkinsRestorer api) {
        if (api == null) {
            throw new IllegalArgumentException("This is a internal method! You may not set it to null!");
        }

        SkinsRestorerProvider.api = api;
    }
}
