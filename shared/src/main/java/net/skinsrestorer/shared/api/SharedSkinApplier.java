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
package net.skinsrestorer.shared.api;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.SRConstants;

import java.util.Optional;

@RequiredArgsConstructor
public class SharedSkinApplier<P> implements SkinApplier<P> {
    private final Class<P> playerClass;
    private final SkinApplierAccess<P> access;
    private final PlatformWrapper<P> wrapper;
    private final PlayerStorage playerStorage;
    private final SkinStorage skinStorage;

    public boolean accepts(Class<?> playerClass) {
        return this.playerClass.isAssignableFrom(playerClass);
    }

    @Override
    public void applySkin(P player) throws DataRequestException {
        SRPlayer srPlayer = wrapper.convert(player);
        Optional<SkinProperty> playerSkin = playerStorage.getSkinForPlayer(srPlayer.getUniqueId(), srPlayer.getName());
        applySkin(player, playerSkin.orElse(SRConstants.EMPTY_SKIN));
    }

    @Override
    public void applySkin(P player, SkinIdentifier identifier) throws DataRequestException {
        skinStorage.getSkinDataByIdentifier(identifier).ifPresent(property -> applySkin(player, property));
    }

    @Override
    public void applySkin(P player, SkinProperty property) {
        access.applySkin(player, property);
    }
}
