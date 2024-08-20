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
package net.skinsrestorer.modded;

import dev.architectury.platform.Platform;
import net.skinsrestorer.modded.logger.Slf4jLoggerImpl;
import net.skinsrestorer.modded.utils.ModSoundProvider;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class SRMod {
    private static final Logger LOGGER = LoggerFactory.getLogger("SkinsRestorer");
    public static final String MOD_ID = "skinsrestorer";

    public static void init() {
        SRBootstrapper.startPlugin(
                runnable -> {
                },
                List.of(
                        new SRBootstrapper.PlatformClass<>(SoundProvider.class, new ModSoundProvider())
                ),
                new Slf4jLoggerImpl(LOGGER),
                true,
                SRModAdapter.class,
                SRServerPlugin.class,
                Platform.getConfigFolder().resolve("skinsrestorer"),
                SRModInit.class
        );
    }
}
