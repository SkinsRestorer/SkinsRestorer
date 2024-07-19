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
import net.skinsrestorer.shared.codec.CodecHelpers;
import net.skinsrestorer.shared.codec.NetworkCodec;
import net.skinsrestorer.shared.codec.NetworkId;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.utils.ComponentString;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public record SRInventory(int rows, ComponentString title, Map<Integer, Item> items) {
    public static final NetworkCodec<SRInventory> CODEC = NetworkCodec.of(
            (stream, inventory) -> {
                CodecHelpers.INT_CODEC.write(stream, inventory.rows());
                ComponentString.CODEC.write(stream, inventory.title());
                CodecHelpers.createMapCodec(CodecHelpers.INT_CODEC, Item.CODEC).write(stream, inventory.items());
            },
            stream -> new SRInventory(
                    CodecHelpers.INT_CODEC.read(stream),
                    ComponentString.CODEC.read(stream),
                    CodecHelpers.createMapCodec(CodecHelpers.INT_CODEC, Item.CODEC).read(stream)
            )
    ).compressed();

    @Getter
    public enum MaterialType implements NetworkId {
        SKULL,
        ARROW,
        BARRIER;

        public static final NetworkCodec<MaterialType> CODEC = CodecHelpers.createEnumCodec(MaterialType.class);

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
        public static NetworkCodec<Item> CODEC = NetworkCodec.of(
                (stream, item) -> {
                    MaterialType.CODEC.write(stream, item.materialType());
                    ComponentString.CODEC.write(stream, item.displayName());
                    CodecHelpers.createListCodec(ComponentString.CODEC).write(stream, item.lore());
                    CodecHelpers.createOptionalCodec(CodecHelpers.STRING_CODEC).write(stream, item.textureHash());
                    CodecHelpers.BOOLEAN_CODEC.write(stream, item.enchantmentGlow());
                    CodecHelpers.createMapCodec(ClickEventType.CODEC, ClickEventAction.CODEC).write(stream, item.clickHandlers());
                },
                stream -> new Item(
                        MaterialType.CODEC.read(stream),
                        ComponentString.CODEC.read(stream),
                        CodecHelpers.createListCodec(ComponentString.CODEC).read(stream),
                        CodecHelpers.createOptionalCodec(CodecHelpers.STRING_CODEC).read(stream),
                        CodecHelpers.BOOLEAN_CODEC.read(stream),
                        CodecHelpers.createMapCodec(ClickEventType.CODEC, ClickEventAction.CODEC).read(stream)
                )
        );
    }

    public record ClickEventAction(SRProxyPluginMessage.GUIActionChannelPayload actionChannelPayload,
                                   boolean closeInventory) {
        public static final NetworkCodec<ClickEventAction> CODEC = NetworkCodec.of(
                (stream, action) -> {
                    SRProxyPluginMessage.GUIActionChannelPayload.CODEC.write(stream, action.actionChannelPayload());
                    CodecHelpers.BOOLEAN_CODEC.write(stream, action.closeInventory());
                },
                stream -> new ClickEventAction(
                        SRProxyPluginMessage.GUIActionChannelPayload.CODEC.read(stream),
                        CodecHelpers.BOOLEAN_CODEC.read(stream)
                )
        );
    }
}
