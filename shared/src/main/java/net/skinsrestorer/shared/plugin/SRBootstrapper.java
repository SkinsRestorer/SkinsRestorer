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
package net.skinsrestorer.shared.plugin;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.log.SRPlatformLogger;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.UpdateCheckInit;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class SRBootstrapper {
    public static void startPlugin(
            Consumer<Injector> platformRegister,
            SRPlatformLogger isrLogger, boolean loggerColor,
            Function<Injector, SRPlatformAdapter<?>> createAdapter,
            Class<? extends UpdateCheckInit> updateCheck, Class<?> srPlatformClass,
            String version, Path dataFolder, Platform platform,
            Class<? extends SRPlatformInit> initCLass) {
        SRPlugin srPlugin = null;
        try {
            Injector injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

            platformRegister.accept(injector);

            injector.register(SRLogger.class, new SRLogger(isrLogger, loggerColor));

            injector.register(SRPlatformAdapter.class, createAdapter.apply(injector));
            srPlugin = new SRPlugin(injector, version, dataFolder, platform, updateCheck);
            injector.getSingleton(srPlatformClass);

            srPlugin.startup(injector.newInstance(initCLass));
        } catch (Throwable e) {
            e.printStackTrace();
            isrLogger.log(SRLogLevel.SEVERE, "An unexpected error occurred while starting the plugin. Please check the console for more details.");

            if (SRPlugin.isUnitTest()) {
                throw new AssertionError("Failed to start plugin: " + e.getMessage());
            }
        }

        if (srPlugin != null && !srPlugin.isUpdaterInitialized()) {
            isrLogger.log(SRLogLevel.WARNING, "Updater was not initialized, a error occurred while starting the plugin. Forcing updater to initialize.");
            try {
                srPlugin.initUpdateCheck();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
