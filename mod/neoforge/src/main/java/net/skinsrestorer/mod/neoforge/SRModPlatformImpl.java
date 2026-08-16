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
package net.skinsrestorer.mod.neoforge;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission.HasCommandLevel;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.skinsrestorer.mod.SRModPlatform;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.utils.Tristate;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.neoforge.NeoForgeServerCommandManager;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class SRModPlatformImpl implements SRModPlatform {
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = new HashMap<>();

    @Override
    public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return ModList.get().getMods().stream()
                .map(mod -> {
                    String homepage = mod.getModURL()
                            .map(Object::toString).orElse("N/A");
                    String issueTracker = mod.getOwningFile().getConfig()
                            .<String>getConfigElement("issueTrackerURL").orElse("N/A");
                    String authors = mod.getConfig()
                            .<String>getConfigElement("authors").orElse("N/A");
                    return new PluginInfo(
                            true,
                            mod.getModId(),
                            mod.getDisplayName(),
                            mod.getVersion().toString(),
                            "N/A",
                            Map.of(
                                    "homepage", homepage,
                                    "sources", "N/A",
                                    "issueTracker", issueTracker
                            ),
                            List.of(authors)
                    );
                }).toList();
    }

    @Override
    public void registerPlayerJoinListener(Consumer<ServerPlayer> listener) {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class,
                e -> listener.accept((ServerPlayer) e.getEntity()));
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public Platform getPlatform() {
        return Platform.NEO_FORGE;
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager(ExecutionCoordinator<SRCommandSender> executionCoordinator,
                                                                SenderMapper<CommandSourceStack, SRCommandSender> senderMapper) {
        return new NeoForgeServerCommandManager<>(executionCoordinator, senderMapper);
    }

    @Override
    public Tristate test(CommandSourceStack stack, Permission permission) {
        if (!stack.isPlayer()) {
            return stack.permissions().hasPermission(new HasCommandLevel(PermissionLevel.GAMEMASTERS))
                    ? Tristate.TRUE : Tristate.UNDEFINED;
        }

        return PermissionAPI.getPermission(Objects.requireNonNull(stack.getPlayer()), PERMISSIONS.get(permission.getPermissionString())) ? Tristate.TRUE : Tristate.FALSE;
    }

    @Override
    public void registerPermission(Permission permission, Component description) {
        String permissionString = permission.getPermissionString();
        int dotIndex = permissionString.indexOf('.');
        if (dotIndex == -1) {
            throw new IllegalArgumentException("Permission string must contain a dot: " + permissionString);
        }
        String beforeDot = permissionString.substring(0, dotIndex);
        String afterDot = permissionString.substring(dotIndex + 1);
        PermissionNode<Boolean> node = new PermissionNode<>(beforeDot, afterDot, PermissionTypes.BOOLEAN, (arg, uUID, permissionDynamicContexts) -> permission.isInDefaultGroup());
        node.setInformation(Component.literal(permission.getPermissionString()), description);

        PERMISSIONS.put(permission.getPermissionString(), node);
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> event.addNodes(node));
    }

    @Override
    public <T extends CustomPacketPayload> void initBidirectionalMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver) {
        registerPayload(registrar -> registrar.playBidirectional(type, codec, (payload, context) ->
                receiver.accept(payload, (ServerPlayer) context.player())));
    }

    @Override
    public <T extends CustomPacketPayload> void initServerboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            BiConsumer<T, ServerPlayer> receiver) {
        registerPayload(registrar -> registrar.playToServer(type, codec, (payload, context) ->
                receiver.accept(payload, (ServerPlayer) context.player())));
    }

    @Override
    public <T extends CustomPacketPayload> void initClientboundMessageChannel(
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        registerPayload(registrar -> registrar.playToClient(type, codec));
    }

    private void registerPayload(Consumer<PayloadRegistrar> registration) {
        var container = Objects.requireNonNull(ModLoadingContext.get().getActiveContainer(), "Active NeoForge mod container");
        var eventBus = Objects.requireNonNull(container.getEventBus(), "Active NeoForge mod event bus");
        eventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar("1").optional();
            registration.accept(registrar);
        });
    }

    @Override
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type.id());
    }

    @Override
    public void sendPluginMessage(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
