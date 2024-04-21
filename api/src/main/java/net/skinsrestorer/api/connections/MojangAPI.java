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
package net.skinsrestorer.api.connections;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Optional;
import java.util.UUID;

/**
 * Fetch Minecraft data from Mojang's API.
 * We use internal data providers to speed up the process and avoid spamming Mojang's API.
 * You have the option to fetch data from mojang:
 * - Get UUID and SkinProperty by a premium player name
 * - SkinProperty by UUID
 * - UUID by premium player name
 * This will not use any local cache, but one of our data providers may remotely cache the result data.
 */
public interface MojangAPI {
    /**
     * Get skin property by player name, this method will return empty if the player is not premium.
     * It may return a hardcoded skin value, for example for "Steve" or "Alex".
     *
     * @param nameOrUniqueId Can be a premium player username or unique id (dashed or non-dashed)
     * @return Skin or empty if the player is not premium
     * @throws DataRequestException If there was an error while getting the data
     */
    Optional<MojangSkinDataResult> getSkin(String nameOrUniqueId) throws DataRequestException;

    Optional<UUID> getUUID(String playerName) throws DataRequestException;

    Optional<SkinProperty> getProfile(UUID uuid) throws DataRequestException;
}
