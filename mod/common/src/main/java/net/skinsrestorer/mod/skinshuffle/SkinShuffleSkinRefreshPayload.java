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
package net.skinsrestorer.mod.skinshuffle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.skinsrestorer.api.property.SkinProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels.SKIN_REFRESH;

public record SkinShuffleSkinRefreshPayload(String propertyName, String value, @Nullable String signature) implements CustomPacketPayload {
    public static final Type<SkinShuffleSkinRefreshPayload> TYPE = new Type<>(Identifier.parse(SKIN_REFRESH));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinShuffleSkinRefreshPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.signature != null);
                buf.writeUtf(payload.propertyName);
                buf.writeUtf(payload.value);
                if (payload.signature != null) {
                    buf.writeUtf(payload.signature);
                }
            },
            buf -> {
                boolean hasSignature = buf.readBoolean();
                String propertyName = buf.readUtf();
                String value = buf.readUtf();
                String signature = hasSignature ? buf.readUtf() : null;
                return new SkinShuffleSkinRefreshPayload(propertyName, value, signature);
            }
    );

    public Optional<SkinProperty> toSkinProperty() {
        return SkinProperty.tryParse(propertyName, value, signature);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
