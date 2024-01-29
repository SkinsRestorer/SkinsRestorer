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

import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.shared.plugin.SRPlugin;
import org.bukkit.Bukkit;

public class BukkitReflection {
    public static final String CRAFTBUKKIT_PACKAGE = getCraftBukkitString();
    public static final SemanticVersion SERVER_VERSION = getServerVersion();

    public static Class<?> getBukkitClass(String clazz) throws ClassNotFoundException {
        return Class.forName(CRAFTBUKKIT_PACKAGE + "." + clazz);
    }

    public static Class<?> getNMSClass(String clazz, String fullClassName) throws ClassNotFoundException {
        if (fullClassName != null) {
            try {
                return Class.forName(fullClassName);
            } catch (ClassNotFoundException ignored) {
            }
        }

        return Class.forName("net.minecraft.server." + getLegacyVersionString() + "." + clazz);
    }

    private static String getCraftBukkitString() {
        if (SRPlugin.isUnitTest()) {
            return "org.bukkit.craftbukkit";
        }

        return Bukkit.getServer().getClass().getPackage().getName();
    }

    private static SemanticVersion getServerVersion() {
        String fullVersion = Bukkit.getServer().getBukkitVersion();
        String versionString = fullVersion.substring(0, fullVersion.indexOf('-'));
        return SemanticVersion.fromString(versionString);
    }

    private static String getLegacyVersionString() {
        return CRAFTBUKKIT_PACKAGE.substring(CRAFTBUKKIT_PACKAGE.lastIndexOf('.') + 1);
    }
}
