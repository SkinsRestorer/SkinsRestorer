/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.log;

import lombok.RequiredArgsConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class JavaLoggerImpl implements SRPlatformLogger {
    private final SRConsole console;
    private final Logger logger;

    @Override
    public void log(SRLogLevel level, String message) {
        switch (level) {
            case INFO:
                console.sendMessage(message);
                break;
            case WARNING:
                logger.warning(message);
                break;
            case SEVERE:
                logger.severe(message);
                break;
            default:
                break;
        }
    }

    @Override
    public void log(SRLogLevel level, String message, Throwable throwable) {
        switch (level) {
            case INFO:
                logger.log(Level.INFO, message, throwable);
                break;
            case WARNING:
                logger.log(Level.WARNING, message, throwable);
                break;
            case SEVERE:
                logger.log(Level.SEVERE, message, throwable);
                break;
            default:
                break;
        }
    }
}
