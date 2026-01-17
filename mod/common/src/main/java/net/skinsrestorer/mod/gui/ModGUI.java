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
package net.skinsrestorer.mod.gui;

import ch.jalu.injector.Injector;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.mod.wrapper.ModComponentHelper;
import net.skinsrestorer.mod.wrapper.WrapperMod;
import net.skinsrestorer.shared.gui.ActionDataCallback;
import net.skinsrestorer.shared.gui.ClickEventType;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ModGUI implements GUIManager<MenuProvider> {
    private final Injector injector;

    @SuppressWarnings("deprecation")
    private ItemStack createItem(SRInventory.Item entry) {
        Item item = switch (entry.materialType()) {
            case DIRT -> Items.DIRT;
            case SKULL -> Items.PLAYER_HEAD;
            case ARROW -> Items.ARROW;
            case BARRIER -> Items.BARRIER;
            case BOOKSHELF -> Items.BOOKSHELF;
            case ENDER_EYE -> Items.ENDER_EYE;
            case ENCHANTING_TABLE -> Items.ENCHANTING_TABLE;
        };
        PatchedDataComponentMap dataComponentMap = new PatchedDataComponentMap(item.components());
        entry.textureHash().ifPresent(hash -> {
            final ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
            builder.put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, SRHelpers.encodeHashToTexturesValue(hash)));

            PropertyMap propertyMap = new PropertyMap(builder.build());
            GameProfile gameProfile = new GameProfile(Util.NIL_UUID, "", propertyMap);

            dataComponentMap.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gameProfile));
        });
        dataComponentMap.set(DataComponents.ITEM_NAME, ModComponentHelper.deserialize(entry.displayName()));
        dataComponentMap.set(DataComponents.LORE, new ItemLore(entry.lore().stream().map(ModComponentHelper::deserialize).toList()));
        if (entry.enchantmentGlow()) {
            dataComponentMap.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return new ItemStack(item.builtInRegistryHolder(), 1, dataComponentMap.asPatch());
    }

    public MenuProvider createGUI(SRInventory srInventory) {
        return new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player inventoryOwner) {
                Map<Integer, Map<ClickEventType, SRInventory.ClickEventAction>> handlers = srInventory
                        .items()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().clickHandlers()
                        ));

                Container container = new SimpleContainer(9 * srInventory.rows());
                srInventory.items().forEach((key, value) -> container.setItem(key, createItem(value)));

                return new SRGUIMenu(
                        srInventory,
                        id,
                        inventory,
                        container,
                        handlers,
                        injector.getSingleton(ActionDataCallback.class),
                        injector.getSingleton(WrapperMod.class)
                );
            }

            @Override
            public @NotNull Component getDisplayName() {
                return ModComponentHelper.deserialize(srInventory.title());
            }
        };
    }

    public static class SRGUIMenu extends ChestMenu {
        private final Map<Integer, Map<ClickEventType, SRInventory.ClickEventAction>> handlers;
        private final ActionDataCallback dataCallback;
        private final WrapperMod wrapper;

        public SRGUIMenu(SRInventory srInventory, int id, Inventory inventory, Container container,
                         Map<Integer, Map<ClickEventType, SRInventory.ClickEventAction>> handlers,
                         ActionDataCallback dataCallback, WrapperMod wrapper) {
            super(switch (srInventory.rows()) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                case 6 -> MenuType.GENERIC_9x6;
                default -> throw new IllegalArgumentException("Invalid rows: " + srInventory.rows());
            }, id, inventory, container, srInventory.rows());
            this.handlers = handlers;
            this.dataCallback = dataCallback;
            this.wrapper = wrapper;
        }

        @Override
        public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player clickingPlayer) {
            Map<ClickEventType, SRInventory.ClickEventAction> slotHandlers = handlers.get(slotId);
            if (slotHandlers == null) {
                return;
            }

            SRInventory.ClickEventAction action = slotHandlers.get(switch (clickType) {
                case PICKUP -> switch (button) {
                    case 0 -> ClickEventType.LEFT;
                    case 1 -> ClickEventType.RIGHT;
                    default -> ClickEventType.OTHER;
                };
                case QUICK_MOVE -> (button == 0)
                        ? ClickEventType.SHIFT_LEFT
                        : ClickEventType.OTHER;
                default -> ClickEventType.OTHER;
            });

            if (action == null) {
                return;
            }

            dataCallback.handle(wrapper.player((ServerPlayer) clickingPlayer), action);
        }
    }
}
