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
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.SoundProvider;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SoundUtil implements SoundProvider {
    private final SettingsManager settings;
    private final SRLogger logger;

    @Override
    public void accept(SRPlayer player) {
        logger.info("Playing sound for player: " + player.getName());
        if (!settings.getProperty(ServerConfig.SOUND_ENABLED)) {
            return;
        }

        Player p = player.getAs(Player.class);
        String sound = settings.getProperty(ServerConfig.SOUND_VALUE);
        XSound.Record record = XSound.parse(sound);
        if (record == null) {
            logger.warning("Invalid sound value in config: " + sound);
            return;
        }

        record.soundPlayer().forPlayers(p).play();
    }
}
