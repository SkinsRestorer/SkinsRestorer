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
package net.skinsrestorer.bungee.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.log.SRLogger;

@RequiredArgsConstructor
public class LoginListener implements Listener {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent event) {
        if (event.isCancelled() && Config.NO_SKIN_IF_LOGIN_CANCELED)
            return;

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        event.registerIntent(plugin);

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            final PendingConnection connection = event.getConnection();
            final String skin = plugin.getSkinStorage().getDefaultSkinName(connection.getName());

            try {
                // TODO: add default skinurl support
                plugin.getSkinApplierBungee().applySkin(skin, (InitialHandler) connection);
            } catch (SkinRequestException ignored) {
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            event.completeIntent(plugin);
        });
    }

    //think we should no have EventPriority.HIGH just to check for updates...
    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnect(final ServerConnectEvent e) {
        if (e.isCancelled())
            return;

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            if (plugin.isOutdated()) {
                final ProxiedPlayer player = e.getPlayer();

                if (player.hasPermission("skinsrestorer.admincommand"))
                    player.sendMessage(TextComponent.fromLegacyText(Locale.OUTDATED));
            }
        });
    }
}
