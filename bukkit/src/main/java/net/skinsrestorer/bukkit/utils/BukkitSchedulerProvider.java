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
package net.skinsrestorer.bukkit.utils;

import lombok.RequiredArgsConstructor;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitSchedulerProvider implements SchedulerProvider {
    private final Server server;
    private final JavaPlugin plugin;

    @Override
    public void runAsync(Runnable runnable) {
        server.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        server.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runSyncDelayed(Runnable runnable, long ticks) {
        server.getScheduler().runTaskLater(plugin, runnable, ticks);
    }

    @Override
    public void runSyncToEntity(Entity entity, Runnable runnable) {
        runSync(runnable);
    }

    @Override
    public void runSyncToEntityDelayed(Entity entity, Runnable runnable, long ticks) {
        runSyncDelayed(runnable, ticks);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        server.getScheduler().runTaskTimerAsynchronously(plugin, runnable, timeUnit.toSeconds(delay) * 20L, timeUnit.toSeconds(interval) * 20L);
    }

    @Override
    public void unregisterTasks() {
        server.getScheduler().cancelTasks(plugin);
    }
}
