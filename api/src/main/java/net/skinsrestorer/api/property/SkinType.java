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

public enum SkinType {
    /**
     * Skin linked to a player by uuid
     */
    PLAYER,
    /**
     * Skin linked to an url, can not update
     */
    URL,
    /**
     * Skin linked to a custom value and signature
     */
    CUSTOM,
    /**
     * Skin linked to an old value and signature from pre-v15 versions
     * DO NOT USE THIS TYPE, IT IS ONLY FOR COMPATIBILITY
     */
    LEGACY
}
