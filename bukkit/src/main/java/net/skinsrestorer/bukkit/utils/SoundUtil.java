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
package net.skinsrestorer.bukkit.utils;

import ch.jalu.configme.SettingsManager;
import com.cryptomorin.xseries.XSound;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.sound.SoundParser;
import net.skinsrestorer.shared.subjects.SRPlayer;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SoundUtil {
    private final SettingsManager settings;
    private final SRLogger logger;

    public void playSound(SRPlayer player) {
        if (!settings.getProperty(ServerConfig.SOUND_ENABLED)) {
            return;
        }

        Player p = player.getAs(Player.class);
        String sound = settings.getProperty(ServerConfig.SOUND_VALUE);
        SoundParser.Record record = SoundParser.parse(sound);
        if (record == null) {
            logger.warning("Invalid sound value in config: %s".formatted(sound));
            return;
        }

        logger.debug("Playing sound for player: %s".formatted(player.getName()));
        XSound.Record record2 = new XSound.Record();
        record2.withSound(XSound.matchXSound(record.getSound()).orElseThrow());
        record2.inCategory(XSound.Category.valueOf(record.getCategory()));
        record2.withVolume(record.getVolume());
        record2.withPitch(record.getPitch());

        record2.soundPlayer().forPlayers(p).play();
    }
}
