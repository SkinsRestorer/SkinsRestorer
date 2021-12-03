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
package net.skinsrestorer.mappings.mapping1_18;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.biome.BiomeManager;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.function.Function;

public class Mapping1_18 {
    public static void triggerHealthUpdate(Player player) {
        ((CraftPlayer) player).getHandle().resetSentInfo();
    }

    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    public static void accept(Player player, Function<ViaPacketData, Boolean> viaFunction) {
        final ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        ClientboundPlayerInfoPacket removePlayer = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, ImmutableList.of(entityPlayer));
        ClientboundPlayerInfoPacket addPlayer = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, ImmutableList.of(entityPlayer));

        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel world = entityPlayer.getLevel();
        ServerPlayerGameMode gamemode = entityPlayer.gameMode;

        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(world.dimensionType(), world.dimension(), BiomeManager.obfuscateSeed(world.getSeed()), gamemode.getGameModeForPlayer(), gamemode.getPreviousGameModeForPlayer(), world.isDebug(), world.isFlat(), true);

        Location l = player.getLocation();
        ClientboundPlayerPositionPacket pos = new ClientboundPlayerPositionPacket(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<>(), 0, false);
        ClientboundSetCarriedItemPacket slot = new ClientboundSetCarriedItemPacket(player.getInventory().getHeldItemSlot());

        sendPacket(entityPlayer, removePlayer);
        sendPacket(entityPlayer, addPlayer);

        @SuppressWarnings("deprecation")
        int dimension = player.getWorld().getEnvironment().getId();

        if (viaFunction.apply(new ViaPacketData(player, dimension, respawn.getSeed(), (short) respawn.getPlayerGameType().getId(), respawn.isFlat()))) {
            sendPacket(entityPlayer, respawn);
        }

        entityPlayer.onUpdateAbilities();

        sendPacket(entityPlayer, pos);
        sendPacket(entityPlayer, slot);

        ((CraftPlayer) player).updateScaledHealth();
        player.updateInventory();
        triggerHealthUpdate(player);
    }
}
