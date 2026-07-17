/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand.gui;

import io.fand.api.Fand;
import io.fand.api.entity.Player;
import io.fand.api.gui.Gui;
import io.fand.api.gui.GuiClick;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemProfile;
import io.fand.api.plugin.PluginContext;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.skinsrestorer.fand.wrapper.FandComponentHelper;
import net.skinsrestorer.fand.wrapper.WrapperFand;
import net.skinsrestorer.shared.gui.ActionDataCallback;
import net.skinsrestorer.shared.gui.ClickEventType;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class FandGUI {
    private final PluginContext context;
    private final ActionDataCallback actionDataCallback;
    private final WrapperFand wrapper;

    public void open(Player player, SRInventory source) {
        Gui.Builder builder = Gui.chest(source.rows(), FandComponentHelper.deserialize(source.title()));
        for (int slot = 0; slot < source.rows() * 9; slot++) {
            builder.protectedSlot(slot);
        }
        source.items().forEach((slot, item) -> {
            builder.item(slot, createItem(item));
            if (!item.clickHandlers().isEmpty()) {
                builder.handler(slot, click -> handleClick(click, item.clickHandlers()));
            }
        });
        context.guis().open(player, builder.build());
    }

    private ItemStack createItem(SRInventory.Item item) {
        String itemId = switch (item.materialType()) {
            case DIRT -> "minecraft:dirt";
            case SKULL -> "minecraft:player_head";
            case ARROW -> "minecraft:arrow";
            case BARRIER -> "minecraft:barrier";
            case BOOKSHELF -> "minecraft:bookshelf";
            case ENDER_EYE -> "minecraft:ender_eye";
            case ENCHANTING_TABLE -> "minecraft:enchanting_table";
        };
        var type = Fand.server().itemType(Key.key(itemId))
                .orElseThrow(() -> new IllegalStateException("Unknown Fand item type: " + itemId));
        ItemStack stack = new ItemStack(type, 1)
                .withCustomName(FandComponentHelper.deserialize(item.displayName()))
                .withLore(item.lore().stream().map(FandComponentHelper::deserialize).toList());
        if (item.enchantmentGlow()) {
            stack = stack.withEnchantmentGlintOverride(true);
        }
        if (item.textureHash().isPresent()) {
            var property = ItemProfile.Property.unsigned(
                    "textures",
                    SRHelpers.encodeHashToTexturesValue(item.textureHash().orElseThrow()));
            stack = stack.withProfile(new ItemProfile(null, null, java.util.List.of(property), null, null, null, null));
        }
        return stack;
    }

    private void handleClick(
            GuiClick click,
            Map<ClickEventType, SRInventory.ClickEventAction> handlers
    ) {
        SRInventory.ClickEventAction action = handlers.get(switch (click.clickType()) {
            case PICKUP -> ClickEventType.LEFT;
            case PICKUP_HALF -> ClickEventType.RIGHT;
            case QUICK_MOVE -> ClickEventType.SHIFT_LEFT;
            default -> ClickEventType.OTHER;
        });
        if (action != null) {
            actionDataCallback.handle(wrapper.player(click.player()), action);
        }
    }
}
