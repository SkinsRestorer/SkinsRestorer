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
package net.skinsrestorer.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.GameProfile.Property;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.interfaces.SRApplier;
import net.skinsrestorer.shared.utils.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SkinApplierVelocity implements SRApplier {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public SkinApplierVelocity(SkinsRestorer plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void applySkin(PlayerWrapper player, SkinsRestorerAPI api) throws SkinRequestException {
        String skin = api.getSkinName(player.get(Player.class).getUsername());

        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);
        List<Property> oldProperties = player.get(Player.class).getGameProfileProperties();
        List<Property> newProperties = updatePropertiesSkin(oldProperties, textures);

        player.get(Player.class).setGameProfileProperties(newProperties);
        sendUpdateRequest(player.get(Player.class), textures);
    }

    public void applySkin(PlayerWrapper player, Property property) {
        player.get(Player.class).setGameProfileProperties(updatePropertiesSkin(player.get(Player.class).getGameProfileProperties(), property));
    }

    public GameProfile updateProfileSkin(GameProfile profile, String skin) throws SkinRequestException {
        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);

        List<Property> oldProperties = profile.getProperties();
        List<Property> newProperties = updatePropertiesSkin(oldProperties, textures);

        return new GameProfile(profile.getId(), profile.getName(), newProperties);
    }

    private List<Property> updatePropertiesSkin(List<Property> original, Property property) {
        List<Property> properties = new ArrayList<>(original);
        boolean applied = false;

        for (int i = 0; i < properties.size(); i++) {
            Property lProperty = properties.get(i);

            if ("textures".equals(lProperty.getName())) {
                properties.set(i, property);
                applied = true;
            }
        }

        if (!applied)
            properties.add(property);

        return properties;
    }

    private void sendUpdateRequest(Player p, Property textures) {
        p.getCurrentServer().ifPresent(serverConnection -> {
            log.log("Sending skin update request for " + p.getUsername());

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            try {
                out.writeUTF("SkinUpdate");

                if (textures != null) {
                    out.writeUTF(textures.getName());
                    out.writeUTF(textures.getValue());
                    out.writeUTF(textures.getSignature());
                }

                serverConnection.sendPluginMessage(MinecraftChannelIdentifier.create("sr", "skinchange"), b.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
