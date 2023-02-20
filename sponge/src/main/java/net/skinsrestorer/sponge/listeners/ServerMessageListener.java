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
package net.skinsrestorer.sponge.listeners;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.listeners.SRServerMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.spongepowered.api.network.EngineConnection;
import org.spongepowered.api.network.ServerPlayerConnection;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerMessageListener implements RawPlayDataHandler<EngineConnection> {
    private final SRServerMessageAdapter adapter;
    private final WrapperSponge wrapper;

    @Override
    public void handlePayload(ChannelBuf data, EngineConnection connection) {
        if (!(connection instanceof ServerPlayerConnection)) {
            return;
        }

        adapter.handlePluginMessage(wrap(data, (ServerPlayerConnection) connection));
    }

    private SRServerMessageEvent wrap(ChannelBuf data, ServerPlayerConnection player) {
        return new SRServerMessageEvent() {
            @Override
            public SRPlayer getPlayer() {
                return wrapper.player(player.player());
            }

            @Override
            public byte[] getData() {
                return data.array();
            }

            @Override
            public String getChannel() {
                return "sr:messagechannel"; // TODO: Maybe do not hardcode this
            }
        };
    }
}
