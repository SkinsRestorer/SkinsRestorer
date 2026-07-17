/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand.wrapper;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.visibility.VanishService;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.skinsrestorer.fand.SRFandAdapter;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.utils.LocaleParser;

import java.util.Locale;
import java.util.UUID;

@SuperBuilder
public class WrapperPlayer extends WrapperCommandSender implements SRServerPlayer {
    private final @NonNull Player player;
    private final @NonNull SRFandAdapter adapter;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        if (senderClass.isAssignableFrom(Player.class)) {
            return senderClass.cast(player);
        }
        return super.getAs(senderClass);
    }

    @Override
    public Locale getLocale() {
        if (!settings.getProperty(MessageConfig.PER_ISSUER_LOCALE)) {
            return settings.getProperty(MessageConfig.LOCALE);
        }
        return LocaleParser.parseLocale(player.clientSettings().locale())
                .orElseGet(() -> settings.getProperty(MessageConfig.LOCALE));
    }

    @Override
    public UUID getUniqueId() {
        return player.uniqueId();
    }

    @Override
    public String getName() {
        return player.name();
    }

    @Override
    public boolean canSee(SRPlayer other) {
        Player otherPlayer = other.getAs(Player.class);
        return Fand.server().services().service(VanishService.class)
                .map(vanish -> vanish.canSee(player, otherPlayer))
                .orElseGet(() -> otherPlayer.visibleInPlayerList(player));
    }

    @Override
    public void closeInventory() {
        player.closeInventory();
    }

    @Override
    public void sendToMessageChannel(byte[] data) {
        adapter.sendPluginMessage(player, data);
    }
}
