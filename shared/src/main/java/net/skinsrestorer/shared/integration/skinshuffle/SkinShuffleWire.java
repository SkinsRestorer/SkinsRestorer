/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.integration.skinshuffle;

import net.skinsrestorer.api.property.SkinProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class SkinShuffleWire {
    private SkinShuffleWire() {
    }

    public static byte[] createHandshakePayload() {
        return new byte[0];
    }

    public static byte[] createSkinRefreshPayload(SkinProperty property) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(1);
        writeString(out, SkinProperty.TEXTURES_NAME);
        writeString(out, property.getValue());
        writeString(out, property.getSignature());
        return out.toByteArray();
    }

    public static Optional<SkinProperty> decodeSkinRefreshPayload(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            boolean hasSignature = readBoolean(in);
            String name = readString(in);
            String value = readString(in);
            String signature = hasSignature ? readString(in) : null;
            if (in.available() != 0) {
                return Optional.empty();
            }

            return SkinProperty.tryParse(name, value, signature);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static byte[] createRefreshPlayerListEntryPayload(int entityId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeVarInt(out, entityId);
        return out.toByteArray();
    }

    public static Optional<Integer> decodeRefreshPlayerListEntryPayload(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            int entityId = readVarInt(in);
            if (in.available() != 0) {
                return Optional.empty();
            }

            return Optional.of(entityId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static boolean readBoolean(ByteArrayInputStream in) {
        int value = in.read();
        if (value == -1) {
            throw new IllegalArgumentException("Unexpected end of SkinShuffle payload");
        }

        return value != 0;
    }

    private static String readString(ByteArrayInputStream in) {
        int length = readVarInt(in);
        if (length < 0) {
            throw new IllegalArgumentException("Negative SkinShuffle string length");
        }
        if (in.available() < length) {
            throw new IllegalArgumentException("Truncated SkinShuffle string payload");
        }

        byte[] bytes = new byte[length];
        int read = in.read(bytes, 0, length);
        if (read != length) {
            throw new IllegalArgumentException("Truncated SkinShuffle string payload");
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.writeBytes(bytes);
    }

    private static int readVarInt(ByteArrayInputStream in) {
        int value = 0;
        int position = 0;

        while (position < 5) {
            int currentByte = in.read();
            if (currentByte == -1) {
                throw new IllegalArgumentException("Unexpected end of SkinShuffle varint");
            }
            if (position == 4 && (currentByte & 0xF0) != 0) {
                throw new IllegalArgumentException("SkinShuffle varint is too large");
            }

            value |= (currentByte & 0x7F) << (position * 7);
            if ((currentByte & 0x80) == 0) {
                return value;
            }

            position++;
        }

        throw new IllegalArgumentException("SkinShuffle varint is too large");
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        int current = value;
        while ((current & ~0x7F) != 0) {
            out.write((current & 0x7F) | 0x80);
            current >>>= 7;
        }
        out.write(current);
    }
}
