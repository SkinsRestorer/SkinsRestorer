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
import co.aikar.commands.CommandManager;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.commands.ProxyGUICommand;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRPlatformStarter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.velocity.listener.ConnectListener;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.listener.PluginMessageListener;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRVelocityStarter implements SRPlatformStarter {
    private final Injector injector;
    private final SRVelocityAdapter adapter;
    private final SRPlugin plugin;
    private final ProxyServer proxy;
    private final SRLogger logger;

    @Override
    public void pluginStartup() {
        plugin.startupStart();

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

        SkinApplierVelocity skinApplierVelocity = injector.getSingleton(SkinApplierVelocity.class);
        plugin.registerSkinApplier(skinApplierVelocity, Player.class, Player::getUsername);

        // Init API
        plugin.registerAPI();

        // Init listener
        proxy.getEventManager().register(adapter.getPluginInstance(), injector.newInstance(ConnectListener.class));
        proxy.getEventManager().register(adapter.getPluginInstance(), injector.newInstance(GameProfileRequest.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = plugin.sharedInitCommands();
        manager.registerCommand(injector.newInstance(ProxyGUICommand.class));

        // Init message channel
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:skinchange"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:messagechannel"));
        proxy.getEventManager().register(adapter.getPluginInstance(), PluginMessageEvent.class, injector.getSingleton(PluginMessageListener.class));

        // Run connection check
        adapter.runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPIImpl.class), logger));
    }
}
