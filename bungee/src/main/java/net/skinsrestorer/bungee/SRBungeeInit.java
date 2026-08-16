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
package net.skinsrestorer.bungee;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.listeners.AdminInfoListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.ProxyMessageListener;
import net.skinsrestorer.bungee.listeners.SkinShuffleHandshakeListener;
import net.skinsrestorer.bungee.listeners.SkinShuffleProxyMessageListener;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRProxyPlatformInit;
import net.skinsrestorer.shared.plugin.SRProxyPluginSupportChecker;
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBungeeInit implements SRProxyPlatformInit {
    private final Injector injector;
    private final SRBungeeAdapter adapter;
    private final SRPlugin plugin;
    private final SRProxyPluginSupportChecker pluginSupportChecker;
    private final ProxyServer proxy;
    private final WrapperBungee wrapper;

    @Override
    public void initSkinApplier() {
        plugin.registerSkinApplier(injector.getSingleton(SkinApplierBungee.class), ProxiedPlayer.class, wrapper);
    }

    @Override
    public void initLoginProfileListener() {
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(LoginListener.class));
    }

    @Override
    public void checkPluginSupport() {
        pluginSupportChecker.checkViaVersionProxyInstall();
    }

    @Override
    public void initAdminInfoListener() {
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(AdminInfoListener.class));
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(SkinShuffleHandshakeListener.class));
    }

    @Override
    public void initMessageChannel() {
        proxy.registerChannel(SRHelpers.MESSAGE_CHANNEL);
        proxy.registerChannel(SkinShuffleChannels.SKIN_REFRESH);
        proxy.registerChannel(SkinShuffleChannels.HANDSHAKE);
        proxy.registerChannel(SkinShuffleChannels.REFRESH_PLAYER_LIST_ENTRY);
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.getSingleton(ProxyMessageListener.class));
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.getSingleton(SkinShuffleProxyMessageListener.class));
    }
}
