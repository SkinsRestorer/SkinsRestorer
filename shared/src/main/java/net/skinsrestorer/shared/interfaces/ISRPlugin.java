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
package net.skinsrestorer.shared.interfaces;

import co.aikar.commands.CommandManager;
import co.aikar.locales.LocaleManager;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.commands.ISkinCommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.CommandPropertiesManager;
import net.skinsrestorer.shared.utils.CommandReplacements;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface ISRPlugin {
    Path getDataFolder();

    SkinStorage getSkinStorage();

    String getVersion();

    CooldownStorage getCooldownStorage();

    SRLogger getLogger();

    InputStream getResource(String resource);

    void runAsync(Runnable runnable);

    void runSync(Runnable runnable);

    void runRepeat(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    Collection<ISRPlayer> getOnlinePlayers();

    ISkinCommand getSkinCommand();

    LocaleManager<ISRForeign> getLocaleManager();

    @SuppressWarnings({"deprecation"})
    default void prepareACF(CommandManager<?, ?, ?, ?, ?, ?> manager, SRLogger srLogger) {
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");
        LocaleManager<ISRForeign> localeManager = getLocaleManager();

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call()));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, localeManager.getMessage(SkinsRestorerAPIShared.getDefaultForeign(), v.call().getKey())));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, localeManager.getMessage(SkinsRestorerAPIShared.getDefaultForeign(), v.call().getKey())));
        CommandReplacements.completions.forEach((k, v) -> manager.getCommandCompletions().registerAsyncCompletion(k, c ->
                Arrays.asList(localeManager.getMessage(SkinsRestorerAPIShared.getDefaultForeign(), v.call().getKey()).split(", "))));

        CommandPropertiesManager.load(manager, getDataFolder(), getResource("command.properties"), srLogger);

        SharedMethods.allowIllegalACFNames();
    }

    default void initStorage() throws InitializeException {
        // Initialise SkinStorage
        SharedMethods.initStorage(getLogger(), getSkinStorage(), getDataFolder());

        // Preload default skins
        runAsync(getSkinStorage()::preloadDefaultSkins);
    }

    CommandManager<?, ?, ?, ?, ?, ?> getManager();

    MojangAPI getMojangAPI();

    default void checkUpdate() {
        checkUpdate(false);
    }

    void checkUpdate(boolean showUpToDate);

    default void checkUpdateInit(Runnable check) {
        Path updaterDisabled = getDataFolder().resolve("noupdate.txt");
        if (Files.exists(updaterDisabled)) {
            getLogger().info("Updater Disabled");
        } else {
            check.run();
        }
    }

    boolean isPluginEnabled(String pluginName);
}
