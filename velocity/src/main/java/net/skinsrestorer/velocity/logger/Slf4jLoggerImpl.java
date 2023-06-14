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
package net.skinsrestorer.velocity.logger;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class Slf4jLoggerImpl implements SRPlatformLogger {
    private final Logger logger;

    @Override
    public void log(SRLogLevel level, String message) {
        switch (level) {
            case INFO:
                logger.info(message);
                break;
            case WARNING:
                logger.warn(message);
                break;
            case SEVERE:
                logger.error(message);
                break;
            default:
                break;
        }
    }

    @Override
    public void log(SRLogLevel level, String message, Throwable throwable) {
        switch (level) {
            case INFO:
                logger.info(message, throwable);
                break;
            case WARNING:
                logger.warn(message, throwable);
                break;
            case SEVERE:
                logger.error(message, throwable);
                break;
            default:
                break;
        }
    }
}
