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

import net.skinsrestorer.shared.serverinfo.ServerVersion;

public class BukkitReflection {
    public static final String SERVER_VERSION_STRING;
    public static final ServerVersion SERVER_VERSION;

    static {
        SERVER_VERSION_STRING = ServerVersion.getNMSVersion().orElseThrow(() -> new RuntimeException("Failed to get NMS version"));

        String[] strings;
        if (SERVER_VERSION_STRING.contains("_")) {
            strings = SERVER_VERSION_STRING.replace("v", "").replace("R", "").split("_");
        } else {
            strings = SERVER_VERSION_STRING.replace("-R0.1-SNAPSHOT", "").split("\\.");
        }

        SERVER_VERSION = new ServerVersion(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
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
}
