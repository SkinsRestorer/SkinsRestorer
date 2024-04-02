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

import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

public interface SchedulerProvider {
    void runAsync(Runnable runnable);

    void runSync(Runnable runnable);

    void runSyncDelayed(Runnable runnable, long ticks);

    void runSyncToEntity(Entity entity, Runnable runnable);

    void runSyncToEntityDelayed(Entity entity, Runnable runnable, long ticks);

    void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    void unregisterTasks();
}
