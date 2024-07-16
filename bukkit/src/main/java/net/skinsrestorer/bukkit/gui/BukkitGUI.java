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

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.utils.ComponentHelper;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitGUI implements GUIManager<Inventory> {
    private final Server server;

    private ItemStack createItem(SRInventory.Item entry) {
        XMaterial material = switch (entry.materialType()) {
            case SKULL -> XMaterial.PLAYER_HEAD;
            case WHITE_PANE -> XMaterial.WHITE_STAINED_GLASS_PANE;
            case YELLOW_PANE -> XMaterial.YELLOW_STAINED_GLASS_PANE;
            case RED_PANE -> XMaterial.RED_STAINED_GLASS_PANE;
            case GREEN_PANE -> XMaterial.GREEN_STAINED_GLASS_PANE;
        };
        ItemStack itemStack = Objects.requireNonNull(material.parseItem());
        if (entry.textureHash() != null) {
            XSkull.of(itemStack)
                    .profile(Profileable.of(Objects.requireNonNull(ProfileInputType.typeOf(entry.textureHash())), entry.textureHash()))
                    .apply();
        }

        ItemMeta skullMeta = Objects.requireNonNull(itemStack.getItemMeta());
        skullMeta.setDisplayName(ComponentHelper.convertJsonToLegacy(entry.displayName()));
        skullMeta.setLore(entry.lore().stream().map(ComponentHelper::convertJsonToLegacy).toList());

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    public Inventory createGUI(SRInventory srInventory) {
        BukkitGUIHolder instance = new BukkitGUIHolder();
        Inventory inventory = server.createInventory(instance, srInventory.rows() * 9,
                ComponentHelper.convertJsonToLegacy(srInventory.title()));
        instance.setInventory(inventory);

        for (Map.Entry<Integer, SRInventory.Item> entry : srInventory.items().entrySet()) {
            inventory.setItem(entry.getKey(), createItem(entry.getValue()));
            instance.getHandlers().put(entry.getKey(), entry.getValue().clickEventHandler());
        }

        return inventory;
    }
}
