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

import ch.jalu.injector.Injector;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.miniplaceholders.SRMiniPlaceholdersAPIExpansion;
import net.skinsrestorer.mod.listener.AdminInfoListener;
import net.skinsrestorer.mod.listener.PlayerJoinListener;
import net.skinsrestorer.mod.listener.ServerMessageListener;
import net.skinsrestorer.mod.wrapper.ModComponentHelper;
import net.skinsrestorer.mod.wrapper.WrapperMod;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.log.SRChatColor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlatformInit;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRModInit implements SRServerPlatformInit {
    public static final CustomPacketPayload.Type<RawBytePayload> SR_MESSAGE_CHANNEL = new CustomPacketPayload.Type<>(ResourceLocation.parse(SRHelpers.MESSAGE_CHANNEL));
    private final SRPlugin plugin;
    private final SRModAdapter adapter;
    private final Injector injector;
    private final SRLogger logger;
    private final WrapperMod wrapper;
    private final SkinsRestorerLocale locale;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierMod.class), ServerPlayer.class, wrapper);
        // Log information about the platform
        logger.info(SRChatColor.GREEN + "Running on Minecraft " + SRChatColor.YELLOW + Platform.getMinecraftVersion() + SRChatColor.GREEN + ".");
    }

    @Override
    public void initLoginProfileListener() {
        PlayerEvent.PLAYER_JOIN.register(injector.getSingleton(PlayerJoinListener.class));
    }

    @Override
    public void initAdminInfoListener() {
        PlayerEvent.PLAYER_JOIN.register(injector.getSingleton(AdminInfoListener.class));
    }

    @Override
    public void placeholderSetupHook() {
        if (adapter.getPluginInfo("miniplaceholders").isPresent()) {
            try {
                new SRMiniPlaceholdersAPIExpansion<>(
                        adapter,
                        ServerPlayer.class,
                        wrapper::player
                ).register();
                logger.info("MiniPlaceholders expansion registered!");
            } catch (Throwable t) {
                logger.severe("Failed to load MiniPlaceholders expansion! Please check if both SkinsRestorer and MiniPlaceholders are up-to-date.", t);
            }
        }
    }

    @Override
    public void checkPluginSupport() {
        checkViaVersion();
    }

    private void checkViaVersion() {
        Optional<PluginInfo> viaVersion = adapter.getPluginInfo("viaversion");
        Optional<PluginInfo> viaBackwards = adapter.getPluginInfo("viabackwards");
        if (viaVersion.isEmpty() || viaBackwards.isEmpty()) {
            return;
        }

        SemanticVersion viaVersionVersion = viaVersion.get().parsedVersion();
        SemanticVersion viaBackwardsVersion = viaBackwards.get().parsedVersion();
        SemanticVersion requiredVersion = new SemanticVersion(5, 0, 0);
        if (!viaVersionVersion.isOlderThan(requiredVersion) && !viaBackwardsVersion.isOlderThan(requiredVersion)) {
            return;
        }

        adapter.runRepeatAsync(() -> logger.severe("Outdated ViaVersion/ViaBackwards found! Please update to at least ViaVersion/ViaBackwards 5.0.0 for SkinsRestorer to work again!"),
                2, 60, TimeUnit.SECONDS);
    }

    @Override
    public void initPermissions() {
        for (PermissionRegistry permission : PermissionRegistry.values()) {
            Component description = ModComponentHelper.deserialize(locale.getMessageRequired(locale.getDefaultForeign(), permission.getDescription()));

            SRModPlatform.INSTANCE.registerPermission(permission.getPermission(), description);
        }

        for (PermissionGroup group : PermissionGroup.values()) {
            Component description = ModComponentHelper.deserialize(locale.getMessageRequired(locale.getDefaultForeign(), group.getDescription()));

            SRModPlatform.INSTANCE.registerPermission(group.getBasePermission(), description);
            SRModPlatform.INSTANCE.registerPermission(group.getWildcard(), description);
        }
    }

    @Override
    public void initGUIListener() {
        // Not needed, MC handles the GUI events inside the menu class
    }

    @Override
    public void initMessageChannel() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SR_MESSAGE_CHANNEL, RawBytePayload.STREAM_CODEC, injector.getSingleton(ServerMessageListener.class));
        NetworkManager.registerS2CPayloadType(
                SR_MESSAGE_CHANNEL,
                RawBytePayload.STREAM_CODEC
        );
    }

    public record RawBytePayload(byte[] data) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, RawBytePayload> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.data),
                buf -> {
                    var bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new RawBytePayload(bytes);
                }
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return SR_MESSAGE_CHANNEL;
        }
    }
}
