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
package net.skinsrestorer.fand;

import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import org.slf4j.Logger;

import java.util.Objects;

public final class SRFandLogger implements SRPlatformLogger {
    private final Logger logger;

    public SRFandLogger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void log(SRLogLevel level, String message) {
        log(level, message, null);
    }

    @Override
    public void log(SRLogLevel level, String message, Throwable throwable) {
        if (throwable == null) {
            switch (level) {
                case INFO -> logger.info(message);
                case WARNING -> logger.warn(message);
                case SEVERE -> logger.error(message);
            }
            return;
        }
        switch (level) {
            case INFO -> logger.info(message, throwable);
            case WARNING -> logger.warn(message, throwable);
            case SEVERE -> logger.error(message, throwable);
        }
    }
}
