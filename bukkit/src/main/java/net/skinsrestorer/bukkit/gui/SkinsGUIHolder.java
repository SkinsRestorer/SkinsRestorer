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
package net.skinsrestorer.bukkit.gui;

import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class SkinsGUIHolder implements InventoryHolder {
    private final int page; // Page number start with 0
    private final Consumer<ClickEventInfo> callback;
    private final WrapperBukkit wrapper;
    @Getter
    @Setter
    private Inventory inventory;

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        // Cancel invalid items
        if (currentItem == null || !currentItem.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = currentItem.getItemMeta();

        if (itemMeta == null) {
            return;
        }

        callback.accept(new ClickEventInfo(getMaterialType(XMaterial.matchXMaterial(currentItem)), itemMeta.getDisplayName(), wrapper.player(player), page));
    }

    private ClickEventInfo.MaterialType getMaterialType(XMaterial material) {
        switch (material) {
            case PLAYER_HEAD:
                return ClickEventInfo.MaterialType.HEAD;
            case RED_STAINED_GLASS_PANE:
                return ClickEventInfo.MaterialType.RED_PANE;
            case GREEN_STAINED_GLASS_PANE:
                return ClickEventInfo.MaterialType.GREEN_PANE;
            case YELLOW_STAINED_GLASS_PANE:
                return ClickEventInfo.MaterialType.YELLOW_PANE;
            default:
                return ClickEventInfo.MaterialType.UNKNOWN;
        }
    }
}
