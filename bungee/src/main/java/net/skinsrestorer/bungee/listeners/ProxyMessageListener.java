/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.bungee.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.listeners.SRProxyMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRProxyMessageEvent;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProxyMessageListener implements Listener {
    private final SRProxyMessageAdapter adapter;
    private final WrapperBungee wrapper;

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        adapter.handlePluginMessage(wrap(event));
    }

    private SRProxyMessageEvent wrap(PluginMessageEvent event) {
        return new SRProxyMessageEvent() {
            @Override
            public SRProxyPlayer getPlayer() {
                return wrapper.player((ProxiedPlayer) event.getReceiver());
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void setCancelled(boolean cancelled) {
                event.setCancelled(cancelled);
            }

            @Override
            public byte[] getData() {
                return event.getData();
            }

            @Override
            public boolean isSenderServerConnection() {
                return event.getSender() instanceof Server;
            }

            @Override
            public boolean isReceiverProxyPlayer() {
                return event.getReceiver() instanceof ProxiedPlayer;
            }

            @Override
            public String getChannel() {
                return event.getTag();
            }
        };
    }
}
