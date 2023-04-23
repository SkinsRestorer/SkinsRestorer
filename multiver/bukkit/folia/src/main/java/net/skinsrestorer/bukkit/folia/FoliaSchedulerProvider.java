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
package net.skinsrestorer.bukkit.folia;

import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import net.skinsrestorer.shared.serverinfo.ClassInfo;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class FoliaSchedulerProvider implements SchedulerProvider {
    @Override
    public void runAsync(Server server, Plugin plugin, Runnable runnable) {
        server.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public void runSync(Server server, Plugin plugin, Runnable runnable) {
        server.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
    }

    @Override
    public void runSyncToEntity(Server server, Plugin plugin, Entity entity, Runnable runnable) {
        entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null);
    }

    @Override
    public void runRepeatAsync(Server server, Plugin plugin, Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        server.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, interval, timeUnit);
    }

    @Override
    public boolean isAvailable() {
        return ClassInfo.get().isFolia();
    }
}
