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
package net.skinsrestorer.mod.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission.HasCommandLevel;
import net.minecraft.server.permissions.PermissionLevel;
import net.skinsrestorer.mod.SRModPlatform;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class SRModPlatformImpl implements SRModPlatform {
    private static final boolean HAS_PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> {
                    ModMetadata meta = mod.getMetadata();
                    ContactInformation contact = meta.getContact();
                    return new PluginInfo(
                            true,
                            meta.getId(),
                            meta.getName(),
                            meta.getVersion().getFriendlyString(),
                            "N/A",
                            Map.of(
                                    "homepage", contact.get("homepage").orElse("N/A"),
                                    "sources", contact.get("sources").orElse("N/A"),
                                    "issueTracker", contact.get("issues").orElse("N/A")
                            ),
                            meta.getAuthors().stream().map(Person::getName).toList()
                    );
                }).toList();
    }

    @Override
    public void registerPlayerJoinListener(Consumer<ServerPlayer> listener) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> listener.accept(handler.getPlayer()));
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public Platform getPlatform() {
        return Platform.FABRIC;
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        return new FabricServerCommandManager<>(executionCoordinator, senderMapper);
    }

    @Override
    public Tristate test(CommandSourceStack stack, Permission permission) {
        if (HAS_PERMISSIONS_API) {
            return switch (Permissions.getPermissionValue(stack, permission.getPermissionString())) {
                case TRUE -> Tristate.TRUE;
                case FALSE -> Tristate.FALSE;
                case DEFAULT -> getOpBasedPermission(stack);
            };
        }

        return getOpBasedPermission(stack);
    }

    private Tristate getOpBasedPermission(CommandSourceStack stack) {
        return stack.permissions().hasPermission(new HasCommandLevel(PermissionLevel.GAMEMASTERS))
                ? Tristate.TRUE : Tristate.UNDEFINED;
    }

    @Override
    public void registerPermission(Permission permission, Component description) {
        // NO-OP
    }

    @Override
    public <T extends CustomPacketPayload> void initBidirectionalMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver) {
        initServerboundMessageChannel(type, codec, receiver);
        initClientboundMessageChannel(type, codec);
    }

    @Override
    public <T extends CustomPacketPayload> void initServerboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver) {
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                receiver.accept(payload, context.player()));
    }

    @Override
    public <T extends CustomPacketPayload> void initClientboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ServerPlayNetworking.canSend(player, type);
    }

    @Override
    public void sendPluginMessage(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
