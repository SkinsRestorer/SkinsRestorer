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
package net.skinsrestorer.shared.gui;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.ComponentHelper;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedGUI {
    private final Injector injector;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SRCommandManager commandManager;
    private final SRServerPlugin plugin;
    private final SRServerAdapter adapter;
    public static final int HEAD_COUNT_PER_PAGE = 9 * 4;

    public SRInventory createGUIPage(SRServerPlayer player, PageInfo pageInfo) {
        Map<Integer, SRInventory.Item> items = new HashMap<>();

        SRInventory.Item none = new SRInventory.Item(
                SRInventory.MaterialType.WHITE_PANE,
                ComponentHelper.convertPlainToJson(" "),
                List.of(),
                null,
                ClickEventHandler.empty()
        );
        SRInventory.Item previous = new SRInventory.Item(
                SRInventory.MaterialType.YELLOW_PANE,
                locale.getMessageRequired(player, Message.SKINSMENU_PREVIOUS_PAGE),
                List.of(),
                null,
                type -> {
                    if (type == ClickEventHandler.ClickEventType.LEFT) {
                        if (plugin.isProxyMode()) {
                            adapter.runAsync(() -> player.requestSkinsFromProxy(pageInfo.page() - 1, pageInfo.pageType()));
                        } else {
                            SkinStorageImpl skinStorage = injector.getSingleton(SkinStorageImpl.class);
                            adapter.runAsync(() -> adapter.openGUI(player, createGUIPage(player, skinStorage.getGUIPage(player, pageInfo.page() - 1, pageInfo.pageType()))));
                        }
                    }
                }
        );
        SRInventory.Item delete = new SRInventory.Item(
                SRInventory.MaterialType.RED_PANE,
                locale.getMessageRequired(player, Message.SKINSMENU_CLEAR_SKIN),
                List.of(),
                null,
                type -> {
                    if (type == ClickEventHandler.ClickEventType.LEFT) {
                        if (plugin.isProxyMode()) {
                            adapter.runAsync(() -> player.sendToMessageChannel(out ->
                                    out.writeUTF("clearSkinV2")));
                            player.closeInventory();
                        } else {
                            commandManager.execute(player, "skin clear");
                            player.closeInventory();
                        }
                    }
                }
        );
        SRInventory.Item next = new SRInventory.Item(
                SRInventory.MaterialType.GREEN_PANE,
                locale.getMessageRequired(player, Message.SKINSMENU_NEXT_PAGE),
                List.of(),
                null,
                type -> {
                    if (type == ClickEventHandler.ClickEventType.LEFT) {
                        if (plugin.isProxyMode()) {
                            adapter.runAsync(() -> player.requestSkinsFromProxy(pageInfo.page() + 1, pageInfo.pageType()));
                        } else {
                            SkinStorageImpl skinStorage = injector.getSingleton(SkinStorageImpl.class);
                            adapter.runAsync(() -> adapter.openGUI(player, createGUIPage(player, skinStorage.getGUIPage(player, pageInfo.page() + 1, pageInfo.pageType()))));
                        }
                    }
                }
        );

        int skinCount = 0;
        for (GUISkinEntry entry : pageInfo.skinList()) {
            if (skinCount >= SharedGUI.HEAD_COUNT_PER_PAGE) {
                logger.warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            items.put(skinCount, new SRInventory.Item(
                    SRInventory.MaterialType.SKULL,
                    ComponentHelper.convertPlainToJson(entry.base().skinName()),
                    entry.lore(),
                    entry.base().textureHash(),
                    type -> {
                        if (type == ClickEventHandler.ClickEventType.LEFT) {
                            if (plugin.isProxyMode()) {
                                adapter.runAsync(() -> player.sendToMessageChannel(out -> {
                                    out.writeUTF("setSkinV2");
                                    out.writeUTF(entry.base().skinId());
                                }));
                                player.closeInventory();
                            } else {
                                commandManager.execute(player, "skin set " + entry.base().skinId());
                                player.closeInventory();
                            }
                        }
                    }
            ));
            skinCount++;
        }

        // White Glass line
        items.put(36, none);
        items.put(37, none);
        items.put(38, none);
        items.put(39, none);
        items.put(40, none);
        items.put(41, none);
        items.put(42, none);
        items.put(43, none);
        items.put(44, none);

        // If page is above starting page (0), add previous button
        if (pageInfo.hasPrevious()) {
            items.put(45, previous);
            items.put(46, previous);
            items.put(47, previous);
        } else {
            // Empty place previous
            items.put(45, none);
            items.put(46, none);
            items.put(47, none);
        }

        // Middle button //remove skin
        items.put(48, delete);
        items.put(49, delete);
        items.put(50, delete);

        // If the page is full, adding Next Page button.
        if (pageInfo.hasNext()) {
            items.put(51, next);
            items.put(52, next);
            items.put(53, next);
        } else {
            // Empty place next
            items.put(51, none);
            items.put(52, none);
            items.put(53, none);
        }

        return new SRInventory(
                6,
                locale.getMessageRequired(player, Message.SKINSMENU_TITLE_NEW,
                        Placeholder.parsed("page_number", String.valueOf(pageInfo.page() + 1))),
                items);
    }
}
