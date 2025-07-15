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
package net.skinsrestorer.bukkit.folia;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FoliaSchedulerProvider implements SchedulerProvider {
    private final Server server;
    private final JavaPlugin plugin;

    public static boolean isAvailable() {
        try {
            Server.class.getMethod("getAsyncScheduler");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        server.getAsyncScheduler().runNow(plugin, getCancellingTaskConsumer(runnable));
    }

    @Override
    public void runAsyncDelayed(Runnable runnable, long delay, TimeUnit timeUnit) {
        server.getAsyncScheduler().runDelayed(plugin, getCancellingTaskConsumer(runnable), delay, timeUnit);
    }

    @Override
    public void runSync(Runnable runnable) {
        server.getGlobalRegionScheduler().run(plugin, getCancellingTaskConsumer(runnable));
    }

    @Override
    public void runSyncDelayed(Runnable runnable, long ticks) {
        server.getGlobalRegionScheduler().runDelayed(plugin, getCancellingTaskConsumer(runnable), ticks);
    }

    @Override
    public void runSyncToEntity(Entity entity, Runnable runnable) {
        entity.getScheduler().run(plugin, getCancellingTaskConsumer(runnable), null);
    }

    @Override
    public void runSyncToEntityDelayed(Entity entity, Runnable runnable, long ticks) {
        entity.getScheduler().runDelayed(plugin, getCancellingTaskConsumer(runnable), null, ticks);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, long delay, long interval, TimeUnit timeUnit) {
        server.getAsyncScheduler().runAtFixedRate(plugin, getCancellingTaskConsumer(runnable), delay, interval, timeUnit);
    }

    @Override
    public void unregisterTasks() {
        server.getAsyncScheduler().cancelTasks(plugin);
        server.getGlobalRegionScheduler().cancelTasks(plugin);
    }

    private Consumer<ScheduledTask> getCancellingTaskConsumer(Runnable runnable) {
        return scheduledTask -> {
            if (plugin.isEnabled()) {
                runnable.run();
            } else {
                // If the plugin is disabled, cancel the task from running again.
                scheduledTask.cancel();
            }
        };
    }
}
