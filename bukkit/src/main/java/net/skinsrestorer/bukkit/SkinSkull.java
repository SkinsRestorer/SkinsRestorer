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
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class SkinSkull extends ItemStack {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public static boolean giveSkull(SkinsRestorer plugin, Player player, String name, String b64stringTexture) {
        try {
            HashMap<Integer, ItemStack> fullInventory = player.getInventory().addItem(createSkull(plugin, name, b64stringTexture));
            if (fullInventory.isEmpty()) {
                player.sendMessage("Skull given"); //todo: add to lang
            } else {
                player.sendMessage("Inventory is full, skull could not be given"); //todo: add to lang
                return true;
            }
        } catch (Exception e) {
            plugin.getSrLogger().warning("Error while giving skull to " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    private static ItemStack createSkull(SkinsRestorer plugin, String name, String b64stringTexture) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        //Objects.requireNonNull(sm).setDisplayName(name);

        List<String> lore = new ArrayList<>(); //todo: improve
        lore.add(C.c(String.valueOf(name + "'s Head")));
        sm.setLore(lore);

        is.setItemMeta(sm);

        try {
            BukkitHeadAPI.setSkull(is, b64stringTexture);
        } catch (Exception e) {
            plugin.getSrLogger().warning("ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            e.printStackTrace();
        }

        return is;
    }
}
