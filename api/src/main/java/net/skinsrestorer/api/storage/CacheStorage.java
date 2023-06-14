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

/**
 * Caches Name -> UUID to avoid spamming Mojang's API and speed up skin fetching.
 * This does also cache whether a premium player exists or not.
 */
public interface CacheStorage {
    /**
     * Gets the skin data of a Mojang player from the cache.
     *
     * @param playerName   Player name to search for
     * @param allowExpired Allow expired data to be returned
     * @return The skin data of the player or empty if no such player exists
     * @throws DataRequestException If the data could not be retrieved
     */
    Optional<MojangSkinDataResult> getSkin(String playerName, boolean allowExpired) throws DataRequestException;

    /**
     * Gets the UUID of a Mojang player from the cache.
     * If the UUID is not found locally, it will try to get the UUID from one of our data providers.
     *
     * @param playerName Player name to search for
     * @return The uuid of the player or empty if no such player exists
     */
    Optional<UUID> getUUID(String playerName, boolean allowExpired) throws DataRequestException;
}
