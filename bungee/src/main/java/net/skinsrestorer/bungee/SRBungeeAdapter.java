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
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.command.SRBungeeCommand;
import net.skinsrestorer.bungee.listeners.ForceAliveListener;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.commands.library.SRCommandMeta;
import net.skinsrestorer.shared.commands.library.SRRegisterPayload;
import net.skinsrestorer.shared.plugin.SRProxyAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import org.bstats.bungeecord.Metrics;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRBungeeAdapter implements SRProxyAdapter<Plugin> {
    private final Injector injector;
    private final ProxyServer proxy;
    @Getter
    private final Plugin pluginInstance; // Only for platform API use

    public SRBungeeAdapter(Injector injector, Plugin pluginInstance) {
        this.injector = injector;
        this.proxy = injector.getSingleton(ProxyServer.class);
        this.pluginInstance = pluginInstance;

        injector.register(SRBungeeAdapter.class, this);
        injector.register(SRProxyAdapter.class, this);
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
    public void extendLifeTime(Plugin plugin, Object object) {
        proxy.getPluginManager().registerListener(plugin, new ForceAliveListener(object));
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion();
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return SkinApplierBungee.getApplyAdapter().getSkinProperty(player.getAs(ProxiedPlayer.class));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);
        return proxy.getPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRProxyPlayer> getPlayer(String name) {
        return Optional.ofNullable(proxy.getPlayer(name)).map(p -> injector.getSingleton(WrapperBungee.class).player(p));
    }

    @Override
    public void registerCommand(SRRegisterPayload<SRCommandSender> payload) {
        proxy.getPluginManager().registerCommand(pluginInstance,
                new SRBungeeCommand(payload, injector.getSingleton(WrapperBungee.class)));
    }
}
