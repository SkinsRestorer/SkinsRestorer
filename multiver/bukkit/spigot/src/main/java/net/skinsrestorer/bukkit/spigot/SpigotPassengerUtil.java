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

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import net.skinsrestorer.shared.config.ServerConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SpigotPassengerUtil {
    private final SchedulerProvider scheduler;
    private final SettingsManager settings;

    public static boolean isAvailable() {
        try {
            Entity.class.getMethod("getPassengers");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public void ejectPassengers(Player player) {
        Entity vehicle = player.getVehicle();

        // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
        if (settings.getProperty(ServerConfig.DISMOUNT_PLAYER_ON_UPDATE) && vehicle != null) {
            vehicle.removePassenger(player);

            if (settings.getProperty(ServerConfig.REMOUNT_PLAYER_ON_UPDATE)) {
                // This is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
                scheduler.runSyncToEntityDelayed(player, () -> {
                    // This is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
                    if (vehicle.isValid()) {
                        vehicle.addPassenger(player);
                    }
                }, 1L);
            }
        }

        // Dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
        if (settings.getProperty(ServerConfig.DISMOUNT_PASSENGERS_ON_UPDATE) && !player.isEmpty()) {
            for (Entity passenger : player.getPassengers()) {
                player.removePassenger(passenger);
            }
        }
    }
}
