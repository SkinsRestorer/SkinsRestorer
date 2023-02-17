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
import net.skinsrestorer.api.util.Pair;

import java.util.Optional;

/**
 * SkinStorage
 * <p>
 * Skin name: A name assigned to a skin property. Cached in a .skin file with a timestamp for expiry.
 * Player skin: Stored as a skin name in a .player file.
 */
public interface SkinStorage {
    /**
     * This method seeks out the skin that would be set on join and returns
     * the property containing all the skin data.
     * That skin can either be custom set, the premium skin or a default skin.
     * It also executes a skin data update if the saved skin data expired.
     *
     * @param playerName Player name to search a skin for
     * @return The skin property containing the skin data and on the right whether it's custom set
     * @throws DataRequestException If MojangAPI lookup errors (e.g. premium player not found)
     */
    Optional<SkinProperty> getSkinOfPlayer(String playerName) throws DataRequestException;

    /**
     * Returns a players custom skin.
     *
     * @param playerName the players name
     * @return the custom skin name a player has set or null if not set
     */
    Optional<String> getSkinNameOfPlayer(String playerName);

    /**
     * Saves custom player's skin name to database
     *
     * @param playerName Players name
     * @param skinName   Skin name
     */
    void setSkinNameOfPlayer(String playerName, String skinName);

    /**
     * Removes custom players skin name from database
     *
     * @param playerName - Players name
     */
    void removeSkinNameOfPlayer(String playerName);

    Optional<SkinProperty> getDefaultSkinForPlayer(String playerName) throws DataRequestException;

    /**
     * This method returns the skin data associated to the skin name.
     * If the skin name is not found, it will try to get the skin data from Mojang.
     *
     * @param skinName Skin name to search for
     * @return The skin property containing the skin data
     * @throws DataRequestException If MojangAPI lookup errors (e.g. premium player not found)
     */
    Optional<SkinProperty> fetchSkinData(String skinName) throws DataRequestException;

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName       Skin name
     * @param updateOutdated Whether we update the skin if expired
     */
    Optional<SkinProperty> getSkinData(String skinName, boolean updateOutdated);

    /**
     * Timestamp is set to current time
     *
     * @see #setSkinData(String, SkinProperty, long)
     */
    default void setSkinData(String skinName, SkinProperty textures) {
        setSkinData(skinName, textures, System.currentTimeMillis());
    }

    /**
     * Saves skin data to database
     *
     * @param skinName  Skin name
     * @param textures  Property object
     * @param timestamp timestamp string in milliseconds
     */
    void setSkinData(String skinName, SkinProperty textures, long timestamp);
}
