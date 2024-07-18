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
import net.skinsrestorer.shared.utils.ComponentString;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
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
            stream -> {
                int rows = CodecHelpers.INT_CODEC.read(stream);
                ComponentString title = ComponentString.CODEC.read(stream);
                Map<Integer, Item> items = CodecHelpers.createMapCodec(CodecHelpers.INT_CODEC, Item.CODEC).read(stream);
                return new SRInventory(rows, title, items);
            }
    );

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
                stream -> {
                    MaterialType materialType = MaterialType.CODEC.read(stream);
                    ComponentString displayName = ComponentString.CODEC.read(stream);
                    List<ComponentString> lore = CodecHelpers.createListCodec(ComponentString.CODEC).read(stream);
                    Optional<String> textureHash = CodecHelpers.createOptionalCodec(CodecHelpers.STRING_CODEC).read(stream);
                    boolean enchantmentGlow = CodecHelpers.BOOLEAN_CODEC.read(stream);
                    Map<ClickEventType, ClickEventAction> clickHandlers = CodecHelpers.createMapCodec(ClickEventType.CODEC, ClickEventAction.CODEC).read(stream);
                    return new Item(materialType, displayName, lore, textureHash, enchantmentGlow, clickHandlers);
                }
        );
    }

    public record ClickEventAction(byte[] actionBytes, boolean closeInventory) {
        public static final NetworkCodec<ClickEventAction> CODEC = NetworkCodec.of(
                (stream, action) -> {
                    CodecHelpers.BYTE_ARRAY_CODEC.write(stream, action.actionBytes());
                    CodecHelpers.BOOLEAN_CODEC.write(stream, action.closeInventory());
                },
                stream -> {
                    byte[] actionBytes = CodecHelpers.BYTE_ARRAY_CODEC.read(stream);
                    boolean closeInventory = CodecHelpers.BOOLEAN_CODEC.read(stream);
                    return new ClickEventAction(actionBytes, closeInventory);
                }
        );

        public static ClickEventAction fromStream(ThrowingConsumer<DataOutput> writer, boolean closeInventory) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                writer.accept(new DataOutputStream(baos));
                return new ClickEventAction(baos.toByteArray(), closeInventory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public interface ThrowingConsumer<T> {
            void accept(T t) throws IOException;
        }
    }
}
