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
package net.skinsrestorer.bungee;

import ch.jalu.injector.Injector;
import co.aikar.commands.CommandManager;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.shared.commands.ProxyGUICommand;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRPlatformStarter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBungeeStarter implements SRPlatformStarter {
    private final Injector injector;
    private final SRBungeeAdapter adapter;
    private final SRPlugin plugin;
    private final ProxyServer proxy;
    private final SRLogger logger;

    public void pluginStartup() {
        plugin.startupStart(ProxiedPlayer.class);

        plugin.initUpdateCheck();

        // Init config files
        plugin.loadConfig();
        plugin.loadLocales();

        plugin.initMineSkinAPI();

        // Init storage
        try {
            plugin.initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        plugin.registerSkinApplier(injector.getSingleton(SkinApplierBungee.class), ProxiedPlayer.class, ProxiedPlayer::getName);

        // Init API
        plugin.registerAPI();

        // Init listener
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(LoginListener.class));
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.newInstance(ConnectListener.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = plugin.sharedInitCommands();
        manager.registerCommand(injector.newInstance(ProxyGUICommand.class));

        // Init message channel
        proxy.registerChannel("sr:skinchange");
        proxy.registerChannel("sr:messagechannel");
        proxy.getPluginManager().registerListener(adapter.getPluginInstance(), injector.getSingleton(PluginMessageListener.class));

        // Run connection check
        adapter.runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPIImpl.class), logger));
    }
}
