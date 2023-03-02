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
import net.skinsrestorer.bukkit.utils.MappingManager;
import net.skinsrestorer.bukkit.utils.NoMappingException;
import net.skinsrestorer.mappings.shared.IMapping;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

public final class PaperSkinRefresher implements Consumer<Player> {
    private final Method refreshPlayerMethod;
    private final Consumer<Player> triggerHealthUpdate;

    public PaperSkinRefresher(SRLogger logger, Server server) throws InitializeException {
        try {
            refreshPlayerMethod = BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            refreshPlayerMethod.setAccessible(true);

            this.triggerHealthUpdate = selectHealthUpdateMethod(server);

            logger.debug("Using PaperSkinRefresher");
        } catch (NoMappingException e) {
            throw e;
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

    private Consumer<Player> selectHealthUpdateMethod(Server server) throws ReflectiveOperationException, NoMappingException {
        // XP won't get updated on unsupported Paper builds
        try {
            Method healthUpdateMethod = BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("triggerHealthUpdate");
            healthUpdateMethod.setAccessible(true);

            return player -> {
                try {
                    healthUpdateMethod.invoke(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
        } catch (NoSuchMethodException ignored) {
            try {
                Method getHandleMethod = BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("getHandle");
                getHandleMethod.setAccessible(true);

                Method healthUpdateMethod = getHandleMethod.getReturnType().getDeclaredMethod("triggerHealthUpdate");
                healthUpdateMethod.setAccessible(true);

                return player -> {
                    try {
                        healthUpdateMethod.invoke(getHandleMethod.invoke(player));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
            } catch (NoSuchMethodException ignored2) {
                Optional<IMapping> mapping = MappingManager.getMapping(server);
                if (!mapping.isPresent()) {
                    throw new NoMappingException(server);
                } else {
                    return player -> {
                        try {
                            mapping.get().triggerHealthUpdate(player);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    };
                }
            }
        }
    }
}
