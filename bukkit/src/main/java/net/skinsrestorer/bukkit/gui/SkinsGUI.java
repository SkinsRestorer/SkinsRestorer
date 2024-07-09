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
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.GUISkinEntry;
import net.skinsrestorer.shared.gui.PageInfo;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.utils.ComponentHelper;
import org.bukkit.Server;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.inject.Inject;
import java.util.Objects;
import java.util.function.Consumer;

import static net.skinsrestorer.shared.utils.FluentList.of;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinsGUI implements GUIManager<Inventory> {
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final Server server;
    private final WrapperBukkit wrapper;

    private static void injectCustomInfo(GameProfile profile, String skinName) {
        PropertyMap properties = profile.getProperties();
        properties.removeAll(SharedGUI.SR_PROPERTY_INTERNAL_NAME);
        properties.put(SharedGUI.SR_PROPERTY_INTERNAL_NAME, new Property(SharedGUI.SR_PROPERTY_INTERNAL_NAME, skinName, null));
    }

    private ItemStack createSkull(SRForeign player, GUISkinEntry entry) {
        ItemStack itemStack = XSkull.createItem()
                .profile(Profileable.of(Objects.requireNonNull(ProfileInputType.typeOf(entry.textureHash())), entry.textureHash()))
                .apply();

        if (itemStack == null) {
            throw new IllegalStateException("Could not create skull for " + entry.skinId() + "!");
        }

        ItemMeta skullMeta = itemStack.getItemMeta();

        if (skullMeta == null) {
            throw new IllegalStateException("Could not create skull for " + entry.skinId() + "!");
        }

        skullMeta.setDisplayName(entry.skinName());
        skullMeta.setLore(of(ComponentHelper.convertJsonToLegacy(locale.getMessageRequired(player, Message.SKINSMENU_SELECT_SKIN))));

        injectCustomInfo(XSkull.of(itemStack).getProfile(), entry.skinId());

        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    private ItemStack createGlass(GlassType type, SRForeign player) {
        ItemStack itemStack = type.getMaterial().parseItem();

        if (itemStack == null) {
            throw new IllegalStateException("Could not create glass for " + type.name() + "!");
        }

        String text = type.getMessage() == null ? " " : ComponentHelper.convertJsonToLegacy(locale.getMessageRequired(player, type.getMessage()));

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta == null) {
            throw new IllegalStateException("Could not create glass for " + type.name() + "!");
        }

        itemMeta.setDisplayName(text);
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    public Inventory createGUI(Consumer<ClickEventInfo> callback, SRForeign player, PageInfo pageInfo) {
        SkinsGUIHolder instance = new SkinsGUIHolder(pageInfo.page(), callback, wrapper);
        Inventory inventory = server.createInventory(instance, 9 * 6, ComponentHelper.convertJsonToLegacy(
                locale.getMessageRequired(player, Message.SKINSMENU_TITLE_NEW,
                        Placeholder.parsed("page_number", String.valueOf(pageInfo.page() + 1)))));
        instance.setInventory(inventory);

        ItemStack none = createGlass(GlassType.NONE, player);
        ItemStack delete = createGlass(GlassType.DELETE, player);
        ItemStack prev = createGlass(GlassType.PREV, player);
        ItemStack next = createGlass(GlassType.NEXT, player);

        int skinCount = 0;
        for (GUISkinEntry entry : pageInfo.skinList()) {
            if (skinCount++ >= SharedGUI.HEAD_COUNT_PER_PAGE) {
                logger.warning("SkinsGUI: Skin count is more than 36, skipping...");
                break;
            }

            inventory.addItem(createSkull(player, entry));
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
        if (pageInfo.hasPrevious()) {
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
        if (pageInfo.hasNext()) {
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
}
