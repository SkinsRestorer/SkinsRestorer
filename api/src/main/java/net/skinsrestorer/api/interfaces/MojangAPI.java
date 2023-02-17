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
package net.skinsrestorer.api.interfaces;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Optional;

public interface MojangAPI {
    /**
     * Get skin property by player name, this method will return empty if the player is not premium.
     * It may return a hardcoded skin value, for example for "Steve" or "Alex".
     * It does in theory a name to uuid to profile request.
     * But internally it is faster than calling {@link #getUUID(String)} and {@link #getProfile(String)} separately.
     * That is because we use ashcon.app, and we can directly get the profile from the name.
     *
     * @param playerName Premium player username
     * @return Skin or empty if the player is not premium
     * @throws DataRequestException If there was an error while getting the data
     */
    Optional<SkinProperty> getSkin(String playerName) throws DataRequestException;

    Optional<String> getUUID(String playerName) throws DataRequestException;

    Optional<SkinProperty> getProfile(String uuid) throws DataRequestException;
}
