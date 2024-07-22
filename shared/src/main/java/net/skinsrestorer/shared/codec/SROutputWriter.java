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

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public class SROutputWriter {
    private final DataOutput dataOutput;

    public SROutputWriter(DataOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    public void writeBoolean(boolean value) {
        try {
            dataOutput.writeBoolean(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeString(String value) {
        try {
            dataOutput.writeUTF(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeInt(int value) {
        try {
            dataOutput.writeInt(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStream wrapper() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                dataOutput.writeByte(b);
            }
        };
    }
}
