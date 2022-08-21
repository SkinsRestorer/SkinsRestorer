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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.bukkit.BukkitHeadAPI;
import net.skinsrestorer.api.interfaces.ISRForeign;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.skinsrestorer.bukkit.utils.WrapperBukkit.wrapPlayer;

@RequiredArgsConstructor
public class SkinsGUI implements InventoryHolder {
    private static final int HEAD_COUNT_PER_PAGE = 36;
    private final SkinsRestorer plugin;
    private final int page; // Page number start with 0
    @Getter
    @Setter
    private Inventory inventory;

    public static Inventory createGUI(SkinsRestorer plugin, ISRForeign player, int page, Map<String, String> skinsList) {
        SkinsGUI instance = new SkinsGUI(plugin, page);
        Inventory inventory = Bukkit.createInventory(instance, 54, C.c(SkinsRestorerAPI.getApi().getMessage(player, Locale.SKINSMENU_TITLE_NEW, String.valueOf(page + 1))));
        instance.setInventory(inventory);

        ItemStack none = createGlass(GlassType.NONE, player);
        ItemStack delete = createGlass(GlassType.DELETE, player);
        ItemStack prev = createGlass(GlassType.PREV, player);
        ItemStack next = createGlass(GlassType.NEXT, player);

        int skinCount = 0;
        for (Map.Entry<String, String> entry : skinsList.entrySet()) {
            if (skinCount >= HEAD_COUNT_PER_PAGE) {
                plugin.getSrLogger().warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            if (CharBuffer.wrap(entry.getKey().toCharArray()).chars().anyMatch(i -> Character.isLetter(i) && Character.isUpperCase(i))) {
                plugin.getSrLogger().info("ERROR: skin " + entry.getKey() + ".skin contains a Upper case!");
                plugin.getSrLogger().info("Please rename the file name to a lower case!.");
                continue;
            }

            inventory.addItem(createSkull(plugin.getSrLogger(), player, entry.getKey(), entry.getValue()));
            skinCount++;
        }

        // White Glass line
        inventory.setItem(36, none);
        inventory.setItem(37, none);
        inventory.setItem(38, none);
        inventory.setItem(39, none);
        inventory.setItem(40, none);
        inventory.setItem(41, none);
        inventory.setItem(42, none);
        inventory.setItem(43, none);
        inventory.setItem(44, none);

        // If page is above starting page (0), add previous button
        if (page > 0) {
            inventory.setItem(45, prev);
            inventory.setItem(46, prev);
            inventory.setItem(47, prev);
        } else {
            // Empty place previous
            inventory.setItem(45, none);
            inventory.setItem(46, none);
            inventory.setItem(47, none);
        }

        // Middle button //remove skin
        inventory.setItem(48, delete);
        inventory.setItem(49, delete);
        inventory.setItem(50, delete);

        // If the page is full, adding Next Page button.
        if (page < 999 && inventory.firstEmpty() > 50) {
            inventory.setItem(51, next);
            inventory.setItem(52, next);
            inventory.setItem(53, next);
        } else {
            // Empty place next
            inventory.setItem(51, none);
            inventory.setItem(52, none);
            inventory.setItem(53, none);
        }

        return inventory;
    }

    public static Inventory createGUI(SkinsRestorer plugin, ISRForeign player, int page) {
        if (page > 999)
            page = 999;
        int skinNumber = HEAD_COUNT_PER_PAGE * page;

        return createGUI(plugin, player, page, plugin.getSkinStorage().getSkins(skinNumber));
    }

    private static ItemStack createSkull(SRLogger log, ISRForeign player, String name, String property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(C.c(SkinsRestorerAPI.getApi().getMessage(player, Locale.SKINSMENU_SELECT_SKIN)));
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

    private static ItemStack createGlass(GlassType type, ISRForeign player) {
        ItemStack itemStack;
        String text;
        switch (type) {
            case NONE:
                itemStack = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
                text = " ";
                break;
            case PREV:
                itemStack = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
                text = C.c(SkinsRestorerAPI.getApi().getMessage(player, Locale.SKINSMENU_PREVIOUS_PAGE));
                break;
            case NEXT:
                itemStack = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
                text = C.c(SkinsRestorerAPI.getApi().getMessage(player, Locale.SKINSMENU_NEXT_PAGE));
                break;
            case DELETE:
                itemStack = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
                text = C.c(SkinsRestorerAPI.getApi().getMessage(player, Locale.SKINSMENU_CLEAR_SKIN));
                break;
            default:
                throw new IllegalArgumentException("Unknown glass type: " + type);
        }

        ItemMeta itemMeta = Objects.requireNonNull(itemStack).getItemMeta();
        Objects.requireNonNull(itemMeta).setDisplayName(text);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null) // Cancel if not a player or if the item is null
            return;

        final Player player = (Player) event.getWhoClicked();
        final ItemStack currentItem = event.getCurrentItem();

        // Cancel invalid items
        if (!currentItem.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = currentItem.getItemMeta();
        assert itemMeta != null;

        if (plugin.isProxyMode()) {
            switch (XMaterial.matchXMaterial(currentItem)) {
                case PLAYER_HEAD:
                    plugin.runAsync(() -> {
                        String skin = itemMeta.getDisplayName();
                        plugin.requestSkinSetFromBungeeCord(player, skin);
                    });
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    plugin.runAsync(() ->
                            plugin.requestSkinClearFromBungeeCord(player));
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    plugin.runAsync(() ->
                            plugin.requestSkinsFromBungeeCord(player, page + 1));
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    plugin.runAsync(() ->
                            plugin.requestSkinsFromBungeeCord(player, page - 1));
                    break;
                default:
                    break;
            }
        } else {
            switch (XMaterial.matchXMaterial(currentItem)) {
                case PLAYER_HEAD:
                    plugin.runAsync(() -> {
                        final String skinName = itemMeta.getDisplayName();
                        plugin.getSkinCommand().onSkinSetShort(player, skinName);
                    });
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    plugin.getSkinCommand().onSkinClear(player);
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    plugin.runAsync(() -> {
                        Inventory newInventory = createGUI(plugin, wrapPlayer(player), page + 1);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                                player.openInventory(newInventory));
                    });
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    plugin.runAsync(() -> {
                        Inventory newInventory = createGUI(plugin, wrapPlayer(player), page - 1);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                                player.openInventory(newInventory));
                    });
                    break;
                default:
                    break;
            }
        }
    }

    private enum GlassType {
        NONE, PREV, NEXT, DELETE
    }
}