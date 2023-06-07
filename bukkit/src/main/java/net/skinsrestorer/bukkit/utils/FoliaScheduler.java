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
package net.skinsrestorer.bukkit.utils;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.reflection.exception.ReflectionException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class FoliaScheduler {

  private static final boolean FOLIA = ReflectionUtil.classExists(
      "io.papermc.paper.threadedregions.RegionizedServer");

  private final Object asyncScheduler;
  private final Object globalRegionScheduler;

  private FoliaScheduler() throws ReflectionException {
    asyncScheduler = ReflectionUtil.invokeMethod(Bukkit.getServer(), "getAsyncScheduler");
    globalRegionScheduler = ReflectionUtil.invokeMethod(Bukkit.getServer(),
        "getGlobalRegionScheduler");
  }

  public void runNow(Plugin plugin, Runnable runnable) throws ReflectionException {
    ReflectionUtil.invokeMethod(asyncScheduler.getClass(), asyncScheduler, "runNow",
        new Class[]{Plugin.class, Consumer.class},
        plugin, (Consumer<Object>) ignored -> runnable.run());

  }

  public void execute(Plugin plugin, Runnable runnable) throws ReflectionException {
    ReflectionUtil.invokeMethod(globalRegionScheduler.getClass(), globalRegionScheduler, "execute",
        new Class[]{Plugin.class, Runnable.class},
        plugin, runnable);

  }

  public void runAtFixedRate(Plugin plugin, Runnable runnable, int delay, int interval,
      TimeUnit timeUnit) throws ReflectionException {
    ReflectionUtil.invokeMethod(asyncScheduler.getClass(), asyncScheduler, "runAtFixedRate",
        new Class[]{Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class},
        plugin, (Consumer<Object>) ignored -> runnable.run(), delay, interval,
        timeUnit);
  }

  private static FoliaScheduler scheduler;

  public static FoliaScheduler getInstance() throws ReflectionException {
    if (scheduler == null) {
      scheduler = new FoliaScheduler();
    }
    return scheduler;
  }

  public static boolean isSupported() {
    return FOLIA;
  }

}
