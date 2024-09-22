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

import ch.jalu.injector.Injector;
import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.wrapper.BukkitComponentHelper;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.SRInventory;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BukkitGUI implements GUIManager<Inventory> {
    private final Injector injector;
    private final Server server;

    @SuppressWarnings("UnstableApiUsage")
    private ItemStack createItem(SRInventory.Item entry) {
        XMaterial material = switch (entry.materialType()) {
            case SKULL -> XMaterial.PLAYER_HEAD;
            case ARROW -> XMaterial.ARROW;
            case BARRIER -> XMaterial.BARRIER.or(XMaterial.RED_WOOL);
            case BOOKSHELF -> XMaterial.BOOKSHELF;
            case ENDER_EYE -> XMaterial.ENDER_EYE;
            case ENCHANTING_TABLE -> XMaterial.ENCHANTING_TABLE;
        };
        ItemStack itemStack = Objects.requireNonNull(material.parseItem());
        entry.textureHash().ifPresent(hash -> {
            ItemMeta skullMeta = Objects.requireNonNull(itemStack.getItemMeta());

            GameProfile profile = Profileable.of(Objects.requireNonNull(ProfileInputType.typeOf(hash)), hash).getProfile();
            try {
                // Some versions require this method to be called instead of setting the field directly (early 1.20.4)
                Method setProfileMethod = Objects.requireNonNull(skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class));
                setProfileMethod.setAccessible(true);
                setProfileMethod.invoke(skullMeta, profile);
            } catch (ReflectiveOperationException e) {
                try {
                    // Fallback for versions without the above method
                    Field profileField = Objects.requireNonNull(skullMeta.getClass().getDeclaredField("profile"));
                    profileField.setAccessible(true);
                    profileField.set(skullMeta, profile);
                } catch (ReflectiveOperationException e2) {
                    throw new RuntimeException(e2);
                }
            }

            itemStack.setItemMeta(skullMeta);
        });

        ItemMeta skullMeta = Objects.requireNonNull(itemStack.getItemMeta());
        skullMeta.setDisplayName(BukkitComponentHelper.toStupidHex(entry.displayName()));
        skullMeta.setLore(entry.lore().stream().map(BukkitComponentHelper::toStupidHex).toList());
        if (entry.enchantmentGlow()) {
            skullMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            skullMeta.addEnchant(Objects.requireNonNull(XEnchantment.LURE.getEnchant()), 1, true);
        }

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    public Inventory createGUI(SRInventory srInventory) {
        BukkitGUIHolder instance = injector.newInstance(BukkitGUIHolder.class);
        Inventory inventory = server.createInventory(instance, srInventory.rows() * 9,
                BukkitComponentHelper.toStupidHex(srInventory.title()));
        instance.setInventory(inventory);

        for (Map.Entry<Integer, SRInventory.Item> entry : srInventory.items().entrySet()) {
            inventory.setItem(entry.getKey(), createItem(entry.getValue()));
            instance.getHandlers().put(entry.getKey(), entry.getValue().clickHandlers());
        }

        return inventory;
    }
}
