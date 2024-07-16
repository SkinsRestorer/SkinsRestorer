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
package net.skinsrestorer.bukkit.gui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.shared.gui.ClickEventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class BukkitGUIHolder implements InventoryHolder {
    private final Map<Integer, ClickEventHandler> handlers = new HashMap<>();
    @Setter
    private Inventory inventory;

    public void onClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();

        // Cancel invalid items
        if (currentItem == null || !currentItem.hasItemMeta()) {
            return;
        }

        ClickEventHandler handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.handle(switch (event.getClick()) {
                case LEFT -> ClickEventHandler.ClickEventType.LEFT;
                case RIGHT -> ClickEventHandler.ClickEventType.RIGHT;
                case MIDDLE -> ClickEventHandler.ClickEventType.MIDDLE;
                default -> ClickEventHandler.ClickEventType.OTHER;
            });
        }
    }
}
