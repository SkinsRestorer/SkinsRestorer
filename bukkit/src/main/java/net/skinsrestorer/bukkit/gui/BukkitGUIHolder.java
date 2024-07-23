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
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.gui.ActionDataCallback;
import net.skinsrestorer.shared.gui.ClickEventType;
import net.skinsrestorer.shared.gui.SRInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitGUIHolder implements InventoryHolder {
    private final ActionDataCallback dataCallback;
    private final WrapperBukkit wrapper;
    private final Map<Integer, Map<ClickEventType, SRInventory.ClickEventAction>> handlers = new HashMap<>();
    @Setter
    private Inventory inventory;

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Map<ClickEventType, SRInventory.ClickEventAction> handlers = this.handlers.get(event.getRawSlot());
        if (handlers != null) {
            SRInventory.ClickEventAction action = handlers.get(switch (event.getClick()) {
                case LEFT -> ClickEventType.LEFT;
                case RIGHT -> ClickEventType.RIGHT;
                case SHIFT_LEFT -> ClickEventType.SHIFT_LEFT;
                default -> ClickEventType.OTHER;
            });
            if (action != null) {
                dataCallback.handle(wrapper.player(player), action);
            }
        }
    }
}
