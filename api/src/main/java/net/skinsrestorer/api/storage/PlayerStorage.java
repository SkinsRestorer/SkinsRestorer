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
package net.skinsrestorer.api.storage;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Optional;
import java.util.UUID;

public interface PlayerStorage {
    /**
     * Get the custom set skin of a player.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     */
    Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid);

    /**
     * Saves players skin identifier to the database
     *
     * @param uuid       Players UUID
     * @param identifier Skin identifier
     */
    void setSkinIdOfPlayer(UUID uuid, SkinIdentifier identifier);

    /**
     * Removes players skin identifier from the database
     *
     * @param uuid Players UUID
     */
    void removeSkinIdOfPlayer(UUID uuid);

    /**
     * Gets the optional set skin identifier of a player and then returns the skin data.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     */
    Optional<SkinProperty> getSkinOfPlayer(UUID uuid) throws DataRequestException;

    /**
     * Get the skin a player would get if there was no skin set for them.
     *
     * @param uuid Players UUID
     * @return The identifier of the default skin
     */
    Optional<SkinProperty> getDefaultSkinForPlayer(UUID uuid, String playerName);

    /**
     * This method seeks out the skin that would be set on join and returns
     * the property containing all the skin data.
     * That skin can either be custom set, the premium skin or a default skin.
     * It also executes a skin data update if the saved skin data expired.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the skin that would be set on join
     * @throws DataRequestException If MojangAPI lookup errors (e.g. mojang offline)
     */
    Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName) throws DataRequestException;
}
