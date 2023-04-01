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
package net.skinsrestorer.mappings.shared;

import org.bukkit.entity.Player;

import java.util.Optional;

public class BukkitReflection {
    public static final String SERVER_VERSION_STRING;

    static {
        SERVER_VERSION_STRING = getNMSVersion().orElseThrow(() -> new RuntimeException("Failed to get NMS version"));
    }

    public static <E> E getHandle(Player player, Class<E> eClass) throws ReflectiveOperationException {
        return eClass.cast(BukkitReflection.getBukkitClass("entity.CraftPlayer")
                .getDeclaredMethod("getHandle").invoke(player));
    }

    public static Class<?> getBukkitClass(String clazz) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + SERVER_VERSION_STRING + "." + clazz);
    }

    public static Class<?> getNMSClass(String clazz, String fullClassName) throws ClassNotFoundException {
        try {
            return Class.forName("net.minecraft.server." + SERVER_VERSION_STRING + "." + clazz);
        } catch (ClassNotFoundException ignored) {
            if (fullClassName != null) {
                return Class.forName(fullClassName);
            }

            throw new ClassNotFoundException("Could not find net.minecraft.server." + SERVER_VERSION_STRING + "." + clazz);
        }
    }

    private static Optional<String> getNMSVersion() {
        String propertyVersion = System.getProperty("sr.nms.version");
        if (propertyVersion != null) {
            return Optional.of(propertyVersion);
        }

        try {
            Object bukkitServer = Class.forName("org.bukkit.Bukkit").getMethod("getServer").invoke(null);

            if (bukkitServer == null) {
                return Optional.empty();
            }

            String serverPackage = bukkitServer.getClass().getPackage().getName();

            return Optional.of(serverPackage.substring(serverPackage.lastIndexOf('.') + 1));
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }
}
