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

import com.flowpowered.math.vector.Vector3d;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.property.SkinProperty;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import javax.inject.Inject;
import java.util.Collection;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierSponge implements SkinApplier<Player> {
    private final SRSpongeAdapter plugin;
    private final Game game;

    @Override
    public void applySkin(Player player, SkinProperty property) {
        setTexture(property, player.getProfile().getPropertyMap().get(SkinProperty.TEXTURES_NAME));

        plugin.runSync(() -> sendUpdate(player));
    }

    public void updateProfileSkin(GameProfile profile, SkinProperty skin) {
        setTexture(skin, profile.getPropertyMap().get(SkinProperty.TEXTURES_NAME));
    }

    private void setTexture(SkinProperty property, Collection<ProfileProperty> oldProperties) {
        ProfileProperty newTextures = game.getServer().getGameProfileManager().createProfileProperty(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature());
        oldProperties.removeIf(property2 -> property2.getName().equals(SkinProperty.TEXTURES_NAME));
        oldProperties.add(newTextures);
    }

    private void sendUpdate(Player receiver) {
        receiver.getTabList().removeEntry(receiver.getUniqueId());
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
            if (!w.getUniqueId().equals(receiver.getWorld().getUniqueId())) {
                game.getServer().loadWorld(w.getUniqueId());
                game.getServer().getWorld(w.getUniqueId()).ifPresent(value -> receiver.setLocation(value.getSpawnLocation()));
                receiver.setLocationAndRotation(loc, rotation);
                break;
            }
        }

        receiver.offer(Keys.VANISH, true);
        game.getScheduler().createTaskBuilder().execute(() -> receiver.offer(Keys.VANISH, false)).delayTicks(1).submit(plugin.getPluginInstance());
    }
}
