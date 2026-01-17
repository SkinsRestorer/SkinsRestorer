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
package net.skinsrestorer.mod.utils;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.sound.SoundParser;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SoundUtil implements SoundProvider {
    private final SettingsManager settings;
    private final SRLogger logger;

    @Override
    public void playSound(SRPlayer player) {
        if (!settings.getProperty(ServerConfig.SOUND_ENABLED)) {
            return;
        }

        ServerPlayer p = player.getAs(ServerPlayer.class);
        String sound = settings.getProperty(ServerConfig.SOUND_VALUE);
        SoundParser.Record record = SoundParser.parse(sound);
        if (record == null) {
            logger.warning("Invalid sound value in config: %s".formatted(sound));
            return;
        }

        logger.debug("Playing sound for player: %s".formatted(player.getName()));
        Holder<SoundEvent> soundEvent = BuiltInRegistries.SOUND_EVENT.stream()
                .filter(soundEvent1 -> soundEvent1.location().getPath().replace(".", "_").equalsIgnoreCase(record.getSound()))
                .map(BuiltInRegistries.SOUND_EVENT::wrapAsHolder)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid sound: " + record.getSound()));
        SoundSource source = SoundSource.valueOf(record.getCategory());

        p.connection.send(new ClientboundSoundPacket(Objects.requireNonNull(soundEvent), source, p.getX(), p.getY(), p.getZ(), record.getVolume(), record.getPitch(), p.getRandom().nextLong()));
    }
}
