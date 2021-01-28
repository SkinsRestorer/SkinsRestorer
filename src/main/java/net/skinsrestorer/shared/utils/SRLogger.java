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
package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.YamlConfig;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SRLogger {
    private final File folder;
    private java.util.logging.Logger logger;

    public SRLogger(File pluginFolder) {
        folder = pluginFolder;
        load();
    }

    private void load() {
        try {
            //Manual check config value
            File pluginConfigFile = new File(folder.getAbsolutePath() + File.separator + "config.yml");
            YamlConfig pluginConfig = new YamlConfig(folder.getAbsolutePath() + File.separator, "config", false);

            if (pluginConfigFile.exists()) {
                pluginConfig.reload();

                if (pluginConfig.getBoolean("Debug")) {
                    Config.DEBUG = true;
                }
            }
        } catch (Exception ignored) {
        }

        if (Config.DEBUG) {
            logger = Logger.getLogger(SRLogger.class.getName());
        } else {
            logger = Logger.getLogger("");
        }
    }

    public void log(String message) {
        this.log(Level.INFO, message);
    }

    public void logAlways(String message) {
        this.logAlways(Level.INFO, message);
    }

    public void log(Level level, String message, Throwable thrown) {
        if (!Config.DEBUG)
            return;

        this.logAlways(level, message, thrown);
    }

    public void log(Level level, String message) {
        if (!Config.DEBUG)
            return;

        this.logAlways(level, message);
    }

    public void logAlways(Level level, String message) {
        logger.log(level, "§e[§2SkinsRestorer§e] §r" + message);
    }

    public void logAlways(Level level, String message, Throwable thrown) {
        logger.log(level, "§e[§2SkinsRestorer§e] §r" + message, thrown);
    }
}
