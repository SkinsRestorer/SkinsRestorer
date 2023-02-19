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

import co.aikar.commands.CommandManager;
import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.SRForeign;
import net.skinsrestorer.shared.interfaces.SRServerAdapter;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import javax.inject.Inject;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.function.Consumer;

import static net.skinsrestorer.shared.utils.FluentList.listOf;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinsGUI {
    private static final int HEAD_COUNT_PER_PAGE = 36;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final Server server;
    private final SkinStorageImpl skinStorage;

    public Inventory createGUI(Consumer<ClickEventInfo> callback, SRForeign player, int page) {
        if (page > 999) {
            page = 999;
        }

        int skinNumber = HEAD_COUNT_PER_PAGE * page;

        return createGUI(callback, player, page, skinStorage.getSkins(skinNumber));
    }

    public Inventory createGUI(Consumer<ClickEventInfo> callback, SRForeign player, int page, Map<String, String> skinsList) {
        SkinsGUIHolder instance = new SkinsGUIHolder(page, callback);
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
        ItemStack itemStack = XMaterial.PLAYER_HEAD.parseItem();

        if (itemStack == null) {
            throw new IllegalStateException("Could not create skull for " + name + "!");
        }

        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        if (skullMeta == null) {
            throw new IllegalStateException("Could not create skull for " + name + "!");
        }

        skullMeta.setDisplayName(name);
        skullMeta.setLore(listOf(locale.getMessage(player, Message.SKINSMENU_SELECT_SKIN)));

        try {
            SkullUtils.applySkin(skullMeta, property);
        } catch (AssertionError e) {
            log.warning(String.format("Could not add '%s' to SkinsGUI, skin might be corrupted or invalid!", name));
            e.printStackTrace();
        }

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    private static ItemStack createGlass(GlassType type, SRForeign player, SkinsRestorerLocale locale) {
        ItemStack itemStack = type.getMaterial().parseItem();

        if (itemStack == null) {
            throw new IllegalStateException("Could not create glass for " + type.name() + "!");
        }

        String text = type.getMessage() == null ? " " : locale.getMessage(player, type.getMessage());

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) {
            throw new IllegalStateException("Could not create glass for " + type.name() + "!");
        }

        itemMeta.setDisplayName(text);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    @Getter
    @RequiredArgsConstructor
    private enum GlassType {
        NONE(XMaterial.WHITE_STAINED_GLASS_PANE, null),
        PREV(XMaterial.YELLOW_STAINED_GLASS_PANE, Message.SKINSMENU_PREVIOUS_PAGE),
        NEXT(XMaterial.GREEN_STAINED_GLASS_PANE, Message.SKINSMENU_NEXT_PAGE),
        DELETE(XMaterial.RED_STAINED_GLASS_PANE, Message.SKINSMENU_CLEAR_SKIN);

        private final XMaterial material;
        private final Message message;
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ServerGUIActions implements Consumer<ClickEventInfo> {
        private final SRServerAdapter adapter;
        private final WrapperBukkit wrapper;
        private final CommandManager<?, ?, ?, ?, ?, ?> commandManager;
        private final SkinsGUI gui;

        @Override
        public void accept(ClickEventInfo event) {
            switch (event.getMaterial()) {
                case PLAYER_HEAD:
                    adapter.runAsync(() -> {
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
                    adapter.runAsync(() -> {
                        Inventory newInventory = gui.createGUI(this, wrapper.player(event.getPlayer()), event.getCurrentPage() + 1);

                        adapter.runSync(() ->
                                event.getPlayer().openInventory(newInventory));
                    });
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    adapter.runAsync(() -> {
                        Inventory newInventory = gui.createGUI(this, wrapper.player(event.getPlayer()), event.getCurrentPage() - 1);

                        adapter.runSync(() ->
                                event.getPlayer().openInventory(newInventory));
                    });
                    break;
                default:
                    break;
            }
        }
    }

    @RequiredArgsConstructor(onConstructor_ = @Inject)
    public static class ProxyGUIActions implements Consumer<ClickEventInfo> {
        private final SRBukkitAdapter adapter;

        @Override
        public void accept(ClickEventInfo event) {
            Player player = event.getPlayer();
            switch (event.getMaterial()) {
                case PLAYER_HEAD:
                    String skinName = event.getItemMeta().getDisplayName();
                    adapter.runAsync(() -> adapter.sendToMessageChannel(player, out -> {
                        out.writeUTF("setSkin");
                        out.writeUTF(player.getName());
                        out.writeUTF(skinName);
                    }));
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    adapter.runAsync(() -> adapter.sendToMessageChannel(player, out -> {
                        out.writeUTF("clearSkin");
                        out.writeUTF(player.getName());
                    }));
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    adapter.runAsync(() ->
                            adapter.requestSkinsFromProxy(player, event.getCurrentPage() + 1));
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    adapter.runAsync(() ->
                            adapter.requestSkinsFromProxy(player, event.getCurrentPage() - 1));
                    break;
                default:
                    break;
            }
        }
    }
}
