/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bukkit.skinapplier;

import lombok.SneakyThrows;
import lombok.val;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * https://github.com/SkinsRestorer/SkinsRestorerX/pull/240
 */
public final class PaperSkinRefresher implements Consumer<Player> {
    private Method MH_REFRESH;
    private Method MH_GET_HANDLE;
    private Method MH_HEALTH_UPDATE;

    public PaperSkinRefresher(SRLogger logger) {
        try {
            MH_REFRESH = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            MH_REFRESH.setAccessible(true);
            MH_GET_HANDLE = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("getHandle");
            MH_GET_HANDLE.setAccessible(true);

            // XP won't get updated on unsupported Paper builds
            try {
                MH_HEALTH_UPDATE = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("triggerHealthUpdate");
                MH_HEALTH_UPDATE.setAccessible(true);
            } catch (Exception ignored) {
            }

            logger.info("Using PaperSkinRefresher");
        } catch (Exception e) {
            logger.info("Failed PaperSkinRefresher");
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    @SneakyThrows
    public void accept(Player player) {
        MH_REFRESH.invoke(player);

        if (MH_HEALTH_UPDATE != null) {
            MH_HEALTH_UPDATE.invoke(player);
        } else {
            ReflectionUtil.invokeMethod(MH_GET_HANDLE.invoke(player), "triggerHealthUpdate");
        }
    }
}
