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

public class BuiltInCodecs {
    public static NetworkCodec<String> STRING_CODEC = NetworkCodec.of(
            SROutputWriter::writeString,
            SRInputReader::readString
    );
    public static NetworkCodec<Integer> INT_CODEC = NetworkCodec.of(
            SROutputWriter::writeInt,
            SRInputReader::readInt
    );
    public static NetworkCodec<Boolean> BOOLEAN_CODEC = NetworkCodec.of(
            SROutputWriter::writeBoolean,
            SRInputReader::readBoolean
    );
    public static NetworkCodec<SkinProperty> SKIN_PROPERTY_CODEC = NetworkCodec.of(
            (os, s) -> {
                STRING_CODEC.write(os, s.getValue());
                STRING_CODEC.write(os, s.getSignature());
            },
            is -> SkinProperty.of(
                    STRING_CODEC.read(is),
                    STRING_CODEC.read(is)
            )
    );
}
