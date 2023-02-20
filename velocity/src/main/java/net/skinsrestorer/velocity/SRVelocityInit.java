/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.velocity;

import ch.jalu.injector.Injector;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRProxyPlatformInit;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.velocity.listener.ConnectListener;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.listener.ProxyMessageListener;

import javax.inject.Inject;

@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRVelocityInit implements SRProxyPlatformInit {
    private final Injector injector;
    private final SRVelocityAdapter adapter;
    private final SRPlugin plugin;
    private final ProxyServer proxy;

    @Override
    public void initSkinApplier() {
        SkinApplierVelocity skinApplierVelocity = injector.getSingleton(SkinApplierVelocity.class);
        plugin.registerSkinApplier(skinApplierVelocity, Player.class, Player::getUsername);
    }

    @Override
    public void initLoginProfileListener() {
        proxy.getEventManager().register(adapter.getPluginInstance(), injector.newInstance(GameProfileRequest.class));
    }

    @Override
    public void initConnectListener() {
        proxy.getEventManager().register(adapter.getPluginInstance(), injector.newInstance(ConnectListener.class));
    }

    @Override
    public void initMessageChannel() {
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:messagechannel"));
        proxy.getEventManager().register(adapter.getPluginInstance(), PluginMessageEvent.class, injector.getSingleton(ProxyMessageListener.class));
    }
}
