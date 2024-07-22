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
package net.skinsrestorer;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import org.junit.jupiter.api.extension.*;

public class SRExtension implements BeforeAllCallback, ParameterResolver {
    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty("sr.unit.test", "true");
        System.setProperty("sr.check.version", "1.0.0");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == Injector.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Injector injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

        SRLogger logger = new SRLogger(new SRPlatformLogger() {
            @Override
            public void log(SRLogLevel level, String message) {
                System.out.printf("%s %s%n", level, message);
            }

            @Override
            public void log(SRLogLevel level, String message, Throwable throwable) {
                System.out.printf("%s %s%n", level, message);
                throwable.printStackTrace();
            }
        }, false);
        logger.setDebug(true);
        injector.register(SRLogger.class, logger);

        return injector;
    }
}
