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
package net.skinsrestorer.shared.codec;

import net.skinsrestorer.api.property.SkinProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.*;

public class CodecHelpers {
    public static NetworkCodec<String> STRING_CODEC = NetworkCodec.of(
            DataOutput::writeUTF,
            DataInput::readUTF
    );
    public static NetworkCodec<Integer> INT_CODEC = NetworkCodec.of(
            DataOutput::writeInt,
            DataInput::readInt
    );
    public static NetworkCodec<Boolean> BOOLEAN_CODEC = NetworkCodec.of(
            DataOutput::writeBoolean,
            DataInput::readBoolean
    );
    public static NetworkCodec<byte[]> BYTE_ARRAY_CODEC = NetworkCodec.of(
            (os, s) -> {
                INT_CODEC.write(os, s.length);
                os.write(s);
            },
            is -> {
                int length = INT_CODEC.read(is);
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return bytes;
            }
    );
    public static NetworkCodec<SkinProperty> SKIN_PROPERTY_CODEC = NetworkCodec.of(
            (os, s) -> {
                STRING_CODEC.write(os, s.getValue());
                STRING_CODEC.write(os, s.getSignature());
            },
            is -> {
                String value = STRING_CODEC.read(is);
                String signature = STRING_CODEC.read(is);
                return SkinProperty.of(value, signature);
            }
    );

    public static <T> NetworkCodec<Optional<T>> createOptionalCodec(NetworkCodec<T> elementCodec) {
        return NetworkCodec.of(
                (os, optional) -> {
                    BOOLEAN_CODEC.write(os, optional.isPresent());
                    optional.ifPresent(t -> elementCodec.write(os, t));
                },
                is -> BOOLEAN_CODEC.read(is) ? Optional.of(elementCodec.read(is)) : Optional.empty()
        );
    }

    public static <T> NetworkCodec<List<T>> createListCodec(NetworkCodec<T> elementCodec) {
        return NetworkCodec.of(
                (os, list) -> {
                    INT_CODEC.write(os, list.size());
                    for (T entry : list) {
                        elementCodec.write(os, entry);
                    }
                },
                is -> {
                    int size = INT_CODEC.read(is);
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(elementCodec.read(is));
                    }

                    return list;
                }
        );
    }

    public static <K, V> NetworkCodec<Map<K, V>> createMapCodec(NetworkCodec<K> keyCodec, NetworkCodec<V> valueCodec) {
        return NetworkCodec.of(
                (os, map) -> {
                    INT_CODEC.write(os, map.size());
                    for (Map.Entry<K, V> entry : map.entrySet()) {
                        keyCodec.write(os, entry.getKey());
                        valueCodec.write(os, entry.getValue());
                    }
                },
                is -> {
                    int size = INT_CODEC.read(is);
                    Map<K, V> map = new LinkedHashMap<>(size);
                    for (int i = 0; i < size; i++) {
                        K key = keyCodec.read(is);
                        V value = valueCodec.read(is);
                        map.put(key, value);
                    }

                    return map;
                }
        );
    }

    public static <T extends Enum<T> & NetworkId> NetworkCodec<T> createEnumCodec(Class<T> clazz) {
        Map<String, T> idToValue = new HashMap<>();
        for (T value : clazz.getEnumConstants()) {
            idToValue.put(value.getId(), value);
        }

        return STRING_CODEC.map(NetworkId::getId, id -> idToValue.computeIfAbsent(id, i -> {
            throw new IllegalArgumentException("Unknown enum value: " + i);
        }));
    }
}
