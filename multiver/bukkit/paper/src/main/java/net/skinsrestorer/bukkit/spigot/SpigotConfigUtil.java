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
package net.skinsrestorer.bukkit.spigot;

import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Optional;

public class SpigotConfigUtil {
    @SuppressWarnings("removal")
    public static Optional<YamlConfiguration> getSpigotConfig(Server server) {
        try {
            return Optional.of(server.spigot().getConfig());
        } catch (Throwable ignored) { // We're not running spigot
            return Optional.empty();
        }
    }
}
