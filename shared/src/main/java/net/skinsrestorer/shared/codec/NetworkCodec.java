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

import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("unused")
public record NetworkCodec<T>(Writer<T> writer, Reader<T> reader) {
    public static <T> NetworkCodec<T> of(Writer<T> writer, Reader<T> reader) {
        return new NetworkCodec<>(writer, reader);
    }

    public static <T> NetworkCodec<T> ofMapBackedDynamic(Map<String, T> idToValue, Function<T, String> dynamicMapper) {
        return ofMapBackedDynamic(idToValue, dynamicMapper, null);
    }

    public static <T> NetworkCodec<T> ofMapBackedDynamic(Map<String, T> idToValue, Function<T, String> dynamicMapper, @Nullable Function<String, String> messageSupplier) {
        Function<String, String> actualMessageSupplier = Objects.requireNonNullElse(messageSupplier, "Unknown id: %s"::formatted);
        return BuiltInCodecs.STRING_CODEC.map(dynamicMapper, id -> idToValue.computeIfAbsent(id, i -> {
            throw new IllegalArgumentException(actualMessageSupplier.apply(i));
        }));
    }

    public static <T extends Enum<T>> NetworkCodec<T> ofEnumDynamic(Class<T> clazz, Function<T, String> dynamicMapper) {
        return ofEnumDynamic(clazz, dynamicMapper, null);
    }

    public static <T extends Enum<T>> NetworkCodec<T> ofEnumDynamic(Class<T> clazz, Function<T, String> dynamicMapper, @Nullable Function<String, String> messageSupplier) {
        Map<String, T> idToValue = new HashMap<>();
        for (T value : clazz.getEnumConstants()) {
            idToValue.put(dynamicMapper.apply(value), value);
        }

        return ofMapBackedDynamic(idToValue, dynamicMapper, messageSupplier);
    }

    public static <T extends Enum<T> & NetworkId> NetworkCodec<T> ofEnum(Class<T> clazz) {
        return ofEnum(clazz, null);
    }

    public static <T extends Enum<T> & NetworkId> NetworkCodec<T> ofEnum(Class<T> clazz, @Nullable Function<String, String> messageSupplier) {
        return ofEnumDynamic(clazz, NetworkId::getId, messageSupplier);
    }

    public void write(SROutputWriter buf, T t) {
        writer.write(buf, t);
    }

    public T read(SRInputReader buf) {
        return reader.read(buf);
    }

    public <O> NetworkCodec<O> map(Function<O, T> to, Function<T, O> from) {
        return NetworkCodec.of(
                (stream, o) -> writer.write(stream, to.apply(o)),
                stream -> from.apply(reader.read(stream))
        );
    }

    public NetworkCodec<Optional<T>> optional() {
        return NetworkCodec.of(
                (os, optional) -> {
                    BuiltInCodecs.BOOLEAN_CODEC.write(os, optional.isPresent());
                    optional.ifPresent(t -> this.write(os, t));
                },
                is -> BuiltInCodecs.BOOLEAN_CODEC.read(is) ? Optional.of(this.read(is)) : Optional.empty()
        );
    }

    public NetworkCodec<List<T>> list() {
        return NetworkCodec.of(
                (os, list) -> {
                    BuiltInCodecs.INT_CODEC.write(os, list.size());
                    for (T entry : list) {
                        this.write(os, entry);
                    }
                },
                is -> {
                    int size = BuiltInCodecs.INT_CODEC.read(is);
                    List<T> list = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        list.add(this.read(is));
                    }

                    return list;
                }
        );
    }

    public <V> NetworkCodec<Map<T, V>> mappedTo(NetworkCodec<V> valueCodec) {
        return NetworkCodec.of(
                (os, map) -> {
                    BuiltInCodecs.INT_CODEC.write(os, map.size());
                    for (Map.Entry<T, V> entry : map.entrySet()) {
                        this.write(os, entry.getKey());
                        valueCodec.write(os, entry.getValue());
                    }
                },
                is -> {
                    int size = BuiltInCodecs.INT_CODEC.read(is);
                    Map<T, V> map = new LinkedHashMap<>(size);
                    for (int i = 0; i < size; i++) {
                        T key = this.read(is);
                        V value = valueCodec.read(is);
                        map.put(key, value);
                    }

                    return map;
                }
        );
    }

    public NetworkCodec<T> compressed() {
        return NetworkCodec.of(
                (stream, t) -> {
                    try (GZIPOutputStream gzip = new GZIPOutputStream(stream.wrapper());
                         DataOutputStream outputStream = new DataOutputStream(gzip)) {
                        writer.write(new SROutputWriter(outputStream), t);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                stream -> {
                    try (GZIPInputStream gzip = new GZIPInputStream(stream.wrapper());
                         DataInputStream inputStream = new DataInputStream(gzip)) {
                        return reader.read(new SRInputReader(inputStream));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public interface Writer<T> {
        void write(SROutputWriter buf, T t);
    }

    public interface Reader<T> {
        T read(SRInputReader buf);
    }
}
