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
package net.skinsrestorer.bungee;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.ProxyMessageListener;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRProxyPlatformInit;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBungeeInit implements SRProxyPlatformInit {
    private final Injector injector;
    private final SRBungeeAdapter adapter;
    private final SRPlugin plugin;
    private final ProxyServer proxy;
    private final WrapperBungee wrapper;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierBungee.class), ProxiedPlayer.class, wrapper::player);
    }

    @Override
    public void initLoginProfileListener() {
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(LoginListener.class));
    }

    @Override
    public void initConnectListener() {
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(ConnectListener.class));
    }

    @Override
    public void initMessageChannel() {
        proxy.registerChannel("sr:messagechannel");
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.getSingleton(ProxyMessageListener.class));
    }
}
