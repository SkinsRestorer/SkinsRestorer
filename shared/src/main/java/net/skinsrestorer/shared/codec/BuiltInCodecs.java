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

import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;

import java.util.Locale;
import java.util.Optional;

public class BuiltInCodecs {
    public static final NetworkCodec<String> STRING_CODEC = NetworkCodec.of(
            SROutputWriter::writeString,
            SRInputReader::readString
    );
    public static final NetworkCodec<Integer> INT_CODEC = NetworkCodec.of(
            SROutputWriter::writeInt,
            SRInputReader::readInt
    );
    public static final NetworkCodec<Boolean> BOOLEAN_CODEC = NetworkCodec.of(
            SROutputWriter::writeBoolean,
            SRInputReader::readBoolean
    );
    public static final NetworkCodec<SkinProperty> SKIN_PROPERTY_CODEC = NetworkCodec.of(
            (os, s) -> {
                STRING_CODEC.write(os, s.getValue());
                STRING_CODEC.write(os, s.getSignature());
            },
            is -> SkinProperty.of(
                    STRING_CODEC.read(is),
                    STRING_CODEC.read(is)
            )
    );
    public static final NetworkCodec<SkinVariant> SKIN_VARIANT_CODEC = NetworkCodec.ofEnumDynamic(SkinVariant.class, t -> t.name().toLowerCase(Locale.ROOT));
    public static final NetworkCodec<SkinType> SKIN_TYPE_CODEC = NetworkCodec.ofEnumDynamic(SkinType.class, t -> t.name().toLowerCase(Locale.ROOT));
    public static final NetworkCodec<SkinIdentifier> SKIN_IDENTIFIER_CODEC = NetworkCodec.of(
            (os, s) -> {
                STRING_CODEC.write(os, s.getIdentifier());
                SKIN_VARIANT_CODEC.optional().write(os, Optional.ofNullable(s.getSkinVariant()));
                SKIN_TYPE_CODEC.write(os, s.getSkinType());
            },
            is -> SkinIdentifier.of(
                    STRING_CODEC.read(is),
                    SKIN_VARIANT_CODEC.optional().read(is).orElse(null),
                    SKIN_TYPE_CODEC.read(is)
            )
    );
}
