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
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.skinsrestorer.bukkit.utils.HandleReflection;
import net.skinsrestorer.viaversion.ExceptionSupplier;
import net.skinsrestorer.viaversion.ViaPacketData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class Mapping1_21_11 implements IMapping {
    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    @Override
    public void accept(Player player, Predicate<ExceptionSupplier<ViaPacketData>> viaFunction) {
        ServerPlayer entityPlayer = HandleReflection.getHandle(player, ServerPlayer.class);

        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel world = entityPlayer.level();

        CommonPlayerSpawnInfo spawnInfo = entityPlayer.createCommonSpawnInfo(world);
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                spawnInfo,
                ClientboundRespawnPacket.KEEP_ALL_DATA
        );

        resendInfoPackets(player, player);

        if (viaFunction.test(() -> IMapping.newViaPacketData(player, spawnInfo.seed(), spawnInfo.gameType().getId(), spawnInfo.isFlat()))) {
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
            sendPacket(entityPlayer, new ClientboundUpdateMobEffectPacket(entityPlayer.getId(), effect, false));
        }
    }

    @Override
    public void resendInfoPackets(Player toResend, Player toSendTo) {
        ServerPlayer toResendInternal = HandleReflection.getHandle(toResend, ServerPlayer.class);
        ServerPlayer toSendToInternal = HandleReflection.getHandle(toSendTo, ServerPlayer.class);

        sendPacket(toSendToInternal, new ClientboundPlayerInfoRemovePacket(List.of(toResendInternal.getUUID())));
        sendPacket(toSendToInternal, ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(toResendInternal)));
    }

    @Override
    public Set<String> getPaperMinecraftVersionIds() {
        return Set.of(
                "1.21.11"
        );
    }

    @Override
    public Set<String> getSpigotMappingVersions() {
        return Set.of(
                "e3cd927e07e6ff434793a0474c51b2b9" // 1.21.11
        );
    }
}
