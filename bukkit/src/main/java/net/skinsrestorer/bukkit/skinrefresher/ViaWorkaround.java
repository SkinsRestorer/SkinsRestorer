/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.bukkit.skinrefresher;

import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import net.skinsrestorer.mappings.mapping1_18.ViaPacketData;

public final class ViaWorkaround {
    private ViaWorkaround() {
    }

    public static boolean isProtocolNewer() {
        return Via.getManager().getProtocolManager().getServerProtocolVersion().lowestSupportedVersion() >= ProtocolVersion.v1_16.getVersion();
    }

    public static boolean sendCustomPacketVia(ViaPacketData packetData) {
        UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(packetData.getPlayer().getUniqueId());
        if (connection != null
                && connection.getProtocolInfo() != null
                && connection.getProtocolInfo().getProtocolVersion() < ProtocolVersion.v1_16.getVersion()) {
            // ViaBackwards double-sends isProtocolNewer respawn packet when its dimension ID matches the current world's.
            // In order to get around this, we send isProtocolNewer packet directly into Via's connection, bypassing the 1.16 conversion step
            // and therefore bypassing their workaround.
            PacketWrapper packet = PacketWrapper.create(ClientboundPackets1_15.RESPAWN.ordinal(), null, connection);

            packet.write(Type.INT, packetData.getDimension());
            packet.write(Type.LONG, packetData.getSeed());
            packet.write(Type.UNSIGNED_BYTE, packetData.getGamemodeId());
            packet.write(Type.STRING, packetData.isFlat() ? "flat" : "default");
            try {
                packet.send(Protocol1_15_2To1_16.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        return true;
    }
}
