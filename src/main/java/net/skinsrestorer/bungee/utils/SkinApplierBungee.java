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
package net.skinsrestorer.bungee.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.interfaces.SRApplier;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.SRLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SkinApplierBungee implements SRApplier {
    private final SkinsRestorer plugin;
    private final SRLogger log;
    private static Class<?> loginResult;

    public SkinApplierBungee(SkinsRestorer plugin) {
        this.plugin = plugin;
        this.log = plugin.getSrLogger();
    }

    public void applySkin(final ProxiedPlayer p, final String nick, InitialHandler handler) throws Exception {
        if (p == null && handler == null)
            return;

        if (p != null) {
            handler = (InitialHandler) p.getPendingConnection();
        }

        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(nick, false);

        if (handler.isOnlineMode() && p != null) {
            sendUpdateRequest(p, textures);
            return;
        }

        LoginResult profile = handler.getLoginProfile();

        if (profile == null) {
            try {
                // NEW BUNGEECORD (id, name, property)
                profile = new LoginResult(null, null, new Property[]{textures});
            } catch (Exception error) {
                // FALL BACK TO OLD (id, property)
                profile = (LoginResult) ReflectionUtil.invokeConstructor(loginResult,
                        new Class<?>[]{String.class, Property[].class},
                        null, new Property[]{textures});
            }
        }

        Property[] newProps = new Property[]{textures};

        profile.setProperties(newProps);
        ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile", profile);

        if (SkinsRestorer.getInstance().isMultiBungee()) {
            if (p != null)
                sendUpdateRequest(p, textures);
        } else {
            if (p != null)
                sendUpdateRequest(p, null);
        }
    }

    public void applySkin(final PlayerWrapper p, SkinsRestorerAPI api) throws Exception {
        applySkin(p.get(ProxiedPlayer.class), p.get(ProxiedPlayer.class).getName(), null);
    }

    public static void init() {
        try {
            loginResult = ReflectionUtil.getBungeeClass("connection", "LoginResult");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUpdateRequest(ProxiedPlayer p, Property textures) {
        if (p == null)
            return;

        if (p.getServer() == null)
            return;

        log.log("Sending skin update request for " + p.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdate");

            if (textures != null) {
                out.writeUTF(textures.getName());
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            p.getServer().sendData("sr:skinchange", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
