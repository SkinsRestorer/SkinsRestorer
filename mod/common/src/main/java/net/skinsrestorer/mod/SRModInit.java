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

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.miniplaceholders.SRMiniPlaceholdersAPIExpansion;
import net.skinsrestorer.mod.listener.AdminInfoListener;
import net.skinsrestorer.mod.listener.PlayerJoinListener;
import net.skinsrestorer.mod.skinshuffle.SkinShuffleHandshakePayload;
import net.skinsrestorer.mod.skinshuffle.SkinShuffleRefreshPlayerListEntryPayload;
import net.skinsrestorer.mod.skinshuffle.SkinShuffleSkinRefreshPayload;
import net.skinsrestorer.mod.wrapper.ModComponentHelper;
import net.skinsrestorer.mod.wrapper.WrapperMod;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleSupport;
import net.skinsrestorer.shared.listeners.SRServerMessageAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.log.SRChatColor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlatformInit;
import net.skinsrestorer.shared.subjects.SRServerPlayer;
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
    public static final CustomPacketPayload.Type<RawBytePayload> SR_MESSAGE_CHANNEL = new CustomPacketPayload.Type<>(Identifier.parse(SRHelpers.MESSAGE_CHANNEL));
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
        logger.info(SRChatColor.GREEN + "Running on Minecraft " + SRChatColor.YELLOW + SharedConstants.getCurrentVersion().name() + SRChatColor.GREEN + ".");
    }

    @Override
    public void initClientCompatibility(boolean proxyMode) {
        SkinShuffleSupport skinShuffleSupport = injector.getSingleton(SkinShuffleSupport.class);
        SRModPlatform.INSTANCE.initClientboundMessageChannel(
                SkinShuffleHandshakePayload.TYPE, SkinShuffleHandshakePayload.STREAM_CODEC);
        SRModPlatform.INSTANCE.initClientboundMessageChannel(
                SkinShuffleRefreshPlayerListEntryPayload.TYPE, SkinShuffleRefreshPlayerListEntryPayload.STREAM_CODEC);
        SRModPlatform.INSTANCE.initServerboundMessageChannel(
                SkinShuffleSkinRefreshPayload.TYPE, SkinShuffleSkinRefreshPayload.STREAM_CODEC,
                (payload, player) -> payload.toSkinProperty().ifPresentOrElse(
                        property -> skinShuffleSupport.handleSkinRefresh(wrapper.player(player), property),
                        () -> logger.warning("Ignoring invalid SkinShuffle skin refresh payload from %s (%s)."
                                .formatted(player.getGameProfile().name(), player.getGameProfile().id()))
                ));

        if (!proxyMode) {
            SRModPlatform.INSTANCE.registerPlayerJoinListener(player -> {
                if (!skinShuffleSupport.isEnabled() || !SRModPlatform.INSTANCE.canSend(player, SkinShuffleHandshakePayload.TYPE)) {
                    return;
                }

                SRModPlatform.INSTANCE.sendPluginMessage(player, SkinShuffleHandshakePayload.INSTANCE);
            });
        }
    }

    @Override
    public void initLoginProfileListener() {
        injector.getSingleton(PlayerJoinListener.class);
    }

    @Override
    public void initAdminInfoListener() {
        SRModPlatform.INSTANCE.registerPlayerJoinListener(injector.getSingleton(AdminInfoListener.class)::join);
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
        SRServerMessageAdapter messageAdapter = injector.getSingleton(SRServerMessageAdapter.class);
        SRModPlatform.INSTANCE.initBidirectionalMessageChannel(SR_MESSAGE_CHANNEL, RawBytePayload.STREAM_CODEC,
                (payload, player) -> messageAdapter.handlePluginMessage(new SRServerMessageEvent() {
                    @Override
                    public SRServerPlayer getPlayer() {
                        return wrapper.player(player);
                    }

                    @Override
                    public byte[] getData() {
                        return payload.data();
                    }

                    @Override
                    public String getChannel() {
                        return SR_MESSAGE_CHANNEL.id().toString();
                    }
                }));
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
