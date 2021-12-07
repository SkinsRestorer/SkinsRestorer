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
package net.skinsrestorer.api.serverinfo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

@Getter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode
public class ServerVersion {
    private final int major;
    private final int minor;
    private final int patch;

    @Nullable
    public static String getNMSVersion() {
        try {
            String serverPackage = Class.forName("org.bukkit.Bukkit").getMethod("getServer").invoke(null).getClass().getPackage().getName();

            return serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            return null;
        }
    }

    public String getFormatted() {
        return major + "." + minor + "." + patch;
    }

    public boolean isNewer(ServerVersion version2) {
        if (this.equals(version2))
            return false;

        if (version2.major > major) {
            return false;
        } else if (version2.major < major) {
            return true;
        } else if (version2.minor > minor) {
            return false;
        } else if (version2.minor < minor) {
            return true;
        } else if (version2.patch > patch) {
            return false;
        } else return version2.patch < patch;
    }

    @Override
    public String toString() {
        return getFormatted();
    }
}
