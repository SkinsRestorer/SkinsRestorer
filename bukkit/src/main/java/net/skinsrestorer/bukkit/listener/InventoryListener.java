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
package net.skinsrestorer.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.gui.BukkitGUIHolder;
import net.skinsrestorer.shared.log.SRLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class InventoryListener implements Listener {
    private final SRLogger logger;

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (holder instanceof BukkitGUIHolder guiHolder) {
            if (isInTop(topInventory, event.getRawSlot())) { // Only handle if there was a click in the top inventory
                try {
                    guiHolder.onClick(event);
                } catch (Throwable e) { // Ensure event always cancels
                    logger.severe("Failed to handle inventory click event", e);
                }
            }

            event.setCancelled(true);
        }
    }

    public boolean isInTop(Inventory topInventory, int rawSlot) {
        return rawSlot < topInventory.getSize();
    }
}
