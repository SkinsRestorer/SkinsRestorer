/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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

import net.skinsrestorer.shared.serverinfo.SemanticVersion;

public class NMSVersion {
    public static final SemanticVersion SERVER_VERSION;

    static {
        String serverVersionString = BukkitReflection.SERVER_VERSION_STRING;

        String[] strings;
        if (serverVersionString.contains("_")) {
            strings = serverVersionString.replace("v", "").replace("R", "").split("_");
        } else {
            strings = serverVersionString.replace("-R0.1-SNAPSHOT", "").split("\\.");
        }

        SERVER_VERSION = new SemanticVersion(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
    }
}
