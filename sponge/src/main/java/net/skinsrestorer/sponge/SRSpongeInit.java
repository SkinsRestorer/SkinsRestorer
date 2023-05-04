/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.sponge;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlatformInit;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.sponge.listeners.LoginListener;
import net.skinsrestorer.sponge.listeners.ServerMessageListener;
import net.skinsrestorer.sponge.wrapper.WrapperSponge;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.service.permission.PermissionDescription;

import javax.inject.Inject;

@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRSpongeInit implements SRServerPlatformInit {
    private final Injector injector;
    private final SRSpongeAdapter adapter;
    private final SRPlugin plugin;
    private final Game game;
    private final WrapperSponge wrapper;
    private final SkinsRestorerLocale locale;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierSponge.class), ServerPlayer.class, wrapper::player);
    }

    @Override
    public void initLoginProfileListener() {
        game.eventManager().registerListener(EventListenerRegistration
                .builder(ServerSideConnectionEvent.Auth.class)
                .plugin(adapter.getPluginContainer())
                .order(Order.DEFAULT)
                .listener(injector.newInstance(LoginListener.class)).build());
    }

    @Override
    public void initPermissions() {
        // We need to delay this until the server is becoming available
        game.eventManager().registerListeners(adapter.getPluginContainer(), this);
    }

    @Override
    public void initGUIListener() {
        // Not needed on sponge because we bind events to inventories directly
    }

    @Override
    public void initMessageChannel() {
        game.channelManager().ofType(ResourceKey.of("sr", "messagechannel"), RawDataChannel.class)
                .play().addHandler(injector.newInstance(ServerMessageListener.class));
    }

    @Listener
    public void onEngineStarting(StartingEngineEvent<Server> event) {
        for (PermissionRegistry permission : PermissionRegistry.values()) {
            newDescriptionBuilder(permission.getPermission(), permission.getDescription()).register();
        }

        for (PermissionGroup group : PermissionGroup.values()) {
            String groupString = group == PermissionGroup.PLAYER ? PermissionDescription.ROLE_USER : PermissionDescription.ROLE_STAFF;
            newDescriptionBuilder(group.getBasePermission(), group.getDescription())
                    .assign(groupString, true)
                    .register();
        }
    }

    private PermissionDescription.Builder newDescriptionBuilder(Permission permission, Message description) {
        return game.server().serviceProvider().permissionService()
                .newDescriptionBuilder(adapter.getPluginContainer())
                .id(permission.getPermissionString())
                .description(Component.text(locale.getMessage(locale.getDefaultForeign(), description)));
    }
}
