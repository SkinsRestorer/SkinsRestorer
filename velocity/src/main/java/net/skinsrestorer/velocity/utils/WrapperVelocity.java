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
package net.skinsrestorer.velocity.utils;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRProxyPlayer;

import java.util.Optional;

public class WrapperVelocity {
    public static ISRCommandSender wrapCommandSender(CommandSource sender) {
        return new ISRCommandSender() {
            @Override
            public void sendMessage(String message) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            }

            @Override
            public String getName() {
                return getSenderName(sender);
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public boolean isConsole() {
                return sender instanceof ConsoleCommandSource;
            }
        };
    }

    public static ISRProxyPlayer wrapPlayer(Player player) {
        return new ISRProxyPlayer() {
            @Override
            public Optional<String> getCurrentServer() {
                return player.getCurrentServer().map(server -> server.getServerInfo().getName());
            }

            @Override
            public void sendDataToServer(String channel, byte[] data) {
                player.getCurrentServer().map(server ->
                        server.sendPluginMessage(MinecraftChannelIdentifier.from(channel), data));
            }

            @Override
            public PlayerWrapper getWrapper() {
                return new PlayerWrapper(player);
            }

            @Override
            public String getName() {
                return player.getUsername();
            }

            @Override
            public void sendMessage(String message) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            }

            @Override
            public boolean hasPermission(String permission) {
                return player.hasPermission(permission);
            }
        };
    }

    private static String getSenderName(CommandSource source) {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }
}
