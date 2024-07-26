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

import lombok.Getter;
import net.skinsrestorer.shared.codec.BuiltInCodecs;
import net.skinsrestorer.shared.codec.NetworkCodec;
import net.skinsrestorer.shared.codec.NetworkId;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.subjects.messages.ComponentString;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record SRInventory(int rows, ComponentString title, Map<Integer, Item> items) {
    public static final NetworkCodec<SRInventory> CODEC = NetworkCodec.of(
            (stream, inventory) -> {
                BuiltInCodecs.INT_CODEC.write(stream, inventory.rows());
                ComponentString.CODEC.write(stream, inventory.title());
                BuiltInCodecs.INT_CODEC.mappedTo(Item.CODEC).write(stream, inventory.items());
            },
            stream -> new SRInventory(
                    BuiltInCodecs.INT_CODEC.read(stream),
                    ComponentString.CODEC.read(stream),
                    BuiltInCodecs.INT_CODEC.mappedTo(Item.CODEC).read(stream)
            )
    ).compressed();

    @Getter
    public enum MaterialType implements NetworkId {
        SKULL,
        ARROW,
        BARRIER,
        BOOKSHELF,
        ENDER_EYE,
        ENCHANTING_TABLE;

        public static final NetworkCodec<MaterialType> CODEC = NetworkCodec.ofEnum(MaterialType.class);

        @Override
        public String getId() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Item(
            MaterialType materialType,
            ComponentString displayName,
            List<ComponentString> lore,
            Optional<String> textureHash,
            boolean enchantmentGlow,
            Map<ClickEventType, ClickEventAction> clickHandlers
    ) {
        public static final NetworkCodec<Item> CODEC = NetworkCodec.of(
                (stream, item) -> {
                    MaterialType.CODEC.write(stream, item.materialType());
                    ComponentString.CODEC.write(stream, item.displayName());
                    ComponentString.CODEC.list().write(stream, item.lore());
                    BuiltInCodecs.STRING_CODEC.optional().write(stream, item.textureHash());
                    BuiltInCodecs.BOOLEAN_CODEC.write(stream, item.enchantmentGlow());
                    ClickEventType.CODEC.mappedTo(ClickEventAction.CODEC).write(stream, item.clickHandlers());
                },
                stream -> new Item(
                        MaterialType.CODEC.read(stream),
                        ComponentString.CODEC.read(stream),
                        ComponentString.CODEC.list().read(stream),
                        BuiltInCodecs.STRING_CODEC.optional().read(stream),
                        BuiltInCodecs.BOOLEAN_CODEC.read(stream),
                        ClickEventType.CODEC.mappedTo(ClickEventAction.CODEC).read(stream)
                )
        );
    }

    public record ClickEventAction(List<SRProxyPluginMessage.GUIActionChannelPayload> actionChannelPayload,
                                   boolean closeInventory) {
        public static final NetworkCodec<ClickEventAction> CODEC = NetworkCodec.of(
                (stream, action) -> {
                    SRProxyPluginMessage.GUIActionChannelPayload.CODEC.list().write(stream, action.actionChannelPayload());
                    BuiltInCodecs.BOOLEAN_CODEC.write(stream, action.closeInventory());
                },
                stream -> new ClickEventAction(
                        SRProxyPluginMessage.GUIActionChannelPayload.CODEC.list().read(stream),
                        BuiltInCodecs.BOOLEAN_CODEC.read(stream)
                )
        );

        public ClickEventAction(SRProxyPluginMessage.GUIActionChannelPayload actionChannelPayload,
                                boolean closeInventory) {
            this(List.of(actionChannelPayload), closeInventory);
        }
    }
}
