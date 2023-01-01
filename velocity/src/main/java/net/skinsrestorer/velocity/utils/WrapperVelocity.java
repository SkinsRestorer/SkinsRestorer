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

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperVelocity {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

    private static String getSenderName(CommandSource source) {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }

    public SRCommandSender commandSender(CommandSource sender) {
        if (sender instanceof Player) {
            return player((Player) sender);
        }

        return new SRCommandSender() {
            @Override
            public Locale getLocale() {
                return settings.getProperty(Config.LANGUAGE);
            }

            @Override
            public void sendMessage(String message) {
                sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            }

            @Override
            public void sendMessage(MessageKeyGetter key, Object... args) {
                sendMessage(locale.getMessage(this, key, args));
            }

            @Override
            public String getName() {
                return getSenderName(sender);
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }
        };
    }

    public SRProxyPlayer player(Player player) {
        return new SRProxyPlayer() {
            @Override
            public Locale getLocale() {
                Locale playerLocale = player.getEffectiveLocale();
                if (playerLocale == null) {
                    return settings.getProperty(Config.LANGUAGE);
                }

                return playerLocale;
            }

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
            public UUID getUniqueId() {
                return player.getUniqueId();
            }

            @Override
            public void sendMessage(String message) {
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
            }

            @Override
            public void sendMessage(MessageKeyGetter key, Object... args) {
                sendMessage(locale.getMessage(this, key, args));
            }

            @Override
            public boolean hasPermission(String permission) {
                return player.hasPermission(permission);
            }
        };
    }
}
