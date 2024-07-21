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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record NetworkCodec<T>(Writer<T> writer, Reader<T> reader) {
    public static <T> NetworkCodec<T> of(Writer<T> writer, Reader<T> reader) {
        return new NetworkCodec<>(writer, reader);
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

    public NetworkCodec<T> compressed() {
        return NetworkCodec.of(
                (stream, t) -> {
                    try (GZIPOutputStream gzip = new GZIPOutputStream(stream.wrapper());
                         DataOutputStream outputStream = new DataOutputStream(gzip)) {
                        writer.write(new SROutputWriter(outputStream), t);

                        outputStream.flush();
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
