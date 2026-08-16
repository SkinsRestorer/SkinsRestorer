/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleWire;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleHandshakeListener implements Listener {
    private final SettingsManager settings;

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnected(ServerConnectedEvent event) {
        if (!settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)) {
            return;
        }

        event.getPlayer().sendData(SkinShuffleChannels.HANDSHAKE, SkinShuffleWire.createHandshakePayload());
    }
}
