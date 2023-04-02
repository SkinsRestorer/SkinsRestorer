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
package net.skinsrestorer.bukkit.skinrefresher;

import lombok.SneakyThrows;
import net.skinsrestorer.bukkit.utils.BukkitReflection;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class PaperSkinRefresher implements Consumer<Player> {
    private final Method refreshPlayerMethod;
    private final Consumer<Player> triggerHealthUpdate;

    public PaperSkinRefresher(SRLogger logger) throws InitializeException {
        try {
            refreshPlayerMethod = BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            refreshPlayerMethod.setAccessible(true);

            // XP won't get updated on unsupported Paper builds
            this.triggerHealthUpdate = player -> {
                try {
                    Object entityPlayer = BukkitReflection.getHandle(player, Object.class);

                    ReflectionUtil.invokeMethod(entityPlayer, "triggerHealthUpdate");
                } catch (ReflectiveOperationException e) {
                    player.resetMaxHealth();
                }
            };

            logger.debug("Using PaperSkinRefresher");
        } catch (Exception e) {
            logger.debug("Failed PaperSkinRefresher", e);
            throw new InitializeException(e);
        }
    }

    @Override
    @SneakyThrows
    public void accept(Player player) {
        refreshPlayerMethod.invoke(player);
        triggerHealthUpdate.accept(player);
    }
}
