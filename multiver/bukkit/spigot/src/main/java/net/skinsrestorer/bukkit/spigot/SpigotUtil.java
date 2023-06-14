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
package net.skinsrestorer.bukkit.spigot;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

public class SpigotUtil {
    public static YamlConfiguration getSpigotConfig(Server server) {
        return server.spigot().getConfig();
    }

    public static boolean isRealSpigot(Server server) {
        try {
            server.spigot().getConfig();
            return true;
        } catch (UnsupportedOperationException e) { // Hypbrid forks don't have a spigot config
            return false;
        }
    }

    public static boolean hasPassengerMethods() {
        try {
            Entity.class.getMethod("getPassengers");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
