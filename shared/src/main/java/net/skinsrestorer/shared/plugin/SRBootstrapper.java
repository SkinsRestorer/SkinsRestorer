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
package net.skinsrestorer.shared.plugin;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import net.skinsrestorer.shared.update.UpdateCheckInit;

import java.nio.file.Path;
import java.util.function.Consumer;

public class SRBootstrapper {
    public static void startPlugin(
            Consumer<Runnable> shutdownHookConsumer,
            Consumer<Injector> platformRegister,
            SRPlatformLogger isrLogger, boolean loggerColor,
            Class<? extends SRPlatformAdapter<?>> adapterClass,
            Class<?> srPlatformClass,
            String version, Path dataFolder,
            Class<? extends SRPlatformInit> initCLass) {
        SRPlugin srPlugin = null;
        try {
            Injector injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

            platformRegister.accept(injector);

            injector.register(SRLogger.class, new SRLogger(isrLogger, loggerColor));

            SRPlatformAdapter<?> adapter = injector.getSingleton(adapterClass);
            injector.register(SRPlatformAdapter.class, adapter);
            if (adapter instanceof SRServerAdapter) {
                injector.register(SRServerAdapter.class, (SRServerAdapter<?>) adapter);
            } else if (adapter instanceof SRProxyAdapter) {
                injector.register(SRProxyAdapter.class, (SRProxyAdapter<?>) adapter);
            }

            srPlugin = new SRPlugin(injector, version, dataFolder);
            injector.getSingleton(srPlatformClass);

            // Allow a platform to call plugin shutdown
            shutdownHookConsumer.accept(srPlugin::shutdown);

            srPlugin.startup(initCLass);
        } catch (Throwable t) {
            isrLogger.log(SRLogLevel.SEVERE, "An unexpected error occurred while starting the plugin. Please check the console for more details.", t);

            if (SRPlugin.isUnitTest()) {
                throw new AssertionError(t);
            }
        }

        if (srPlugin != null && !srPlugin.isUpdaterInitialized()) {
            isrLogger.log(SRLogLevel.WARNING, "Updater was not initialized, a error occurred while starting the plugin. Forcing updater to initialize.");
            try {
                srPlugin.initUpdateCheck(UpdateCheckInit.InitCause.ERROR);
            } catch (Throwable t) {
                isrLogger.log(SRLogLevel.SEVERE, "An unexpected error occurred while initializing the updater. Please check the console for more details.", t);
            }
        }
    }
}
