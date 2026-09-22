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
package net.skinsrestorer.mod;

import net.skinsrestorer.mod.logger.Slf4jLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class SRMod {
    public static final String MOD_ID_NAME = "skinsrestorer";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID_NAME);

    public static void init() {
        // Independent of the rest of the bootstrap below: must run even when this mod
        // instance is a pure client connecting to an external server it does not host.
        SRModPlatform.INSTANCE.initTabHeadVisibilityChannel();

        SRBootstrapper.startPlugin(
                runnable -> {
                },
                List.of(),
                new Slf4jLoggerImpl(LOGGER),
                true,
                SRModAdapter.class,
                SRServerPlugin.class,
                SRModPlatform.INSTANCE.getConfigFolder().resolve(MOD_ID_NAME),
                SRModInit.class
        );
    }

    private SRMod() {
    }
}
