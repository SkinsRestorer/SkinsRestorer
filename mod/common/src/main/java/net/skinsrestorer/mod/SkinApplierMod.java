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
package net.skinsrestorer.mod;

import ch.jalu.configme.SettingsManager;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.RequiredArgsConstructor;
import net.lenni0451.reflect.stream.RStream;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkMap;
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
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.viaversion.ViaPacketData;
import net.skinsrestorer.viaversion.ViaRefreshProvider;
import net.skinsrestorer.viaversion.ViaWorkaround;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinApplierMod implements SkinApplierAccess<ServerPlayer> {
    private final SRModAdapter adapter;
    private final EventBusImpl eventBus;
    private final SettingsManager settings;
    private final SRLogger logger;

    public static void setGameProfileTextures(GameProfile gameProfile, SkinProperty property) {
        PropertyMap properties = gameProfile.properties();
        var newProperties = ImmutableMultimap.<String, Object>builder();
        for (var entry : properties.entries()) {
            if (SkinProperty.TEXTURES_NAME.equals(entry.getKey())) {
                continue;
            }

            newProperties.put(entry);
        }
        newProperties.put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));

        RStream.of(properties)
                .withSuper()
                .fields()
                .by("properties")
                .set(newProperties.build());
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
            adapter.runSync(Objects.requireNonNull(player.level().getServer()), () -> applySkinSync(player, applyEvent.getProperty()));
        });
    }

    public void applySkinSync(ServerPlayer player, SkinProperty property) {
        if (player.hasDisconnected()) {
            return;
        }

        ejectPassengers(player);

        setGameProfileTextures(player.getGameProfile(), property);

        for (ServerPlayer otherPlayer : getSeenByPlayers(player)) {
            untrackAndHideEntity(otherPlayer, player);
            trackAndShowEntity(otherPlayer, player);
        }

        // Refresh the players own skin
        refresh(player);
    }

    private List<ServerPlayer> getSeenByPlayers(ServerPlayer player) {
        return Objects.requireNonNull(player.level().getServer()).getPlayerList().getPlayers();
    }

    @SuppressWarnings("resource")
    private void untrackAndHideEntity(ServerPlayer currentEntity, Entity entityToHide) {
        ChunkMap tracker = currentEntity.level().getChunkSource().chunkMap;
        ChunkMap.TrackedEntity entry = tracker.entityMap.get(entityToHide.getId());
        if (entry != null) {
            entry.removePlayer(currentEntity);
        }

        if (entityToHide instanceof ServerPlayer otherPlayer) {
            // TODO: Maybe readd this? Bukkit-only code that was used to hide players
            // if (otherPlayer.sentListPacket) {}

            currentEntity.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(otherPlayer.getUUID())));
        }
    }

    @SuppressWarnings("resource")
    private void trackAndShowEntity(ServerPlayer currentEntity, Entity entityToShow) {
        if (entityToShow instanceof ServerPlayer otherPlayer) {
            currentEntity.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(otherPlayer)));
        }

        ChunkMap tracker = currentEntity.level().getChunkSource().chunkMap;
        ChunkMap.TrackedEntity entry = tracker.entityMap.get(entityToShow.getId());
        if (entry != null && !entry.seenBy.contains(currentEntity.connection)) {
            entry.updatePlayer(currentEntity);
        }
    }

    private void ejectPassengers(ServerPlayer player) {
        Entity vehicle = player.getVehicle();

        // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
        if (settings.getProperty(ServerConfig.DISMOUNT_PLAYER_ON_UPDATE) && vehicle != null) {
            player.stopRiding();

            if (settings.getProperty(ServerConfig.REMOUNT_PLAYER_ON_UPDATE)) {
                // This is delayed to next tick to allow the accepter to propagate if necessary
                adapter.runSync(Objects.requireNonNull(player.level().getServer()), () ->
                        player.startRiding(vehicle, false, true));
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

    @SuppressWarnings("resource")
    public void refresh(ServerPlayer player) {
        // Slowly getting from object to object till we get what is needed for
        // the respawn packet
        ServerLevel world = player.level();

        CommonPlayerSpawnInfo spawnInfo = player.createCommonSpawnInfo(world);
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(
                spawnInfo,
                ClientboundRespawnPacket.KEEP_ALL_DATA
        );

        sendPacket(player, new ClientboundPlayerInfoRemovePacket(List.of(player.getGameProfile().id())));
        sendPacket(player, ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));

        ViaRefreshProvider refreshProvider;
        if (adapter.getPluginInfo("viabackwards").isPresent() && ViaWorkaround.shouldApplyWorkaround()) {
            logger.debug("Activating ViaBackwards workaround.");
            refreshProvider = d -> {
                try {
                    return ViaWorkaround.sendCustomPacketVia(d.get());
                } catch (Exception e) {
                    logger.severe("Error while refreshing skin via ViaBackwards", e);
                    return false;
                }
            };
        } else {
            refreshProvider = ViaRefreshProvider.NO_OP;
        }

        if (refreshProvider.test(() -> new ViaPacketData(
                player.getUUID(),
                player.level().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).getId(player.level().dimensionType()),
                spawnInfo.seed(),
                spawnInfo.gameType().getId(),
                spawnInfo.isFlat()
        ))) {
            sendPacket(player, respawn);
        }

        player.onUpdateAbilities();

        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());

        // Send health, food, experience (food is sent together with health)
        player.resetSentInfo();

        PlayerList playerList = Objects.requireNonNull(player.level().getServer()).getPlayerList();
        playerList.sendPlayerPermissionLevel(player);
        playerList.sendLevelInfo(player, world);
        playerList.sendAllPlayerInfo(player);

        // Resend their effects
        for (MobEffectInstance effect : player.getActiveEffects()) {
            sendPacket(player, new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
        }
    }
}
