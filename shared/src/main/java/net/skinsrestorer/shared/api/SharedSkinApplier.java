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
package net.skinsrestorer.shared.api;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.property.SkinApplier;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRSubjectWrapper;
import net.skinsrestorer.shared.utils.SRHelpers;

import java.util.Optional;

@RequiredArgsConstructor
public class SharedSkinApplier<P> implements SkinApplier<P> {
    private final Class<P> playerClass;
    private final SkinApplierAccess<P> access;
    private final SRSubjectWrapper<?, P, ?> wrapper;
    private final PlayerStorage playerStorage;
    private final SkinStorage skinStorage;
    private final Injector injector;

    public boolean accepts(Class<?> playerClass) {
        return this.playerClass.isAssignableFrom(playerClass);
    }

    @Override
    public void applySkin(P player) throws DataRequestException {
        SRPlayer srPlayer = wrapper.player(player);
        Optional<SkinProperty> playerSkin = playerStorage.getSkinForPlayer(srPlayer.getUniqueId(), srPlayer.getName());
        applySkin(player, playerSkin.orElse(SRHelpers.EMPTY_SKIN));
    }

    @Override
    public void applySkin(P player, SkinIdentifier identifier) {
        skinStorage.getSkinDataByIdentifier(identifier).ifPresent(property -> applySkin(player, property));
    }

    @Override
    public void applySkin(P player, SkinProperty property) {
        access.applySkin(player, property);

        SRPlayer srPlayer = wrapper.player(player);
        Optional.ofNullable(injector.getIfAvailable(SoundProvider.class))
                .ifPresent(soundProvider -> soundProvider.accept(injector, srPlayer));
    }
}
