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
package net.skinsrestorer.shared.log;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class SRLogger {
    private final SRPlatformLogger logger;
    private final boolean color;
    @Setter
    private boolean debug = false;

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
        if (!debug) {
            return;
        }

        log(level, message);
    }

    public void debug(SRLogLevel level, String message, Throwable thrown) {
        if (!debug) {
            return;
        }

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
        logger.log(level, formatMessage(level, message));
    }

    private void log(SRLogLevel level, String message, Throwable thrown) {
        logger.log(level, formatMessage(level, message), thrown);
    }

    private String formatMessage(SRLogLevel level, String message) {
        message = color ? "§e[§2SkinsRestorer§e] §r%s%s".formatted(switch (level) {
            case INFO -> "";
            case WARNING -> "§e";
            case SEVERE -> "§c";
        }, message) : message;
        message += "§r";
        message = ANSIConverter.convertToAnsi(message);
        return message;
    }
}
