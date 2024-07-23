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

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SharedGUI {
    public static final int HEAD_COUNT_PER_PAGE = 9 * 4;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;

    public SRInventory createGUIPage(SRPlayer player, PageInfo pageInfo) {
        Map<Integer, SRInventory.Item> items = new HashMap<>();

        int skinCount = 0;
        for (GUISkinEntry entry : pageInfo.skinList()) {
            if (skinCount >= SharedGUI.HEAD_COUNT_PER_PAGE) {
                logger.warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            items.put(skinCount, new SRInventory.Item(
                    SRInventory.MaterialType.SKULL,
                    entry.base().skinName(),
                    entry.lore(),
                    Optional.of(entry.base().textureHash()),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.SetSkinPayload(
                                    entry.base().skinId()
                            )), true))
                    )
            ));
            skinCount++;
        }

        if (pageInfo.hasPrevious()) {
            items.put(48, new SRInventory.Item(
                    SRInventory.MaterialType.ARROW,
                    locale.getMessageRequired(player, Message.SKINSMENU_PREVIOUS_PAGE),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    pageInfo.page() - 1, pageInfo.pageType()
                            )), false))
                    )
            ));
        } else if (pageInfo.pageType() != PageType.SELECT) {
            items.put(48, new SRInventory.Item(
                    SRInventory.MaterialType.ARROW,
                    locale.getMessageRequired(player, Message.SKINSMENU_BACK_SELECT_BUTTON),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    0, PageType.SELECT
                            )), false))
                    )
            ));
        }

        items.put(49, new SRInventory.Item(
                SRInventory.MaterialType.BARRIER,
                locale.getMessageRequired(player, Message.SKINSMENU_CLEAR_SKIN),
                List.of(),
                Optional.empty(),
                false,
                Map.ofEntries(
                        Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.ClearSkinPayload()), true))
                )
        ));

        if (pageInfo.hasNext()) {
            items.put(50, new SRInventory.Item(
                    SRInventory.MaterialType.ARROW,
                    locale.getMessageRequired(player, Message.SKINSMENU_NEXT_PAGE),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    pageInfo.page() + 1, pageInfo.pageType()
                            )), false))
                    )
            ));
        }

        if (pageInfo.pageType() == PageType.SELECT) {
            items.put(20, new SRInventory.Item(
                    SRInventory.MaterialType.BOOKSHELF,
                    locale.getMessageRequired(player, Message.SKINSMENU_MAIN_BUTTON),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    0, PageType.MAIN
                            )), false))
                    )
            ));
            items.put(22, new SRInventory.Item(
                    SRInventory.MaterialType.ENDER_EYE,
                    locale.getMessageRequired(player, Message.SKINSMENU_HISTORY_BUTTON),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    0, PageType.HISTORY
                            )), false))
                    )
            ));
            items.put(24, new SRInventory.Item(
                    SRInventory.MaterialType.ENCHANTING_TABLE,
                    locale.getMessageRequired(player, Message.SKINSMENU_FAVOURITES_BUTTON),
                    List.of(),
                    Optional.empty(),
                    false,
                    Map.ofEntries(
                            Map.entry(ClickEventType.LEFT, new SRInventory.ClickEventAction(new SRProxyPluginMessage.GUIActionChannelPayload(new SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload(
                                    0, PageType.FAVOURITES
                            )), false))
                    )
            ));
        }

        return new SRInventory(
                6,
                locale.getMessageRequired(player, pageInfo.pageType().getTitle(),
                        Placeholder.parsed("page_number", String.valueOf(pageInfo.page() + 1))),
                items);
    }
}
