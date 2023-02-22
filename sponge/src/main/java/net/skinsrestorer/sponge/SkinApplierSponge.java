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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.VanishState;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierSponge implements SkinApplierAccess<ServerPlayer> {
    private final SRSpongeAdapter plugin;
    private final Game game;

    @SuppressWarnings("unchecked")
    @Override
    public void applySkin(ServerPlayer player, SkinProperty property) {
        try {
            GameProfile gameProfile = (GameProfile) ReflectionUtil.invokeMethod(player, "getGameProfile");
            gameProfile.getProperties().removeAll(SkinProperty.TEXTURES_NAME);
            gameProfile.getProperties().put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        plugin.runSync(() -> sendUpdate(player));
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
        for (ServerWorld serverWorld : game.server().worldManager().worlds()) {
            if (serverWorld.uniqueId().equals(receiver.world().uniqueId())) {
                continue;
            }

            receiver.setLocation(serverWorld.location(serverWorld.properties().spawnPosition()));
            receiver.setLocationAndRotation(loc, rotation);
            break;
        }

        receiver.offer(Keys.VANISH_STATE, VanishState.vanished());
        game.server().scheduler().executor(plugin.getPluginContainer()).execute(() -> receiver.offer(Keys.VANISH_STATE, VanishState.unvanished()));
    }
}
