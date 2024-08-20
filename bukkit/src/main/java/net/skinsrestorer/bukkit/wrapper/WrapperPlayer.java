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
package net.skinsrestorer.bukkit.wrapper;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.utils.LocaleParser;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

@SuperBuilder
public class WrapperPlayer extends WrapperCommandSender implements SRServerPlayer {
    private final @NonNull Player player;

    @Override
    public Locale getLocale() {
        if (!settings.getProperty(MessageConfig.PER_ISSUER_LOCALE)) {
            return settings.getProperty(MessageConfig.LOCALE);
        }

        try {
            return LocaleParser.parseLocale(player.getLocale())
                    .orElseGet(() -> settings.getProperty(MessageConfig.LOCALE));
        } catch (NoSuchMethodError ignored) {
            return settings.getProperty(MessageConfig.LOCALE);
        }
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean canSee(SRPlayer player) {
        return this.player.canSee(player.getAs(Player.class));
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
    }

    @Override
    public void sendToMessageChannel(byte[] data) {
        player.sendPluginMessage(adapter.getPluginInstance(), SRHelpers.MESSAGE_CHANNEL, data);
    }
}
