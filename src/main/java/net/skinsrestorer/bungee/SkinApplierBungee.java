/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
package net.skinsrestorer.bungee;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.api.bungeecord.events.SkinApplyBungeeEvent;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.exception.ReflectionException;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@RequiredArgsConstructor
public class SkinApplierBungee {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public void applySkin(String nick, InitialHandler handler) throws SkinRequestException {
        try {
            applySkin(null, plugin.getSkinStorage().getSkinForPlayer(nick, false).get(), handler); // FIXME
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    protected void applySkin(ProxiedPlayer player, IProperty property) {
        try {
            applySkin(player, property, (InitialHandler) player.getPendingConnection());
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    private void applySkin(@Nullable ProxiedPlayer player, IProperty property, InitialHandler handler) throws ReflectionException {
        if (handler == null) {
            assert player != null;
            handler = (InitialHandler) player.getPendingConnection();
        }

        SkinApplyBungeeEvent event = new SkinApplyBungeeEvent(player, property);

        plugin.getProxy().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        applyWithProperty(player, handler, (Property) event.getProperty());
    }

    private void applyWithProperty(@Nullable ProxiedPlayer player, InitialHandler handler, Property textures) throws ReflectionException {
        applyToHandler(handler, textures);

        if (player == null)
            return;

        if (plugin.isMultiBungee()) {
            sendUpdateRequest(player, textures);
        } else {
            sendUpdateRequest(player, null);
        }
    }

    private void applyToHandler(InitialHandler handler, Property textures) throws ReflectionException {
        LoginResult profile = handler.getLoginProfile();
        Property[] newProps = new Property[]{textures};

        if (profile == null) {
            try {
                // NEW BUNGEECORD (id, name, property)
                profile = new LoginResult(null, null, newProps);
            } catch (Exception error) {
                // FALL BACK TO OLD (id, property)
                profile = (LoginResult) ReflectionUtil.invokeConstructor(LoginResult.class,
                        new Class<?>[]{String.class, Property[].class},
                        null, newProps);
            }
        } else {
            profile.setProperties(newProps);
        }

        ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile", profile);
    }

    private void sendUpdateRequest(@NotNull ProxiedPlayer player, Property textures) {
        if (player.getServer() == null)
            return;

        log.debug("Sending skin update request for " + player.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdate");

            if (textures != null) {
                out.writeUTF(textures.getName());
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            player.getServer().sendData("sr:skinchange", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
