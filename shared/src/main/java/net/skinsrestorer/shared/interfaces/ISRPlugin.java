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

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.shared.SkinsRestorerLocale;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public interface ISRPlugin {
    Path getDataFolder();

    String getVersion();

    InputStream getResource(String resource);

    void runAsync(Runnable runnable);

    void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    default void checkUpdate() {
        checkUpdate(false);
    }

    void checkUpdate(boolean showUpToDate);

    boolean isPluginEnabled(String pluginName);

    void loadConfig();

    void loadLocales();

    boolean isOutdated();
}
