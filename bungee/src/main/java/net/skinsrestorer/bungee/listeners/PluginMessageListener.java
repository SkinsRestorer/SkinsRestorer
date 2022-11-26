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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.skinsrestorer.bungee.SkinsRestorerBungee;
import net.skinsrestorer.shared.listeners.SRPluginMessageEvent;
import net.skinsrestorer.shared.listeners.SharedPluginMessageListener;

@Getter
@RequiredArgsConstructor
public class PluginMessageListener extends SharedPluginMessageListener implements Listener {
    private final SkinsRestorerBungee plugin;

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        handlePluginMessage(new SRPluginMessageEvent() {
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
            public boolean isServerConnection() {
                return event.getSender() instanceof ServerConnection;
            }

            @Override
            public String getTag() {
                return event.getTag();
            }
        });
    }
}
