/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.mappings.mapping1_19;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.BiomeManager;
import net.skinsrestorer.mappings.shared.IMapping;
import net.skinsrestorer.mappings.shared.MappingReflection;
import net.skinsrestorer.mappings.shared.ViaPacketData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Mapping1_19 implements IMapping {
    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    @Override
    public void triggerHealthUpdate(Player player) {
        MappingReflection.getHandle(player, ServerPlayer.class).resetSentInfo();
    }

    @Override
    public void accept(Player player, Predicate<ViaPacketData> viaFunction) {
        try {
            ServerPlayer entityPlayer = MappingReflection.getHandle(player, ServerPlayer.class);

            ClientboundPlayerInfoPacket removePlayer = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(entityPlayer));
            ClientboundPlayerInfoPacket addPlayer = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, List.of(entityPlayer));

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
                    true,
                    entityPlayer.getLastDeathLocation()
            );

            Location l = player.getLocation();
            ClientboundPlayerPositionPacket pos = new ClientboundPlayerPositionPacket(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), new HashSet<>(), 0, false);
            ClientboundSetCarriedItemPacket slot = new ClientboundSetCarriedItemPacket(player.getInventory().getHeldItemSlot());

            sendPacket(entityPlayer, removePlayer);
            sendPacket(entityPlayer, addPlayer);

            @SuppressWarnings("deprecation")
            int dimension = player.getWorld().getEnvironment().getId();

            if (viaFunction.test(new ViaPacketData(player, dimension, respawn.getSeed(), (short) respawn.getPlayerGameType().getId(), respawn.isFlat()))) {
                sendPacket(entityPlayer, respawn);
            }

            entityPlayer.onUpdateAbilities();

            sendPacket(entityPlayer, pos);
            sendPacket(entityPlayer, slot);

            player.getClass().getMethod("updateScaledHealth").invoke(player);
            player.updateInventory();
            triggerHealthUpdate(player);

            // Resend their effects
            for (MobEffectInstance mobEffect : entityPlayer.getActiveEffects()) {
                ClientboundUpdateMobEffectPacket effect = new ClientboundUpdateMobEffectPacket(entityPlayer.getId(), mobEffect);
                sendPacket(entityPlayer, effect);
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Set<String> getSupportedVersions() {
        return Set.of(
                "7b9de0da1357e5b251eddde9aa762916" // 1.19
        );
    }
}
