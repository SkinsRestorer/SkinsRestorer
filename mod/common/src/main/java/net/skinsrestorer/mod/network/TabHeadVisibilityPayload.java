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
package net.skinsrestorer.mod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent by ANY server-side plugin (not necessarily SkinsRestorer's own server plugin)
 * to tell the client whether it already renders a head next to this player's name in
 * the tab list by itself — e.g. via the vanilla 1.21.9+ player-head text object
 * component — so the mod's client should not also force its own vanilla tab-list skin
 * icon on top of it (see {@code PlayerTabOverlayMixin}).
 * <p>
 * Purely a client-side visual hint; carries no player data, and requires no
 * server-side SkinsRestorer plugin to be installed. Registered unconditionally at mod
 * init, independent of whether this mod instance is hosting.
 */
public record TabHeadVisibilityPayload(boolean suppressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TabHeadVisibilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("skinsrestorer", "tab_head_visibility"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabHeadVisibilityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.suppressed()),
            buf -> new TabHeadVisibilityPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
