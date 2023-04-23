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
package net.skinsrestorer.api.storage;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;

import java.util.Optional;
import java.util.UUID;

public interface CacheStorage {
    Optional<MojangSkinDataResult> getSkin(String playerName, boolean allowExpired) throws DataRequestException;

    /**
     * Gets the uuid of a Mojang player from the cache.
     * If the uuid is not found locally, it will try to get the uuid from Mojang.
     *
     * @param playerName Player name to search for
     * @return The uuid of the player or empty if no such player exists
     */
    Optional<UUID> getUUID(String playerName, boolean allowExpired) throws DataRequestException;
}
