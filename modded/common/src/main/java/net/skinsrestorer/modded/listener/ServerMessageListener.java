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
package net.skinsrestorer.modded.listener;

import dev.architectury.networking.NetworkManager;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.modded.SRModInit;
import net.skinsrestorer.modded.wrapper.WrapperMod;
import net.skinsrestorer.shared.listeners.SRServerMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.subjects.SRServerPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerMessageListener implements NetworkManager.NetworkReceiver<RegistryFriendlyByteBuf> {
    private final SRServerMessageAdapter adapter;
    private final WrapperMod wrapper;

    @Override
    public void receive(RegistryFriendlyByteBuf value, NetworkManager.PacketContext context) {
        adapter.handlePluginMessage(wrap(value, context));
    }

    private SRServerMessageEvent wrap(RegistryFriendlyByteBuf value, NetworkManager.PacketContext context) {
        byte[] message = new byte[value.readableBytes()];
        value.readBytes(message);

        return new SRServerMessageEvent() {
            @Override
            public SRServerPlayer getPlayer() {
                return wrapper.player((ServerPlayer) context.getPlayer());
            }

            @Override
            public byte[] getData() {
                return message;
            }

            @Override
            public String getChannel() {
                return SRModInit.SR_MESSAGE_CHANNEL.toString();
            }
        };
    }
}
