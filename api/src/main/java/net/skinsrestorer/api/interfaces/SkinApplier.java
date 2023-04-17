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
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;

public interface SkinApplier<P> {
    /**
     * Applies the player selected skin from the player table/file.
     * This is useful in combination with setSkinName.
     *
     * @param player Player to apply the skin to.
     * @throws DataRequestException
     */
    void applySkin(P player) throws DataRequestException;

    /**
     * Only Apply the skinName from the skin table/file.
     * This will not keep the skin on rejoin / applySkin(playerWrapper).
     *
     * @param player   Player to apply the skin to.
     * @param identifier SkinIdentifier to apply
     * @throws DataRequestException
     */
    void applySkin(P player, SkinIdentifier identifier) throws DataRequestException;

    /**
     * Applies the skin In other words, sets the skin data, but no changes will
     * be visible until you reconnect or force update with.
     *
     * @param player   Player to apply the skin to.
     * @param property Skin property to apply
     */
    void applySkin(P player, SkinProperty property);
}
