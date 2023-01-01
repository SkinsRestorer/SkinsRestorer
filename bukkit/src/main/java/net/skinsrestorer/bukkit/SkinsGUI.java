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

import co.aikar.commands.CommandManager;
import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.SRForeign;
import net.skinsrestorer.shared.interfaces.SRServerPlugin;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import javax.inject.Inject;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SkinsGUI implements InventoryHolder {
    private static final int HEAD_COUNT_PER_PAGE = 36;
    private final int page; // Page number start with 0
    private final Consumer<EventInfo> callback;
    @Getter
    @Setter(value = AccessLevel.PRIVATE)
    private Inventory inventory;

    public static Inventory createGUI(Consumer<EventInfo> callback, SkinsRestorerLocale locale, SRLogger logger, Server server, SkinStorage skinStorage, SRForeign player, int page) {
        if (page > 999) {
            page = 999;
        }

        int skinNumber = HEAD_COUNT_PER_PAGE * page;

        return createGUI(callback, locale, logger, server, player, page, skinStorage.getSkins(skinNumber));
    }

    public static Inventory createGUI(Consumer<EventInfo> callback, SkinsRestorerLocale locale, SRLogger logger, Server server, SRForeign player, int page, Map<String, String> skinsList) {
        SkinsGUI instance = new SkinsGUI(page, callback);
        Inventory inventory = server.createInventory(instance, 54, locale.getMessage(player, Message.SKINSMENU_TITLE_NEW, String.valueOf(page + 1)));
        instance.setInventory(inventory);

        ItemStack none = createGlass(GlassType.NONE, player, locale);
        ItemStack delete = createGlass(GlassType.DELETE, player, locale);
        ItemStack prev = createGlass(GlassType.PREV, player, locale);
        ItemStack next = createGlass(GlassType.NEXT, player, locale);

        int skinCount = 0;
        for (Map.Entry<String, String> entry : skinsList.entrySet()) {
            if (skinCount >= HEAD_COUNT_PER_PAGE) {
                logger.warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            if (CharBuffer.wrap(entry.getKey().toCharArray()).chars().anyMatch(i -> Character.isLetter(i) && Character.isUpperCase(i))) {
                logger.info("ERROR: skin " + entry.getKey() + ".skin contains a Upper case!");
                logger.info("Please rename the file name to a lower case!.");
                continue;
            }

            inventory.addItem(createSkull(logger, locale, player, entry.getKey(), entry.getValue()));
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

    private static ItemStack createSkull(SRLogger log, SkinsRestorerLocale locale, SRForeign player, String name, String property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(C.c(locale.getMessage(player, Message.SKINSMENU_SELECT_SKIN)));
        Objects.requireNonNull(sm).setDisplayName(name);
        sm.setLore(lore);

        try {
            SkullUtils.applySkin(sm, property);
        } catch (AssertionError e) {
            log.info("ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            e.printStackTrace();
        }

        is.setItemMeta(sm);

        return is;
    }

    private static ItemStack createGlass(GlassType type, SRForeign player, SkinsRestorerLocale locale) {
        ItemStack itemStack;
        String text;
        switch (type) {
            case NONE:
                itemStack = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
                text = " ";
                break;
            case PREV:
                itemStack = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
                text = locale.getMessage(player, Message.SKINSMENU_PREVIOUS_PAGE);
                break;
            case NEXT:
                itemStack = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
                text = locale.getMessage(player, Message.SKINSMENU_NEXT_PAGE);
                break;
            case DELETE:
                itemStack = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
                text = locale.getMessage(player, Message.SKINSMENU_CLEAR_SKIN);
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

        callback.accept(new EventInfo(XMaterial.matchXMaterial(currentItem), itemMeta, player, page));
    }

    private enum GlassType {
        NONE, PREV, NEXT, DELETE
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ServerGUIActions implements Consumer<EventInfo> {
        private final Server server;
        private final SRServerPlugin plugin;
        private final SkinsRestorerLocale locale;
        private final SRLogger logger;
        private final SkinStorage skinStorage;
        private final WrapperBukkit wrapper;
        private final CommandManager<?, ?, ?, ?, ?, ?> commandManager;

        @Override
        public void accept(EventInfo event) {
            switch (event.getMaterial()) {
                case PLAYER_HEAD:
                    plugin.runAsync(() -> {
                        String skin = event.getItemMeta().getDisplayName();
                        commandManager.getRootCommand("skin").execute(
                                commandManager.getCommandIssuer(event.getPlayer()), "skin", new String[]{"set", skin});
                    });
                    event.getPlayer().closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    commandManager.getRootCommand("skin").execute(
                            commandManager.getCommandIssuer(event.getPlayer()), "skin", new String[]{"clear"});
                    event.getPlayer().closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    plugin.runAsync(() -> {
                        Inventory newInventory = createGUI(this, locale, logger, server, skinStorage,
                                wrapper.player(event.getPlayer()), event.getCurrentPage() + 1);

                        plugin.runSync(() ->
                                event.getPlayer().openInventory(newInventory));
                    });
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    plugin.runAsync(() -> {
                        Inventory newInventory = createGUI(this, locale, logger, server, skinStorage,
                                wrapper.player(event.getPlayer()), event.getCurrentPage() - 1);

                        plugin.runSync(() ->
                                event.getPlayer().openInventory(newInventory));
                    });
                    break;
                default:
                    break;
            }
        }
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ProxyGUIActions implements Consumer<EventInfo> {
        private final SkinsRestorerBukkit plugin;

        @Override
        public void accept(EventInfo event) {
            Player player = event.getPlayer();
            switch (event.getMaterial()) {
                case PLAYER_HEAD:
                    String skinName = event.getItemMeta().getDisplayName();
                    plugin.runAsync(() -> plugin.sendToMessageChannel(player, out -> {
                        out.writeUTF("setSkin");
                        out.writeUTF(player.getName());
                        out.writeUTF(skinName);
                    }));
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    plugin.runAsync(() -> plugin.sendToMessageChannel(player, out -> {
                        out.writeUTF("clearSkin");
                        out.writeUTF(player.getName());
                    }));
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    plugin.runAsync(() ->
                            plugin.requestSkinsFromProxy(player, event.getCurrentPage() + 1));
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    plugin.runAsync(() ->
                            plugin.requestSkinsFromProxy(player, event.getCurrentPage() - 1));
                    break;
                default:
                    break;
            }
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class EventInfo {
        private final XMaterial material;
        private final ItemMeta itemMeta;
        private final Player player;
        private final int currentPage;
    }
}
