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

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.commands.library.CommandPlatform;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface SRPlatformAdapter<P> extends CommandPlatform<SRCommandSender> {
    InputStream getResource(String resource);

    void runAsync(Runnable runnable);

    void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    boolean isPluginEnabled(String pluginName);

    String getPlatformVersion();

    Optional<SkinProperty> getSkinProperty(SRPlayer player);

    Object createMetricsInstance();

    /**
     * Force an object to stay alive as long as another plugin is loaded.
     * This can be done by registering a listener to the plugin and keeping a reference to the object in the listener.
     *
     * @param plugin The plugin to keep the object alive.
     * @param object The object to keep alive.
     */
    void extendLifeTime(P plugin, Object object);
}
