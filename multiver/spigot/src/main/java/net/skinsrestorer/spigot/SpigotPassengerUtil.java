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
package net.skinsrestorer.spigot;

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.shared.config.ServerConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotPassengerUtil {
    public static void refreshPassengers(JavaPlugin plugin, Player player, Entity vehicle, boolean disableDismountPlayer, boolean disableRemountPlayer, boolean enableDismountEntities, SettingsManager settings) {
        // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
        if (settings.getProperty(ServerConfig.DISMOUNT_PLAYER_ON_UPDATE) && !disableDismountPlayer && vehicle != null) {
            vehicle.removePassenger(player);

            if (settings.getProperty(ServerConfig.REMOUNT_PLAYER_ON_UPDATE) && !disableRemountPlayer) {
                // This is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    // This is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
                    if (vehicle.isValid()) {
                        vehicle.addPassenger(player);
                    }
                }, 1);
            }
        }

        // Dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
        if ((settings.getProperty(ServerConfig.DISMOUNT_PASSENGERS_ON_UPDATE) || enableDismountEntities) && !player.isEmpty()) {
            for (Entity passenger : player.getPassengers()) {
                player.removePassenger(passenger);
            }
        }
    }
}
