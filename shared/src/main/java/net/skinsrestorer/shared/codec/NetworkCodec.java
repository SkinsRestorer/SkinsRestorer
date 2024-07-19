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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Function;

public record NetworkCodec<T>(Writer<T> writer, Reader<T> reader) {
    public static <T> NetworkCodec<T> of(Writer<T> writer, Reader<T> reader) {
        return new NetworkCodec<>(writer, reader);
    }

    public void write(DataOutput stream, T t) {
        try {
            writer.write(stream, t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public T read(DataInput stream) {
        try {
            return reader.read(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <O> NetworkCodec<O> map(Function<O, T> to, Function<T, O> from) {
        return NetworkCodec.of(
                (stream, o) -> writer.write(stream, to.apply(o)),
                stream -> from.apply(reader.read(stream))
        );
    }

    public interface Writer<T> {
        void write(DataOutput stream, T t) throws IOException;
    }

    public interface Reader<T> {
        T read(DataInput stream) throws IOException;
    }
}
