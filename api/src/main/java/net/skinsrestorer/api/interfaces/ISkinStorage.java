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

import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;

import java.util.Optional;

/**
 * SkinStorage
 * <p>
 * Skin name: A name assigned to a skin property. Cached in a .skin file with a timestamp for expiry.
 * Player skin: Stored as a skin name in a .player file.
 */
public interface ISkinStorage {
    /**
     * Returns a players custom skin.
     *
     * @param playerName the players name
     * @return the custom skin name a player has set or null if not set
     */
    Optional<String> getSkinOfPlayer(String playerName);

    /**
     * This method seeks out a players actual skin (chosen or not) and returns
     * either null (if no skin data found) or the property containing all
     * the skin data.
     * It also schedules a skin update to stay up to date with skin changes.
     *
     * @param playerName Player name to search skin for
     * @throws SkinRequestException If MojangAPI lookup errors
     */
    IProperty getSkinForPlayer(String playerName) throws SkinRequestException;

    /**
     * Removes custom players skin name from database
     *
     * @param playerName - Players name
     */
    void removeSkinOfPlayer(String playerName);

    /**
     * Saves custom player's skin name to database
     *
     * @param playerName Players name
     * @param skinName   Skin name
     */
    void setSkinOfPlayer(String playerName, String skinName);

    /**
     * @see #getSkinData(String, boolean)
     */
    Optional<IProperty> getSkinData(String skinName);

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName       Skin name
     * @param updateOutdated On true, we update the skin if expired
     */
    Optional<IProperty> getSkinData(String skinName, boolean updateOutdated);

    /**
     * Timestamp is set to current time
     *
     * @see #setSkinData(String, IProperty, long)
     */
    void setSkinData(String skinName, IProperty textures);

    /**
     * Saves skin data to database
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp string in millis (null for current)
     */
    void setSkinData(String skinName, IProperty textures, long timestamp);
}
