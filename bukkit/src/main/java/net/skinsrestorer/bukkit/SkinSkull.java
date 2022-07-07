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
package net.skinsrestorer.bukkit;

import com.cryptomorin.xseries.XMaterial;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.bukkit.BukkitHeadAPI;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class SkinSkull extends ItemStack {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public boolean giveSkull(Player player, String name, IProperty property) {
        player.getInventory().addItem(createSkull(name, property));
        //todo: add catch for if inventory is full
        return true;
    }

    private ItemStack createSkull(String name, IProperty property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(C.c(Locale.SKINSMENU_SELECT_SKIN));
        Objects.requireNonNull(sm).setDisplayName(name);
        sm.setLore(lore);
        is.setItemMeta(sm);

        try {
            BukkitHeadAPI.setSkull(is, property);
        } catch (Exception e) {
            log.info("ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            e.printStackTrace();
        }

        return is;
    }
}
