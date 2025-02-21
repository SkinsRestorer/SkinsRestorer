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
package net.skinsrestorer.modded.gui;

import ch.jalu.injector.Injector;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.skinsrestorer.modded.MinecraftKyoriSerializer;
import net.skinsrestorer.modded.wrapper.WrapperMod;
import net.skinsrestorer.shared.gui.ActionDataCallback;
import net.skinsrestorer.shared.gui.ClickEventType;
import net.skinsrestorer.shared.gui.GUIManager;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ModGUI implements GUIManager<MenuProvider> {
    private final Injector injector;

    @SuppressWarnings("deprecation")
    private ItemStack createItem(SRInventory.Item entry) {
        Item item = switch (entry.materialType()) {
            case SKULL -> Items.PLAYER_HEAD;
            case ARROW -> Items.ARROW;
            case BARRIER -> Items.BARRIER;
            case BOOKSHELF -> Items.BOOKSHELF;
            case ENDER_EYE -> Items.ENDER_EYE;
            case ENCHANTING_TABLE -> Items.ENCHANTING_TABLE;
        };
        PatchedDataComponentMap dataComponentMap = new PatchedDataComponentMap(item.components());
        entry.textureHash().ifPresent(hash -> {
            PropertyMap propertyMap = new PropertyMap();
            propertyMap.put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, SRHelpers.encodeHashToTexturesValue(hash)));

            dataComponentMap.set(DataComponents.PROFILE, new ResolvableProfile(Optional.empty(), Optional.empty(), propertyMap));
        });
        dataComponentMap.set(DataComponents.ITEM_NAME, MinecraftKyoriSerializer.toNative(entry.displayName()));
        dataComponentMap.set(DataComponents.LORE, new ItemLore(entry.lore().stream().map(MinecraftKyoriSerializer::toNative).toList()));
        if (entry.enchantmentGlow()) {
            dataComponentMap.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        return new ItemStack(item.builtInRegistryHolder(), 1, dataComponentMap.asPatch());
    }

    public MenuProvider createGUI(SRInventory srInventory) {
        Map<Integer, Map<ClickEventType, SRInventory.ClickEventAction>> handlers = new HashMap<>();
        ActionDataCallback dataCallback = injector.getSingleton(ActionDataCallback.class);
        WrapperMod wrapper = injector.getSingleton(WrapperMod.class);

        return new MenuProvider() {
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                ChestMenu menu = new ChestMenu(switch (srInventory.rows()) {
                    case 1 -> MenuType.GENERIC_9x1;
                    case 2 -> MenuType.GENERIC_9x2;
                    case 3 -> MenuType.GENERIC_9x3;
                    case 4 -> MenuType.GENERIC_9x4;
                    case 5 -> MenuType.GENERIC_9x5;
                    case 6 -> MenuType.GENERIC_9x6;
                    default -> throw new IllegalArgumentException("Invalid rows: " + srInventory.rows());
                }, id, inventory, new SimpleContainer(9 * srInventory.rows()), srInventory.rows()) {
                    @Override
                    public void clicked(int slotId, int button, ClickType clickType, Player player) {
                        Map<ClickEventType, SRInventory.ClickEventAction> slotHandlers = handlers.get(slotId);
                        if (slotHandlers != null) {
                            SRInventory.ClickEventAction action = slotHandlers.get(switch (clickType) {
                                case PICKUP -> {
                                    if (button == 0) {
                                        yield ClickEventType.LEFT;
                                    } else if (button == 1) {
                                        yield ClickEventType.RIGHT;
                                    } else {
                                        yield ClickEventType.OTHER;
                                    }
                                }
                                case QUICK_MOVE -> {
                                    if (button == 0) {
                                        yield ClickEventType.SHIFT_LEFT;
                                    } else {
                                        yield ClickEventType.OTHER;
                                    }
                                }
                                default -> ClickEventType.OTHER;
                            });
                            if (action != null) {
                                dataCallback.handle(wrapper.player((ServerPlayer) player), action);
                            }
                        }
                    }
                };

                for (Map.Entry<Integer, SRInventory.Item> entry : srInventory.items().entrySet()) {
                    menu.setItem(entry.getKey(), 0, createItem(entry.getValue()));
                    handlers.put(entry.getKey(), entry.getValue().clickHandlers());
                }

                return menu;
            }

            @Override
            public @NotNull Component getDisplayName() {
                return MinecraftKyoriSerializer.toNative(srInventory.title());
            }
        };
    }
}
