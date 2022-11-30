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
package net.skinsrestorer.bukkit.utils;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.LocaleParser;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class WrapperBukkit {
    @Inject
    private SettingsManager settings;
    @Inject
    private SkinsRestorerLocale locale;

    public ISRCommandSender commandSender(CommandSender sender) {
        if (sender instanceof Player) {
            return player((Player) sender);
        }

        return new ISRCommandSender() {
            @Override
            public Locale getLocale() {
                return settings.getProperty(Config.LANGUAGE);
            }

            @Override
            public void sendMessage(String message) {
                sender.sendMessage(message);
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

            @Override
            public boolean isConsole() {
                return sender instanceof ConsoleCommandSender;
            }
        };
    }

    public ISRPlayer player(Player player) {
        return new ISRPlayer() {
            @Override
            public Locale getLocale() {
                return LocaleParser.parseLocale(player.getLocale()).orElseGet(() -> settings.getProperty(Config.LANGUAGE));
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
            public boolean hasPermission(String permission) {
                return player.hasPermission(permission);
            }

            @Override
            public void sendMessage(String message) {
                player.sendMessage(message);
            }

            @Override
            public void sendMessage(MessageKeyGetter key, Object... args) {
                sendMessage(locale.getMessage(this, key, args));
            }
        };
    }
}
