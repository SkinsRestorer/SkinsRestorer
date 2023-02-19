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
import net.skinsrestorer.api.property.SkinProperty;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SRPlatformAdapter {
    InputStream getResource(String resource);

    void runAsync(Runnable runnable);

    void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    boolean isPluginEnabled(String pluginName);

    String getPlatformVersion();

    List<SkinProperty> getPropertiesOfPlayer(SRPlayer player);

    Collection<SRPlayer> getOnlinePlayers();

    CommandManager<?, ?, ?, ?, ?, ?> createCommandManager();

    Object createMetricsInstance();

    SRCommandSender convertCommandSender(Object sender);
}
