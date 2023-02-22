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
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.CommandManager;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.acf.OnlineSRPlayer;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRProxyAdapter;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;
import org.bstats.bungeecord.Metrics;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRBungeeAdapter implements SRProxyAdapter {
    private final Injector injector;
    private final ProxyServer proxy;
    @Getter
    private final Plugin pluginInstance; // Only for platform API use

    public SRBungeeAdapter(Injector injector, Plugin pluginInstance, ProxyServer proxy) {
        this.injector = injector;
        this.proxy = proxy;
        this.pluginInstance = pluginInstance;

        injector.register(SRBungeeAdapter.class, this);
        injector.register(SRProxyAdapter.class, this);
        injector.register(ProxyServer.class, proxy);
    }

    @Override
    public Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1686);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName) != null;
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        proxy.getScheduler().runAsync(pluginInstance, runnable);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        proxy.getScheduler().schedule(pluginInstance, runnable, delay, interval, timeUnit);
    }

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        BungeeCommandManager manager = new BungeeCommandManager(pluginInstance);

        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);

        val playerResolver = manager.getCommandContexts().getResolver(ProxiedPlayer.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            ProxiedPlayer player = (ProxiedPlayer) playerResolver.getContext(c);
            if (player == null) {
                return null;
            }
            return wrapper.player(player);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSender.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            CommandSender commandSenderObject = (CommandSender) commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender(commandSenderObject);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineSRPlayer.class, c -> {
            OnlinePlayer onlinePlayer = (OnlinePlayer) onlinePlayerResolver.getContext(c);
            if (onlinePlayer == null) {
                return null;
            }
            return new OnlineSRPlayer(wrapper.player(onlinePlayer.getPlayer()));
        });

        return manager;
    }

    @Override
    public SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperBungee.class).commandSender((CommandSender) sender);
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion();
    }

    @Override
    public List<SkinProperty> getPropertiesOfPlayer(SRPlayer player) {
        return injector.getSingleton(SkinApplierBungee.class).getAdapter().getProperties(player.getAs(ProxiedPlayer.class));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);
        return proxy.getPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRProxyPlayer> getPlayer(String name) {
        ProxiedPlayer player = proxy.getPlayer(name);
        if (player == null) {
            return Optional.empty();
        } else {
            return Optional.of(injector.getSingleton(WrapperBungee.class).player(player));
        }
    }
}
