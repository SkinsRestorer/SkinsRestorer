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
package net.skinsrestorer.bungee;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.skinsrestorer.api.bungeecord.events.SkinApplyBungeeEvent;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.reflection.exception.ReflectionException;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public abstract class SkinApplierBungeeShared {
    private final ISRPlugin plugin;
    private final SRLogger log;

    public void applySkin(IProperty property, InitialHandler handler) {
        try {
            applyEvent(null, property, handler);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    protected void applySkin(ProxiedPlayer player, IProperty property) {
        try {
            applyEvent(player, property, (InitialHandler) player.getPendingConnection());
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    private void applyEvent(@Nullable ProxiedPlayer player, IProperty property, InitialHandler handler) throws ReflectionException {
        SkinApplyBungeeEvent event = new SkinApplyBungeeEvent(player, property);

        ProxyServer.getInstance().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        applyWithProperty(player, handler, event.getProperty());
    }

    private void applyWithProperty(@Nullable ProxiedPlayer player, InitialHandler handler, IProperty textures) throws ReflectionException {
        applyToHandler(handler, textures);

        if (player == null)
            return;

        sendUpdateRequest(player, Config.FORWARD_TEXTURES ? textures : null);
    }

    protected abstract void applyToHandler(InitialHandler handler, IProperty textures) throws ReflectionException;

    public abstract List<IProperty> getProperties(ProxiedPlayer player);

    private void sendUpdateRequest(@NotNull ProxiedPlayer player, IProperty textures) {
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
