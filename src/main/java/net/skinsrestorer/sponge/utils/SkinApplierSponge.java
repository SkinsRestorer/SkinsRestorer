/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.sponge.utils;

import com.flowpowered.math.vector.Vector3d;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.interfaces.ISRApplier;
import net.skinsrestorer.shared.utils.property.GenericProperty;
import net.skinsrestorer.sponge.SkinsRestorer;
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

public class SkinApplierSponge implements ISRApplier {
    private final SkinsRestorer plugin;
    private Player receiver;

    public SkinApplierSponge(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    public void applySkin(final PlayerWrapper player) throws SkinRequestException {
        applySkin(player, plugin.getSkinsRestorerAPI().getSkinName(player.get(Player.class).getName()), true);
    }

    public void updateProfileSkin(GameProfile profile, String skin) throws SkinRequestException {
        setTexture(skin, profile.getPropertyMap().get("textures"));
    }

    public void applySkin(final PlayerWrapper player, final String skin, boolean updatePlayer) throws SkinRequestException {
        setTexture(skin, player.get(Player.class).getProfile().getPropertyMap().get("textures"));

        if (!updatePlayer)
            return;

        Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> updatePlayerSkin(player.get(Player.class)));
    }

    private void setTexture(String skin, Collection<ProfileProperty> oldProperties) throws SkinRequestException {
        GenericProperty textures = (GenericProperty) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);
        ProfileProperty newTextures = Sponge.getServer().getGameProfileManager().createProfileProperty("textures", textures.getValue(), textures.getSignature());
        oldProperties.clear();
        oldProperties.add(newTextures);
    }

    public void updatePlayerSkin(Player p) {
        receiver = p;

        sendUpdate();
    }

    private void sendUpdate() {
        sendUpdateSelf();

        receiver.offer(Keys.VANISH, true);
        Sponge.getScheduler().createTaskBuilder().execute(() -> receiver.offer(Keys.VANISH, false)).delayTicks(1).submit(plugin);
    }

    private void sendUpdateSelf() {
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
    }
}
