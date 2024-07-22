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

import java.io.*;

public class SRInputReader {
    private final DataInput dataInput;

    public SRInputReader(byte[] bytes) {
        this(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    public SRInputReader(DataInput dataInput) {
        this.dataInput = dataInput;
    }

    public boolean readBoolean() {
        try {
            return dataInput.readBoolean();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int readInt() {
        try {
            return dataInput.readInt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readString() {
        try {
            return dataInput.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream wrapper() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return dataInput.readUnsignedByte();
            }
        };
    }
}
