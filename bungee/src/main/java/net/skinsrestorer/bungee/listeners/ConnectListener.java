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
package net.skinsrestorer.bungee.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.storage.Locale;

import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapPlayer;

@RequiredArgsConstructor
public class ConnectListener implements Listener {
    private final SkinsRestorer plugin;

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnect(final ServerConnectEvent event) {
        if (event.isCancelled())
            return;

        plugin.runAsync(() -> {
            if (plugin.isOutdated()) {
                ProxiedPlayer player = event.getPlayer();
                ISRProxyPlayer proxyPlayer = wrapPlayer(player);

                if (player.hasPermission("skinsrestorer.admincommand"))
                    proxyPlayer.sendMessage(Locale.OUTDATED);
            }
        });
    }
}
