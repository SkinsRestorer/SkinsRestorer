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
import org.jetbrains.annotations.NotNull;

import static net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels.REFRESH_PLAYER_LIST_ENTRY;

public record SkinShuffleRefreshPlayerListEntryPayload(int entityId) implements CustomPacketPayload {
    public static final Type<SkinShuffleRefreshPlayerListEntryPayload> TYPE = new Type<>(Identifier.parse(REFRESH_PLAYER_LIST_ENTRY));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinShuffleRefreshPlayerListEntryPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.entityId),
            buf -> new SkinShuffleRefreshPlayerListEntryPayload(buf.readVarInt())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
