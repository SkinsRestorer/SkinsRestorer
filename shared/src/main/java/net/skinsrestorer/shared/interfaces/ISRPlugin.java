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
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.CommandPropertiesManager;
import net.skinsrestorer.shared.utils.CommandReplacements;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

public interface ISRPlugin {
    File getDataFolder();

    SkinStorage getSkinStorage();

    String getVersion();

    MetricsCounter getMetricsCounter();

    InputStream getResource(String resource);

    @SuppressWarnings({"deprecation"})
    default void prepareACF(CommandManager<?, ?, ?, ?, ?, ?> manager, SRLogger srLogger) {
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call()));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call()));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call()));
        CommandReplacements.completions.forEach((k, v) -> manager.getCommandCompletions().registerAsyncCompletion(k, c ->
                Arrays.asList(v.call().split(", "))));

        CommandPropertiesManager.load(manager, getDataFolder(), getResource("command-messages.properties"), srLogger);

        SharedMethods.allowIllegalACFNames();
    }
}
