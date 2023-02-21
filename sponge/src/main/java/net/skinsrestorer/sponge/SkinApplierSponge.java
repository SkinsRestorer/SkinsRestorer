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
package net.skinsrestorer.sponge;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.property.ProfileProperty;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierSponge implements SkinApplierAccess<ServerPlayer> {
    private final SRSpongeAdapter plugin;
    private final Game game;

    @Override
    public void applySkin(ServerPlayer player, SkinProperty property) {
        player.offer(Keys.UPDATE_GAME_PROFILE, true);
        ProfileProperty profileProperty = ProfileProperty.of(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature());
        player.skinProfile().set(profileProperty);

        plugin.runSync(() -> sendUpdate(player, profileProperty));
    }

    private void sendUpdate(ServerPlayer receiver, ProfileProperty property) {
        receiver.tabList().removeEntry(receiver.uniqueId());
        receiver.tabList().addEntry(TabListEntry.builder()
                .displayName(receiver.displayName().get())
                .latency(receiver.connection().latency())
                .list(receiver.tabList())
                .gameMode(receiver.gameMode().get())
                .profile(receiver.profile().withProperty(property))
                .build());

        /*
        // Save position to teleport player back after respawn
        Location<World> loc = receiver.location();
        Vector3d rotation = receiver.rotation();

        // Simulate respawn to see skin active
        for (WorldProperties w : game.server().getAllWorldProperties()) {
            if (!w.uniqueId().equals(receiver.getWorld().uniqueId())) {
                game.getServer().loadWorld(w.uniqueId());
                game.getServer().getWorld(w.uniqueId()).ifPresent(value -> receiver.setLocation(value.getSpawnLocation()));
                receiver.setLocationAndRotation(loc, rotation);
                break;
            }
        }*/

        receiver.offer(Keys.VANISH_STATE, VanishState.vanished());
        game.server().scheduler().executor(plugin.getPluginContainer()).execute(() -> receiver.offer(Keys.VANISH_STATE, VanishState.unvanished()));
    }
}
