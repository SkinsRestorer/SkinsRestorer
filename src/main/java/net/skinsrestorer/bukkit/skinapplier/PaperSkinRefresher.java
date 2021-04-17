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
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * https://github.com/SkinsRestorer/SkinsRestorerX/pull/240
 */
final class PaperSkinRefresher implements Consumer<Player> {
    private final MethodHandle MH_REFRESH;
    private final MethodHandle MH_GET_HANDLE;
    private MethodHandle MH_HEALTH_UPDATE = null;

    public PaperSkinRefresher(SRLogger logger) {
        try {
            val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            field.setAccessible(true);
            MethodHandles.publicLookup();
            val lookup = (MethodHandles.Lookup) field.get(null);
            MH_REFRESH = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "refreshPlayer", MethodType.methodType(Void.TYPE));
            MH_GET_HANDLE = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "getHandle", MethodType.methodType(ReflectionUtil.getNMSClass("EntityPlayer")));

            // XP won't get updated on unsupported Paper builds
            try {
                MH_HEALTH_UPDATE = lookup.findVirtual(ReflectionUtil.getBukkitClass("entity.CraftPlayer"), "triggerHealthUpdate", MethodType.methodType(Void.TYPE));
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
            val handle = MH_GET_HANDLE.invoke(player);
            ReflectionUtil.invokeMethod(handle, "triggerHealthUpdate");
        }
    }
}
