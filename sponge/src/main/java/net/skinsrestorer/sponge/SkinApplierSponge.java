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
package net.skinsrestorer.sponge;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.property.ProfileProperty;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierSponge implements SkinApplierAccess<ServerPlayer> {
    @Override
    public void applySkin(ServerPlayer player, SkinProperty property) {
        player.offer(Keys.UPDATE_GAME_PROFILE, true);
        player.offer(Keys.SKIN_PROFILE_PROPERTY, ProfileProperty.of(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
    }
}
