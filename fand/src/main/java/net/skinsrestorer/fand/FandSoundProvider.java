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
package net.skinsrestorer.fand;

import ch.jalu.configme.SettingsManager;
import io.fand.api.entity.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.sound.SoundParser;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;
import java.util.Locale;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class FandSoundProvider implements SoundProvider {
    private final SettingsManager settings;
    private final SRLogger logger;

    @Override
    public void playSound(SRPlayer player) {
        if (!settings.getProperty(ServerConfig.SOUND_ENABLED)) {
            return;
        }
        SoundParser.Record parsed = SoundParser.parse(settings.getProperty(ServerConfig.SOUND_VALUE));
        if (parsed == null) {
            return;
        }
        try {
            String soundName = parsed.getSound().toLowerCase(Locale.ROOT).replace('_', '.');
            if (!soundName.contains(":")) {
                soundName = "minecraft:" + soundName;
            }
            Sound effect = Sound.sound()
                    .type(Key.key(soundName))
                    .source(source(parsed.getCategory()))
                    .volume(parsed.getVolume())
                    .pitch(parsed.getPitch())
                    .seed(parsed.generateSeed())
                    .build();
            player.getAs(Player.class).playSound(effect);
        } catch (IllegalArgumentException invalidSound) {
            logger.warning("Invalid sound value in config: %s".formatted(
                    settings.getProperty(ServerConfig.SOUND_VALUE)), invalidSound);
        }
    }

    private static Sound.Source source(String category) {
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "MASTER" -> Sound.Source.MASTER;
            case "MUSIC" -> Sound.Source.MUSIC;
            case "RECORD", "RECORDS" -> Sound.Source.RECORD;
            case "WEATHER" -> Sound.Source.WEATHER;
            case "BLOCK", "BLOCKS" -> Sound.Source.BLOCK;
            case "HOSTILE" -> Sound.Source.HOSTILE;
            case "NEUTRAL" -> Sound.Source.NEUTRAL;
            case "PLAYER", "PLAYERS" -> Sound.Source.PLAYER;
            case "AMBIENT" -> Sound.Source.AMBIENT;
            case "VOICE" -> Sound.Source.VOICE;
            case "UI" -> Sound.Source.UI;
            default -> throw new IllegalArgumentException("Unknown sound category: " + category);
        };
    }
}
