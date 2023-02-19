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
package net.skinsrestorer.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.listeners.SRServerMessageAdapter;
import net.skinsrestorer.shared.listeners.SRServerMessageEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerMessageListener implements PluginMessageListener {
    private final SRServerMessageAdapter adapter;
    private final WrapperBukkit wrapper;

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        adapter.handlePluginMessage(wrap(channel, player, message));
    }

    private SRServerMessageEvent wrap(String channel, Player player, byte[] message) {
        return new SRServerMessageEvent() {
            @Override
            public String getTag() {
                return channel;
            }

            @Override
            public SRPlayer getPlayer() {
                return wrapper.player(player);
            }

            @Override
            public byte[] getData() {
                return message;
            }
        };
    }
}
