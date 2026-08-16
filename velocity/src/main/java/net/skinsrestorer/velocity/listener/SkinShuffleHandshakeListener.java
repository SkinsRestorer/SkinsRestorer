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
package net.skinsrestorer.velocity.listener;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleWire;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleHandshakeListener {
    private static final MinecraftChannelIdentifier HANDSHAKE_CHANNEL = MinecraftChannelIdentifier.from(SkinShuffleChannels.HANDSHAKE);
    private final SettingsManager settings;

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnected(ServerConnectedEvent event) {
        if (!settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)) {
            return;
        }

        event.getPlayer().sendPluginMessage(HANDSHAKE_CHANNEL, SkinShuffleWire.createHandshakePayload());
    }
}
