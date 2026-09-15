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
package net.skinsrestorer.bukkit.mappings;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.skinsrestorer.viaversion.ViaRefreshProvider;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class PaperMapping26_1 implements IMapping {
    @Override
    public void accept(Player player, ViaRefreshProvider viaFunction) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel serverLevel = serverPlayer.level();

        CommonPlayerSpawnInfo spawnInfo = serverPlayer.createCommonSpawnInfo(serverLevel);
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                spawnInfo,
                ClientboundRespawnPacket.KEEP_ALL_DATA
        );

        resendInfoPackets(player, player);

        if (viaFunction.test(() -> IMapping.newViaPacketData(player, spawnInfo.seed(), spawnInfo.gameType().getId(), spawnInfo.isFlat()))) {
            serverPlayer.connection.send(respawn);
        }

        serverPlayer.onUpdateAbilities();

        serverPlayer.connection.internalTeleport(player.getLocation());

        // Send health, food, experience (food is sent together with health)
        serverPlayer.resetSentInfo();

        PlayerList playerList = serverLevel.getServer().getPlayerList();
        playerList.sendPlayerPermissionLevel(serverPlayer);
        playerList.sendLevelInfo(serverPlayer, serverLevel);
        playerList.sendAllPlayerInfo(serverPlayer);

        // Resend their effects
        for (MobEffectInstance effect : serverPlayer.getActiveEffects()) {
            serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(serverPlayer.getId(), effect, false));
        }
    }

    @Override
    public void resendInfoPackets(Player toResend, Player toSendTo) {
        ServerPlayer toResendInternal = ((CraftPlayer) toResend).getHandle();
        ServerPlayer toSendToInternal = ((CraftPlayer) toSendTo).getHandle();

        toSendToInternal.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(toResendInternal.getUUID())));
        toSendToInternal.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(toResendInternal)));
    }

    @Override
    public Set<String> getPaperMinecraftVersionIds() {
        return Set.of(
                "26.1+",
                "26.2+",
                "26.3+"
        );
    }

}
