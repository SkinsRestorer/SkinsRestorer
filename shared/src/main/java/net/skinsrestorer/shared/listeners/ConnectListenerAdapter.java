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
package net.skinsrestorer.shared.listeners;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRProxyPlugin;
import net.skinsrestorer.shared.storage.Message;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class ConnectListenerAdapter {
    private final SRProxyPlugin plugin;

    public void handleConnect(SRServerConnectedEvent event) {
        plugin.runAsync(() -> {
            if (plugin.isOutdated()) {
                SRPlayer player = event.getPlayer();

                if (player.hasPermission("skinsrestorer.admincommand")) {
                    player.sendMessage(Message.OUTDATED);
                }
            }
        });
    }
}
