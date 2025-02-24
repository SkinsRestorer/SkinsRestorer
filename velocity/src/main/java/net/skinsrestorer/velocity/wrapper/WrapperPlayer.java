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
package net.skinsrestorer.velocity.wrapper;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.utils.SRHelpers;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@SuperBuilder
public class WrapperPlayer extends WrapperCommandSender implements SRProxyPlayer {
    private static final ChannelIdentifier MESSAGE_CHANNEL = MinecraftChannelIdentifier.from(SRHelpers.MESSAGE_CHANNEL);
    private final @NonNull Player player;

    @Override
    public Locale getLocale() {
        if (!settings.getProperty(MessageConfig.PER_ISSUER_LOCALE)) {
            return settings.getProperty(MessageConfig.LOCALE);
        }

        return Objects.requireNonNullElseGet(player.getEffectiveLocale(),
                () -> settings.getProperty(MessageConfig.LOCALE));
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public Optional<String> getCurrentServer() {
        return player.getCurrentServer().map(server -> server.getServerInfo().getName());
    }

    @Override
    public void sendToMessageChannel(byte[] data) {
        if (!player.isActive()) {
            return;
        }

        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }

        serverConnection.get().sendPluginMessage(MESSAGE_CHANNEL, data);
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public boolean canSee(SRPlayer player) {
        return true;
    }
}
