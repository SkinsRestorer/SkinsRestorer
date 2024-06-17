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
package net.skinsrestorer.bukkit.refresher;

import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import net.skinsrestorer.bukkit.mappings.ViaPacketData;

public final class ViaWorkaround {
    private ViaWorkaround() {
    }

    public static boolean shouldApplyWorkaround() {
        return Via.getManager().getProtocolManager().getServerProtocolVersion()
                .lowestSupportedProtocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_16);
    }

    @SuppressWarnings("deprecation")
    public static boolean sendCustomPacketVia(ViaPacketData packetData) {
        UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(packetData.player().getUniqueId());
        if (connection == null || connection.getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_16)) {
            return true;
        }

        // ViaBackwards double-sends the respawn packet when its dimension ID matches the current world's.
        // In order to get around this, we send the packet directly into Via's connection, bypassing the 1.16 conversion step
        // and therefore bypassing their workaround.
        PacketWrapper packet = PacketWrapper.create(ClientboundPackets1_15.RESPAWN, connection);

        packet.write(Types.INT, packetData.player().getWorld().getEnvironment().getId());
        packet.write(Types.LONG, packetData.seed());
        packet.write(Types.UNSIGNED_BYTE, (short) packetData.gamemodeId());
        packet.write(Types.STRING, packetData.isFlat() ? "flat" : "default");
        try {
            packet.send(Protocol1_16To1_15_2.class);
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
