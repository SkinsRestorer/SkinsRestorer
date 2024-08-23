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
package net.skinsrestorer.modded;

import ch.jalu.configme.SettingsManager;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.api.event.SkinApplyEventImpl;
import net.skinsrestorer.shared.config.ServerConfig;

import javax.inject.Inject;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierMod implements SkinApplierAccess<ServerPlayer> {
    private final SRModAdapter adapter;
    private final EventBusImpl eventBus;
    private final SettingsManager settings;

    public static void setGameProfileTextures(ServerPlayer player, SkinProperty property) {
        PropertyMap properties = player.getGameProfile().getProperties();
        properties.removeAll(SkinProperty.TEXTURES_NAME);
        properties.put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
    }

    @Override
    public void applySkin(ServerPlayer player, SkinProperty property) {
        if (player.hasDisconnected()) {
            return;
        }

        adapter.runAsync(() -> {
            SkinApplyEventImpl applyEvent = new SkinApplyEventImpl(player, property);

            eventBus.callEvent(applyEvent);

            if (applyEvent.isCancelled()) {
                return;
            }

            // delay 1 server tick so we override online-mode
            adapter.runSync(player.server, () -> applySkinSync(player, applyEvent.getProperty()));
        });
    }

    public void applySkinSync(ServerPlayer player, SkinProperty property) {
        if (player.hasDisconnected()) {
            return;
        }

        ejectPassengers(player);

        setGameProfileTextures(player, property);

        for (ServerPlayer otherPlayer : player.server.getPlayerList().getPlayers()) {
            // Do not hide the player from itself or do anything if the other player cannot see the player
            if (otherPlayer.getGameProfile().getId().equals(player.getGameProfile().getId())) {
                continue;
            }

            // Force player to be re-added to the player-list of every player on the server
            sendPacket(player, new ClientboundPlayerInfoRemovePacket(List.of(otherPlayer.getGameProfile().getId())));
            sendPacket(player, ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(otherPlayer)));
        }

        // Refresh the players own skin
        refresh(player);
    }

    private void ejectPassengers(ServerPlayer player) {
        Entity vehicle = player.getVehicle();

        // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
        if (settings.getProperty(ServerConfig.DISMOUNT_PLAYER_ON_UPDATE) && vehicle != null) {
            player.stopRiding();

            if (settings.getProperty(ServerConfig.REMOUNT_PLAYER_ON_UPDATE)) {
                // This is delayed to next tick to allow the accepter to propagate if necessary
                adapter.runSync(player.server, () -> {
                    player.startRiding(vehicle, false);
                });
            }
        }

        // Dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
        if (settings.getProperty(ServerConfig.DISMOUNT_PASSENGERS_ON_UPDATE) && !player.getPassengers().isEmpty()) {
            player.ejectPassengers();
        }
    }

    private static void sendPacket(ServerPlayer player, Packet<?> packet) {
        player.connection.send(packet);
    }

    public void refresh(ServerPlayer player) {
        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel world = player.serverLevel();

        CommonPlayerSpawnInfo spawnInfo = player.createCommonSpawnInfo(world);
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                spawnInfo,
                ClientboundRespawnPacket.KEEP_ALL_DATA
        );

        sendPacket(player, new ClientboundPlayerInfoRemovePacket(List.of(player.getGameProfile().getId())));
        sendPacket(player, ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));

        // TODO: Add via edge-case support
        sendPacket(player, respawn);

        player.onUpdateAbilities();

        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

        // Send health, food, experience (food is sent together with health)
        player.resetSentInfo();

        PlayerList playerList = player.server.getPlayerList();
        playerList.sendPlayerPermissionLevel(player);
        playerList.sendLevelInfo(player, world);
        playerList.sendAllPlayerInfo(player);

        // Resend their effects
        for (MobEffectInstance effect : player.getActiveEffects()) {
            sendPacket(player, new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
        }
    }
}
