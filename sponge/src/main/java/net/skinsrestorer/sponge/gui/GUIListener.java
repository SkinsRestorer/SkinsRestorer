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
package net.skinsrestorer.sponge.gui;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.handler.SlotClickHandler;

import java.util.Optional;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class GUIListener implements SlotClickHandler {
    private final Consumer<ClickEventInfo> callback;
    private final int page;
    private final WrapperSponge wrapper;

    @Override
    public boolean handle(Cause cause, Container container, Slot slot, int slotIndex, ClickType<?> clickType) {
        Optional<Player> player = cause.context().get(EventContextKeys.PLAYER);

        if (!player.isPresent() || !(player.get() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player.get();

        ItemStack stack = slot.peek();

        if (stack == ItemStack.empty()) {
            return false;
        }

        Optional<Value<Component>> displayName = stack.getValue(Keys.DISPLAY_NAME);

        if (!displayName.isPresent()) {
            return false;
        }

        callback.accept(new ClickEventInfo(getMaterialType(stack),
                PlainTextComponentSerializer.plainText().serialize(displayName.get().get()), wrapper.player(serverPlayer), page));

        return false;
    }

    private ClickEventInfo.MaterialType getMaterialType(ItemStack stack) {
        ItemType type = stack.type();

        if (type == ItemTypes.PLAYER_HEAD.get()) {
            return ClickEventInfo.MaterialType.HEAD;
        } else if (type == ItemTypes.YELLOW_STAINED_GLASS_PANE.get()) {
            return ClickEventInfo.MaterialType.YELLOW_PANE;
        } else if (type == ItemTypes.GREEN_STAINED_GLASS_PANE.get()) {
            return ClickEventInfo.MaterialType.GREEN_PANE;
        } else if (type == ItemTypes.RED_STAINED_GLASS_PANE.get()) {
            return ClickEventInfo.MaterialType.RED_PANE;
        }

        return ClickEventInfo.MaterialType.UNKNOWN;
    }
}
