/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.utils.log;

import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.YamlConfig;

import java.io.File;

public class SRLogger {
    private final ISRLogger logger;
    private final boolean color;

    public SRLogger(File pluginFolder, ISRLogger logger) {
        this.logger = logger;
        this.color = false;

        load(pluginFolder);
    }

    public SRLogger(File pluginFolder, ISRLogger logger, boolean color) {
        this.logger = logger;
        this.color = color;

        load(pluginFolder);
    }

    private void load(File pluginFolder) {
        try {
            // Manual check config value
            File pluginConfigFile = new File(pluginFolder, "config.yml");
            YamlConfig pluginConfig = new YamlConfig(pluginFolder, "config", false);

            if (pluginConfigFile.exists()) {
                pluginConfig.reload();

                if (pluginConfig.getBoolean("Debug")) {
                    Config.DEBUG = true;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void log(String message) {
        log(SRLogLevel.INFO, message);
    }

    public void log(SRLogLevel level, String message) {
        if (!Config.DEBUG)
            return;

        logAlways(level, message);
    }

    public void log(SRLogLevel level, String message, Throwable thrown) {
        if (!Config.DEBUG)
            return;

        logAlways(level, message, thrown);
    }

    public void logAlways(String message) {
        logAlways(SRLogLevel.INFO, message);
    }

    public void logAlways(SRLogLevel level, String message) {
        logger.log(level, color ? "§e[§2SkinsRestorer§e] §r" + message : message);
    }

    public void logAlways(SRLogLevel level, String message, Throwable thrown) {
        logger.log(level, color ? "§e[§2SkinsRestorer§e] §r" + message : message, thrown);
    }
}
