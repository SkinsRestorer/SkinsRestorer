/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.sponge.gui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.sponge.SRSpongeAdapter;
import net.skinsrestorer.sponge.wrapper.WrapperSponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;

import javax.inject.Inject;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static net.skinsrestorer.shared.utils.FluentList.listOf;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinsGUI implements GUIManager<InventoryMenu> {
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SRSpongeAdapter adapter;
    private final WrapperSponge wrapper;

    private static ItemStack createSkull(SkinsRestorerLocale locale, SRForeign player, String name, String property) {
        return ItemStack.builder()
                .itemType(ItemTypes.PLAYER_HEAD)
                .add(Keys.LORE, listOf(Component.text(locale.getMessage(player, Message.SKINSMENU_SELECT_SKIN))))
                .add(Keys.GAME_PROFILE, GameProfile.of(UUID.randomUUID(), null).withProperty(ProfileProperty.of(SkinProperty.TEXTURES_NAME, property)))
                .add(Keys.CUSTOM_NAME, Component.text(name))
                .build();
    }

    private static ItemStack createGlass(GlassType type, SRForeign player, SkinsRestorerLocale locale) {
        return ItemStack.builder()
                .itemType(type.getMaterial())
                .add(Keys.CUSTOM_NAME, Component.text(type.getMessage() == null ? " " : locale.getMessage(player, type.getMessage())))
                .build();
    }

    public InventoryMenu createGUI(Consumer<ClickEventInfo> callback, SRForeign player, int page, Map<String, String> skinsList) {
        ViewableInventory inventory = ViewableInventory.builder()
                .type(ContainerTypes.GENERIC_9X6)
                .completeStructure()
                .plugin(adapter.getPluginContainer())
                .build();

        ItemStack none = createGlass(GlassType.NONE, player, locale);
        ItemStack delete = createGlass(GlassType.DELETE, player, locale);
        ItemStack prev = createGlass(GlassType.PREV, player, locale);
        ItemStack next = createGlass(GlassType.NEXT, player, locale);

        int skinCount = 0;
        for (Map.Entry<String, String> entry : skinsList.entrySet()) {
            if (skinCount >= SharedGUI.HEAD_COUNT_PER_PAGE) {
                logger.warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            if (CharBuffer.wrap(entry.getKey().toCharArray()).chars().anyMatch(i -> Character.isLetter(i) && Character.isUpperCase(i))) {
                logger.info("ERROR: skin " + entry.getKey() + ".skin contains a Upper case!");
                logger.info("Please rename the file name to a lower case!.");
                continue;
            }

            inventory.set(skinCount, createSkull(locale, player, entry.getKey(), entry.getValue()));
            skinCount++;
        }

        // White Glass line
        inventory.set(36, none);
        inventory.set(37, none);
        inventory.set(38, none);
        inventory.set(39, none);
        inventory.set(40, none);
        inventory.set(41, none);
        inventory.set(42, none);
        inventory.set(43, none);
        inventory.set(44, none);

        // If page is above starting page (0), add previous button
        if (page > 0) {
            inventory.set(45, prev);
            inventory.set(46, prev);
            inventory.set(47, prev);
        } else {
            // Empty place previous
            inventory.set(45, none);
            inventory.set(46, none);
            inventory.set(47, none);
        }

        // Middle button //remove skin
        inventory.set(48, delete);
        inventory.set(49, delete);
        inventory.set(50, delete);

        // If the page is full, adding Next Page button.
        if (page < 999 && skinCount <= SharedGUI.HEAD_COUNT_PER_PAGE) {
            inventory.set(51, next);
            inventory.set(52, next);
            inventory.set(53, next);
        } else {
            // Empty place next
            inventory.set(51, none);
            inventory.set(52, none);
            inventory.set(53, none);
        }

        InventoryMenu menu = inventory.asMenu();

        menu.setTitle(Component.text(locale.getMessage(player, Message.SKINSMENU_TITLE_NEW, String.valueOf(page + 1))));
        menu.setReadOnly(true);
        menu.registerSlotClick(new GUIListener(callback, page, wrapper));

        return menu;
    }

    @Getter
    @RequiredArgsConstructor
    private enum GlassType {
        NONE(ItemTypes.WHITE_STAINED_GLASS_PANE.get(), null),
        PREV(ItemTypes.YELLOW_STAINED_GLASS_PANE.get(), Message.SKINSMENU_PREVIOUS_PAGE),
        NEXT(ItemTypes.GREEN_STAINED_GLASS.get(), Message.SKINSMENU_NEXT_PAGE),
        DELETE(ItemTypes.RED_STAINED_GLASS_PANE.get(), Message.SKINSMENU_CLEAR_SKIN);

        private final ItemType material;
        private final Message message;
    }
}
