/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.velocity.listener;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.listeners.SRProxyMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRProxyMessageEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProxyMessageListener implements EventHandler<PluginMessageEvent> {
    private final SRProxyMessageAdapter adapter;

    @Override
    public void execute(PluginMessageEvent event) {
        adapter.handlePluginMessage(wrap(event));
    }

    private SRProxyMessageEvent wrap(PluginMessageEvent event) {
        return new SRProxyMessageEvent() {
            @Override
            public boolean isCancelled() {
                return !event.getResult().isAllowed();
            }

            @Override
            public void setCancelled(boolean cancelled) {
                event.setResult(PluginMessageEvent.ForwardResult.handled());
            }

            @Override
            public byte[] getData() {
                return event.getData();
            }

            @Override
            public boolean isServerConnection() {
                return event.getSource() instanceof ServerConnection;
            }

            @Override
            public String getChannel() {
                return event.getIdentifier().getId();
            }
        };
    }
}
