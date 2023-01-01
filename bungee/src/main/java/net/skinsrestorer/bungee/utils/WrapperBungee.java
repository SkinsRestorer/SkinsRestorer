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
package net.skinsrestorer.bungee.utils;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperBungee {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

    public ISRCommandSender commandSender(CommandSender sender) {
        if (sender instanceof ProxiedPlayer) {
            return player((ProxiedPlayer) sender);
        }

        return new ISRCommandSender() {
            @Override
            public Locale getLocale() {
                return settings.getProperty(Config.LANGUAGE);
            }

            @Override
            public void sendMessage(String message) {
                sender.sendMessage(TextComponent.fromLegacyText(message));
            }

            @Override
            public void sendMessage(MessageKeyGetter key, Object... args) {
                sendMessage(locale.getMessage(this, key, args));
            }

            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }
        };
    }

    public ISRProxyPlayer player(ProxiedPlayer player) {
        return new ISRProxyPlayer() {
            @Override
            public Locale getLocale() {
                Locale playerLocale = player.getLocale();
                if (playerLocale == null) {
                    return settings.getProperty(Config.LANGUAGE);
                }

                return playerLocale;
            }

            @Override
            public Optional<String> getCurrentServer() {
                return Optional.ofNullable(player.getServer()).map(server -> server.getInfo().getName());
            }

            @Override
            public void sendDataToServer(String channel, byte[] data) {
                player.getServer().sendData(channel, data);
            }

            @Override
            public PlayerWrapper getWrapper() {
                return new PlayerWrapper(player);
            }

            @Override
            public String getName() {
                return player.getName();
            }

            @Override
            public UUID getUniqueId() {
                return player.getUniqueId();
            }

            @Override
            public void sendMessage(String message) {
                player.sendMessage(TextComponent.fromLegacyText(message));
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
