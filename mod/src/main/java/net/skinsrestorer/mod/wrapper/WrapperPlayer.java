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
package net.skinsrestorer.mod.wrapper;

import dev.architectury.networking.NetworkManager;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.mod.SRModInit;
import net.skinsrestorer.shared.config.MessageConfig;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.utils.LocaleParser;

import java.util.Locale;
import java.util.UUID;

@SuperBuilder
public class WrapperPlayer extends WrapperCommandSender implements SRServerPlayer {
    private final @NonNull ServerPlayer player;

    @Override
    public <S> S getAs(Class<S> senderClass) {
        if (senderClass.isAssignableFrom(ServerPlayer.class)) {
            return senderClass.cast(player);
        }

        return super.getAs(senderClass);
    }

    @Override
    public Locale getLocale() {
        if (!settings.getProperty(MessageConfig.PER_ISSUER_LOCALE)) {
            return settings.getProperty(MessageConfig.LOCALE);
        }

        try {
            return LocaleParser.parseLocale(player.clientInformation().language())
                    .orElseGet(() -> settings.getProperty(MessageConfig.LOCALE));
        } catch (NoSuchMethodError ignored) {
            return settings.getProperty(MessageConfig.LOCALE);
        }
    }

    @Override
    public UUID getUniqueId() {
        return player.getGameProfile().id();
    }

    @Override
    public String getName() {
        return player.getGameProfile().name();
    }

    @Override
    public boolean canSee(SRPlayer player) {
        return true;
    }

    @Override
    public void closeInventory() {
        player.closeContainer();
    }

    @Override
    public void sendToMessageChannel(byte[] data) {
        NetworkManager.sendToPlayer(player, new SRModInit.RawBytePayload(data));
    }
}
