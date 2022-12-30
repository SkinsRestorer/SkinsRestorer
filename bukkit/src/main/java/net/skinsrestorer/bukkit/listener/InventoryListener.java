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
package net.skinsrestorer.bukkit.listener;

import net.skinsrestorer.bukkit.SkinsGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (event.getClickedInventory() == null) {
                return;
            }

            InventoryHolder holder = event.getView().getTopInventory().getHolder();
            if (holder instanceof SkinsGUI) {
                if (event.getClickedInventory().getHolder() == holder) {
                    try {
                        ((SkinsGUI) holder).onClick(event);
                    } catch (Exception e) { // Ensure event always cancels
                        e.printStackTrace();
                    }
                }

                event.setCancelled(true);
            }
        } catch (NoSuchMethodError ignored) {
            // Bukkit 1.8.8
            if (event.getSlotType() != InventoryType.SlotType.CONTAINER) {
                return;
            }

            Inventory destInvent = event.getInventory();
            int slotClicked = event.getRawSlot();
            if (slotClicked < destInvent.getSize()) { // Check if slot clicked was container
                InventoryHolder holder = destInvent.getHolder();
                if (holder instanceof SkinsGUI) {
                    try {
                        ((SkinsGUI) holder).onClick(event);
                    } catch (Exception e) { // Ensure event always cancels
                        e.printStackTrace();
                    }

                    event.setCancelled(true);
                }
            }
        }
    }
}
