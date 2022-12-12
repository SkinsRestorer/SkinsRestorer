/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.mappings.mapping1_19_3;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.biome.BiomeManager;
import net.skinsrestorer.mappings.shared.IMapping;
import net.skinsrestorer.mappings.shared.ViaPacketData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class Mapping1_19_3 implements IMapping {
    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    public void triggerHealthUpdate(Player player) {
        extractServerPlayer(player).resetSentInfo();
    }

    public void accept(Player player, Predicate<ViaPacketData> viaFunction) {
        try {
            final ServerPlayer entityPlayer = (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);

            ClientboundPlayerInfoRemovePacket removePlayer = new ClientboundPlayerInfoRemovePacket(ImmutableList.of(entityPlayer.getUUID()));
            ClientboundPlayerInfoUpdatePacket addPlayer = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, entityPlayer);

            // Slowly getting from object to object till we get what is needed for
            // the respawn packet
            ServerLevel world = entityPlayer.getLevel();
            ServerPlayerGameMode gamemode = entityPlayer.gameMode;

            ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                    world.dimensionTypeId(),
                    world.dimension(),
                    BiomeManager.obfuscateSeed(world.getSeed()),
                    gamemode.getGameModeForPlayer(),
                    gamemode.getPreviousGameModeForPlayer(),
                    world.isDebug(),
                    world.isFlat(),
                    (byte) 3,
                    entityPlayer.getLastDeathLocation()
            );

            Location l = player.getLocation();
            ClientboundPlayerPositionPacket pos = new ClientboundPlayerPositionPacket(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<>(), 0, false);
            ClientboundSetCarriedItemPacket slot = new ClientboundSetCarriedItemPacket(player.getInventory().getHeldItemSlot());

            sendPacket(entityPlayer, removePlayer);
            sendPacket(entityPlayer, addPlayer);

            @SuppressWarnings("deprecation")
            int dimension = player.getWorld().getEnvironment().getId();

            if (Boolean.TRUE.equals(viaFunction.test(new ViaPacketData(player, dimension, respawn.getSeed(), (short) respawn.getPlayerGameType().getId(), respawn.isFlat())))) {
                sendPacket(entityPlayer, respawn);
            }

            entityPlayer.onUpdateAbilities();

            sendPacket(entityPlayer, pos);
            sendPacket(entityPlayer, slot);

            player.getClass().getMethod("updateScaledHealth").invoke(player);
            player.updateInventory();
            triggerHealthUpdate(player);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private ServerPlayer extractServerPlayer(Player player) {
        try {
            return (ServerPlayer) player.getClass().getMethod("getHandle").invoke(player);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Set<String> getSupportedVersions() {
        return Set.of(
                "1afe2ffe8a9d7fc510442a168b3d4338" // 1.19.3
        );
    }
}
