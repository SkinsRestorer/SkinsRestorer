/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.mappings.mapping1_18.Mapping1_18;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class PaperSkinRefresher implements Consumer<Player> {
    private final Method refreshPlayerMethod;
    private final Consumer<Player> triggerHealthUpdate;

    public PaperSkinRefresher(SRLogger logger) throws InitializeException {
        try {
            refreshPlayerMethod = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            refreshPlayerMethod.setAccessible(true);

            Consumer<Player> triggerHealthUpdate;
            // XP won't get updated on unsupported Paper builds
            try {
                Method healthUpdateMethod = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("triggerHealthUpdate");
                healthUpdateMethod.setAccessible(true);

                triggerHealthUpdate = player -> {
                    try {
                        healthUpdateMethod.invoke(player);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                };
            } catch (NoSuchMethodException ignored) {
                try {
                    Method getHandleMethod = ReflectionUtil.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("getHandle");
                    getHandleMethod.setAccessible(true);

                    Method healthUpdateMethod = getHandleMethod.getReturnType().getDeclaredMethod("triggerHealthUpdate");
                    healthUpdateMethod.setAccessible(true);

                    triggerHealthUpdate = player -> {
                        try {
                            healthUpdateMethod.invoke(getHandleMethod.invoke(player));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    };
                } catch (NoSuchMethodException ignored2) {
                    triggerHealthUpdate = player -> {
                        try {
                            Mapping1_18.triggerHealthUpdate(player);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    };

                }
            }
            this.triggerHealthUpdate = triggerHealthUpdate;

            logger.info("Using PaperSkinRefresher");
        } catch (Exception e) {
            logger.info("Failed PaperSkinRefresher");
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
