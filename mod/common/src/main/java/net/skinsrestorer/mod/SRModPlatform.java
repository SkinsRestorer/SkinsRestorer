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
package net.skinsrestorer.mod;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;

import net.skinsrestorer.shared.info.PluginInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public interface SRModPlatform {
    SRModPlatform INSTANCE = ServiceLoader.load(SRModPlatform.class).findFirst().orElseThrow();

    Path getConfigFolder();

    List<PluginInfo> getPlugins();

    void registerPlayerJoinListener(Consumer<ServerPlayer> listener);

    String getPlatformName();

    Platform getPlatform();

    CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                         SenderMapper<CommandSourceStack, SRCommandSender> senderMapper);

    Tristate test(CommandSourceStack stack, Permission permission);

    void registerPermission(Permission permission, Component description);

    /**
     * Registers a raw message channel directly with the platform networking API,
     * bypassing Architectury's codec wrapping which adds varint length prefixes
     * incompatible with proxy plugin messages.
     */
    <T extends CustomPacketPayload> void initBidirectionalMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver);

    <T extends CustomPacketPayload> void initServerboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver);

    <T extends CustomPacketPayload> void initClientboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec);

    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);

    /**
     * Sends a payload to a player using the platform networking API directly,
     * bypassing Architectury's codec wrapping.
     */
    void sendPluginMessage(ServerPlayer player, CustomPacketPayload payload);
}
