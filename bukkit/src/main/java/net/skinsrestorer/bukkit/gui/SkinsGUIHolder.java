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
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.utils.AuthLibHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Optional;
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
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        // Cancel invalid items
        if (currentItem == null || !currentItem.hasItemMeta()) {
            return;
        }

        String skinName;
        if (currentItem.getItemMeta() instanceof SkullMeta skullMeta) {
            GameProfile gameProfile = XSkull.of(skullMeta).getProfile();
            Optional<String> skinNameProperty = gameProfile.getProperties().values().stream()
                    .filter(p -> AuthLibHelper.getPropertyName(p).equals(SharedGUI.SR_PROPERTY_INTERNAL_NAME))
                    .map(AuthLibHelper::getPropertyValue)
                    .findFirst();

            if (skinNameProperty.isEmpty()) {
                return;
            }

            skinName = skinNameProperty.get();
        } else {
            skinName = null;
        }

        callback.accept(new ClickEventInfo(getMaterialType(XMaterial.matchXMaterial(currentItem)), skinName, wrapper.player(player), page));
    }

    private ClickEventInfo.MaterialType getMaterialType(XMaterial material) {
        return switch (material) {
            case PLAYER_HEAD -> ClickEventInfo.MaterialType.HEAD;
            case RED_STAINED_GLASS_PANE -> ClickEventInfo.MaterialType.RED_PANE;
            case GREEN_STAINED_GLASS_PANE -> ClickEventInfo.MaterialType.GREEN_PANE;
            case YELLOW_STAINED_GLASS_PANE -> ClickEventInfo.MaterialType.YELLOW_PANE;
            default -> ClickEventInfo.MaterialType.UNKNOWN;
        };
    }
}
