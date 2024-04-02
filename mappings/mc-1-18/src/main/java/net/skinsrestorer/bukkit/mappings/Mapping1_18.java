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
package net.skinsrestorer.bukkit.mappings;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.biome.BiomeManager;
import net.skinsrestorer.bukkit.mappings.IMapping;
import net.skinsrestorer.bukkit.mappings.MappingReflection;
import net.skinsrestorer.bukkit.mappings.ViaPacketData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Mapping1_18 implements IMapping {
    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    @Override
    public void accept(Player player, Predicate<ViaPacketData> viaFunction) {
        ServerPlayer entityPlayer = MappingReflection.getHandle(player, ServerPlayer.class);

        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel world = entityPlayer.getLevel();
        ServerPlayerGameMode gamemode = entityPlayer.gameMode;

        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                world.dimensionType(),
                world.dimension(),
                BiomeManager.obfuscateSeed(world.getSeed()),
                gamemode.getGameModeForPlayer(),
                gamemode.getPreviousGameModeForPlayer(),
                world.isDebug(),
                world.isFlat(),
                true);

        sendPacket(entityPlayer, new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, List.of(entityPlayer)));
        sendPacket(entityPlayer, new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, List.of(entityPlayer)));

        if (viaFunction.test(new ViaPacketData(player, respawn.getSeed(), respawn.getPlayerGameType().getId(), respawn.isFlat()))) {
            sendPacket(entityPlayer, respawn);
        }

        entityPlayer.onUpdateAbilities();

        entityPlayer.connection.teleport(player.getLocation());

        // Send health, food, experience (food is sent together with health)
        entityPlayer.resetSentInfo();

        PlayerList playerList = entityPlayer.server.getPlayerList();
        playerList.sendPlayerPermissionLevel(entityPlayer);
        playerList.sendLevelInfo(entityPlayer, world);
        playerList.sendAllPlayerInfo(entityPlayer);

        // Resend their effects
        for (MobEffectInstance effect : entityPlayer.getActiveEffects()) {
            sendPacket(entityPlayer,  new ClientboundUpdateMobEffectPacket(entityPlayer.getId(), effect));
        }
    }

    @Override
    public Set<String> getSupportedVersions() {
        return Set.of(
                "9e9fe6961a80f3e586c25601590b51ec", // 1.18
                "20b026e774dbf715e40a0b2afe114792" // 1.18.1
        );
    }
}
