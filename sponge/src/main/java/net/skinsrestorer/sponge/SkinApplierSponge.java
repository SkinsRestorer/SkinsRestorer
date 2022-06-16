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
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Collection;

@RequiredArgsConstructor
public class SkinApplierSponge {
    private final SkinsRestorer plugin;

    protected void applySkin(Player player, IProperty property) {
        setTexture(property, player.getProfile().getPropertyMap().get(IProperty.TEXTURES_NAME));

        Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> sendUpdate(player));
    }

    public void updateProfileSkin(GameProfile profile, String skin) throws SkinRequestException {
        setTexture(plugin.getSkinStorage().getSkinForPlayer(skin), profile.getPropertyMap().get(IProperty.TEXTURES_NAME));
    }

    private void setTexture(IProperty property, Collection<ProfileProperty> oldProperties) {
        ProfileProperty newTextures = Sponge.getServer().getGameProfileManager().createProfileProperty(IProperty.TEXTURES_NAME, property.getValue(), property.getSignature());
        oldProperties.removeIf(property2 -> property2.getName().equals(IProperty.TEXTURES_NAME));
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
        for (WorldProperties w : Sponge.getServer().getAllWorldProperties()) {
            if (!w.getUniqueId().equals(receiver.getWorld().getUniqueId())) {
                Sponge.getServer().loadWorld(w.getUniqueId());
                Sponge.getServer().getWorld(w.getUniqueId()).ifPresent(value -> receiver.setLocation(value.getSpawnLocation()));
                receiver.setLocationAndRotation(loc, rotation);
                break;
            }
        }

        receiver.offer(Keys.VANISH, true);
        Sponge.getScheduler().createTaskBuilder().execute(() -> receiver.offer(Keys.VANISH, false)).delayTicks(1).submit(plugin);
    }
}
