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
package net.skinsrestorer.sponge8;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.IProperty;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.server.storage.ServerWorldProperties;
import org.spongepowered.math.vector.Vector3d;

import java.util.Optional;

@RequiredArgsConstructor
public class SkinApplierSponge {
    private final SkinsRestorer plugin;

    protected void applySkin(ServerPlayer player, IProperty property) {
        setTexture(player.user(), property);

        Sponge.server().scheduler().executor(plugin.getContainer()).execute(() -> sendUpdate(player));
    }

    public void updateProfileSkin(User user, IProperty skin) {
        setTexture(user, skin);
    }

    private void setTexture(User user, IProperty property) {
        user.offer(Keys.UPDATE_GAME_PROFILE, true);
        user.offer(Keys.SKIN_PROFILE_PROPERTY, ProfileProperty.of(ProfileProperty.TEXTURES, property.getValue(), property.getSignature()));
    }

    private void sendUpdate(ServerPlayer receiver) {
        receiver.tabList().removeEntry(receiver.uniqueId());
        receiver.tabList().addEntry(TabListEntry.builder()
                .displayName(receiver.displayName().get())
                .latency(receiver.connection().latency())
                .list(receiver.tabList())
                .gameMode(receiver.gameMode().get())
                .profile(receiver.profile())
                .build());

        // Save position to teleport player back after respawn
        ServerLocation loc = receiver.serverLocation();
        Vector3d rotation = receiver.rotation();

        // Simulate respawn to see skin active
        for (ResourceKey w : plugin.getGame().server().worldManager().offlineWorldKeys()) {
            Optional<ServerWorldProperties> worldPropertiesOptional = plugin.getGame().server().worldManager().loadProperties(w).join();

            if (worldPropertiesOptional.isPresent()
                    && !worldPropertiesOptional.get().uniqueId().equals(receiver.world().uniqueId())) {
                ServerWorld world = Sponge.server().worldManager().loadWorld(w).join();
                receiver.setLocation(world.location(world.properties().spawnPosition()));
                receiver.setLocationAndRotation(loc, rotation);
                break;
            }
        }

        receiver.offer(Keys.VANISH_STATE, VanishState.vanished());
        plugin.getGame().server().scheduler().submit(
                        Task.builder().execute(() ->
                                receiver.offer(Keys.VANISH_STATE, VanishState.unvanished())
                        ).delay(Ticks.of(1)).plugin(plugin.getContainer()).build());
    }
}
