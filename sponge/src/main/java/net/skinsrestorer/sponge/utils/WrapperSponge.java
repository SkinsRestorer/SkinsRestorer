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
package net.skinsrestorer.sponge.utils;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.interfaces.MessageKeyGetter;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import javax.inject.Inject;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WrapperSponge {
    private final SettingsManager settings;
    private final SkinsRestorerLocale locale;

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
                sender.sendMessage(Text.builder(message).build());
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

    public SRPlayer player(Player player) {
        return new SRPlayer() {
            @Override
            public Locale getLocale() {
                return player.getLocale();
            }

            @Override
            public <P> P getAs(Class<P> playerClass) {
                return playerClass.cast(player);
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
                player.sendMessage(Text.builder(message).build());
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
