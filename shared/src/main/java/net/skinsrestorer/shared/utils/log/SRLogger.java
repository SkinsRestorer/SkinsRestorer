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

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.shared.interfaces.ISRLogger;
import net.skinsrestorer.shared.storage.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class SRLogger {
    private final ISRLogger logger;
    private final boolean color;
    @Setter
    private boolean debug = false;

    public SRLogger(ISRLogger logger) {
        this(logger, false);
    }

    public void load(Path dataFolder) {
        Path configFile = dataFolder.resolve("config.yml");

        if (Files.exists(configFile)) {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = Files.newInputStream(configFile)) {
                SimpleConfig config = yaml.loadAs(inputStream, SimpleConfig.class);
                debug = config.Debug;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void debug(String message) {
        debug(SRLogLevel.INFO, message);
    }

    public void debug(String message, Throwable thrown) {
        debug(SRLogLevel.WARNING, message, thrown);
    }

    public void debug(Throwable thrown) {
        debug(SRLogLevel.WARNING, "Received error", thrown);
    }

    public void debug(SRLogLevel level, String message) {
        if (!Config.DEBUG || debug)
            return;

        log(level, message);
    }

    public void debug(SRLogLevel level, String message, Throwable thrown) {
        if (!Config.DEBUG || debug)
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
        logger.log(level, formatMessage(message));
    }

    private void log(SRLogLevel level, String message, Throwable thrown) {
        logger.log(level, formatMessage(message), thrown);
    }

    private String formatMessage(String message) {
        message = color ? "§e[§2SkinsRestorer§e] §r" + message : message;
        message += "§r";
        message = ANSIConverter.convertToAnsi(message);
        return message;
    }

    private static class SimpleConfig {
        private boolean Debug = false;
    }
}
