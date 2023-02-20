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
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.profile.property.ProfileProperty;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierSponge implements SkinApplierAccess<ServerPlayer> {
    private final SRSpongeAdapter plugin;
    private final Game game;

    @Override
    public void applySkin(ServerPlayer player, SkinProperty property) {
        player.offer(Keys.UPDATE_GAME_PROFILE, true);
        player.skinProfile().set(ProfileProperty.of(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));

        // plugin.runSync(() -> sendUpdate(player)); // TODO: May not be needed
    }

    /*
    private void sendUpdate(ServerPlayer receiver) {
        receiver.tabList().removeEntry(receiver.uniqueId());
        receiver.getTabList().addEntry(TabListEntry.builder()
                .displayName(receiver.getDisplayNameData().displayName().get())
                .latency(receiver.getConnection().getLatency())
                .list(receiver.getTabList())
                .gameMode(receiver.getGameModeData().type().get())
                .profile(receiver.getProfile())
                .build());

        // Save position to teleport player back after respawn
        Location<World> loc = receiver.getLocation();
        Vector3d rotation = receiver.getRotation();

        // Simulate respawn to see skin active
        for (WorldProperties w : game.getServer().getAllWorldProperties()) {
            if (!w.uniqueId().equals(receiver.getWorld().uniqueId())) {
                game.getServer().loadWorld(w.uniqueId());
                game.getServer().getWorld(w.uniqueId()).ifPresent(value -> receiver.setLocation(value.getSpawnLocation()));
                receiver.setLocationAndRotation(loc, rotation);
                break;
            }
        }

        receiver.offer(Keys.VANISH, true);
        game.getScheduler().createTaskBuilder().execute(() -> receiver.offer(Keys.VANISH, false)).delayTicks(1).submit(plugin.getPluginInstance());
    }*/
}
