/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.utils.log;

import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.YamlConfig;

import java.io.File;

public class SRLogger {
    private final ISRLogger logger;
    private final boolean color;

    public SRLogger(ISRLogger logger) {
        this(logger, false);
    }

    public SRLogger(ISRLogger logger, boolean color) {
        this.logger = logger;
        this.color = color;
    }

    public void load(File pluginFolder) {
        try {
            // Manual check config value
            File pluginConfigFile = new File(pluginFolder, "config.yml");
            YamlConfig pluginConfig = new YamlConfig(pluginFolder, "config.yml", false, this);

            if (pluginConfigFile.exists()) {
                pluginConfig.reload();

                if (pluginConfig.getBoolean("Debug")) {
                    Config.DEBUG = true;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void debug(String message) {
        debug(SRLogLevel.INFO, message);
    }

    public void debug(SRLogLevel level, String message) {
        if (!Config.DEBUG)
            return;

        log(level, message);
    }

    public void debug(SRLogLevel level, String message, Throwable thrown) {
        if (!Config.DEBUG)
            return;

        log(level, message, thrown);
    }

    public void info(String message) {
        log(SRLogLevel.INFO, message);
    }

    public void info(String message, Throwable thrown) {
        log(SRLogLevel.INFO, message, thrown);
    }

    public void warning(String message) {
        log(SRLogLevel.WARNING, message);
    }

    public void warning(String message, Throwable thrown) {
        log(SRLogLevel.WARNING, message, thrown);
    }

    public void severe(String message) {
        log(SRLogLevel.SEVERE, message);
    }

    public void severe(String message, Throwable thrown) {
        log(SRLogLevel.SEVERE, message, thrown);
    }

    private void log(SRLogLevel level, String message) {
        logger.log(level, color ? "§e[§2SkinsRestorer§e] §r" + message : message);
    }

    private void log(SRLogLevel level, String message, Throwable thrown) {
        logger.log(level, color ? "§e[§2SkinsRestorer§e] §r" + message : message, thrown);
    }
}
