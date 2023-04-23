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
package net.skinsrestorer.bukkit.listener;

import net.skinsrestorer.bukkit.gui.SkinsGUIHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getView().getTopInventory();
        Inventory clickedInventory = event.getView().getInventory(event.getRawSlot());
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof SkinsGUIHolder) {
            if (inventory == clickedInventory) { // Only handle if there was a click in the top inventory
                try {
                    ((SkinsGUIHolder) holder).onClick(event);
                } catch (Exception e) { // Ensure event always cancels
                    e.printStackTrace();
                }
            }

            event.setCancelled(true);
        }
    }
}
