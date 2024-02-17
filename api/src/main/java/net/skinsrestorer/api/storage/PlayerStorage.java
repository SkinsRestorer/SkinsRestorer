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
package net.skinsrestorer.api.storage;

import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

import java.util.Optional;
import java.util.UUID;

/**
 * This interface is used to store and retrieve skin data for players.
 * <br/>
 * Internally a player is stored by their UUID and is linked to a skin identifier.
 */
public interface PlayerStorage {
    /**
     * Get the linked skin identifier of a player.
     *
     * @param uuid Players UUID
     * @return The skin identifier of the player
     */
    Optional<SkinIdentifier> getSkinIdOfPlayer(UUID uuid);

    /**
     * Links a player to a skin identifier.
     *
     * @param uuid       Players UUID
     * @param identifier Skin identifier to link
     */
    void setSkinIdOfPlayer(UUID uuid, SkinIdentifier identifier);

    /**
     * Removes the link between the player and a skin identifier.
     *
     * @param uuid Players UUID
     */
    void removeSkinIdOfPlayer(UUID uuid);

    /**
     * Calls {@link #getSkinIdOfPlayer(UUID)} and fetches the stored skin property from the skin identifier.
     *
     * @param uuid Players UUID
     * @return The skin property of the player or empty if no identifier is linked or data is missing.
     */
    Optional<SkinProperty> getSkinOfPlayer(UUID uuid);

    /**
     * This method seeks out the skin that would be set on join and returns
     * the property containing all the skin data (Value and Signature).
     * That skin can either be custom set, the premium skin or a default skin.
     * It also executes a skin data update if the saved skin data expired.
     * <p>
     * The isOnlineMode parameter is used to determine whether the player is
     * connected via online mode, so we may skip default skins for that player
     * if configured by the admin.
     *
     * @param uuid         Players UUID
     * @param playerName   Players name
     * @param isOnlineMode Whether the player gets properties from the platform already
     * @return The skin identifier of the skin that would be set on join
     */
    Optional<SkinIdentifier> getSkinIdForPlayer(UUID uuid, String playerName, boolean isOnlineMode) throws DataRequestException;

    /**
     * @see #getSkinIdForPlayer(UUID, String, boolean)
     */
    default Optional<SkinIdentifier> getSkinIdForPlayer(UUID uuid, String playerName) throws DataRequestException {
        return getSkinIdForPlayer(uuid, playerName, false);
    }

    /**
     * @see #getSkinIdForPlayer(UUID, String, boolean)
     */
    Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName, boolean isOnlineMode) throws DataRequestException;

    /**
     * @see #getSkinForPlayer(UUID, String, boolean)
     */
    default Optional<SkinProperty> getSkinForPlayer(UUID uuid, String playerName) throws DataRequestException {
        return getSkinForPlayer(uuid, playerName, false);
    }
}
