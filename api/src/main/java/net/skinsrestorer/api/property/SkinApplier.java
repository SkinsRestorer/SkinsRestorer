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

import net.skinsrestorer.api.exception.DataRequestException;

/**
 * Applies skins to players.
 *
 * @param <P> Player type of the server implementation
 */
public interface SkinApplier<P> {
    /**
     * Applies a skin to a player that would be set on join.
     *
     * @param player Player to apply the skin to.
     * @throws DataRequestException If the skin data could not be requested.
     */
    void applySkin(P player) throws DataRequestException;

    /**
     * Applies a skin to a player from a skin identifier.
     *
     * @param player     Player to apply the skin to.
     * @param identifier SkinIdentifier to apply
     */
    void applySkin(P player, SkinIdentifier identifier);

    /**
     * Applies a raw skin property to a player.
     * You can use this to apply custom values to a player without needing to touch the storage.
     *
     * @param player   Player to apply the skin to.
     * @param property Skin property to apply
     */
    void applySkin(P player, SkinProperty property);
}
