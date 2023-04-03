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
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import lombok.Getter;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.commands.library.PlatformRegistration;
import net.skinsrestorer.shared.plugin.SRProxyAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.velocity.listener.ForceAliveListener;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;
import org.bstats.velocity.Metrics;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SRVelocityAdapter implements SRProxyAdapter<PluginContainer> {
    private final Injector injector;
    private final Object pluginInstance; // Only for platform API use
    private final ProxyServer proxy;
    private final Metrics.Factory metricsFactory;

    public SRVelocityAdapter(Injector injector, Object pluginInstance, Metrics.Factory metricsFactory) {
        this.injector = injector;
        this.pluginInstance = pluginInstance;
        this.proxy = injector.getSingleton(ProxyServer.class);
        this.metricsFactory = metricsFactory;

        injector.register(SRVelocityAdapter.class, this);
        injector.register(SRProxyAdapter.class, this);
    }

    @Override
    public Object createMetricsInstance() {
        return metricsFactory.make(pluginInstance, 10606);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName).isPresent();
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        proxy.getScheduler().buildTask(pluginInstance, runnable).schedule();
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        proxy.getScheduler().buildTask(pluginInstance, runnable).delay(delay, timeUnit).repeat(interval, timeUnit).schedule();
    }

    @Override
    public void extendLifeTime(PluginContainer plugin, Object object) {
        proxy.getEventManager().register(plugin, ProxyShutdownEvent.class, PostOrder.LAST, new ForceAliveListener(object));
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion().getVersion();
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        List<GameProfile.Property> prop = player.getAs(Player.class).getGameProfileProperties();

        return prop.stream().filter(p -> p.getName().equals(SkinProperty.TEXTURES_NAME))
                .map(p -> SkinProperty.of(p.getValue(), p.getSignature())).findFirst();
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        return proxy.getAllPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRProxyPlayer> getPlayer(String name) {
        return proxy.getPlayer(name).map(injector.getSingleton(WrapperVelocity.class)::player)
                .or(() -> proxy.getAllPlayers().stream()
                        .filter(p -> p.getUsername().equalsIgnoreCase(name))
                        .findFirst().map(injector.getSingleton(WrapperVelocity.class)::player));
    }

    @Override
    public void registerCommand(PlatformRegistration<SRCommandSender> registration) {
        CommandMeta meta = proxy.getCommandManager()
                .metaBuilder(registration.getRootNode())
                .plugin(pluginInstance)
                .aliases(registration.getAliases()).build();
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);

        proxy.getCommandManager().register(meta, new RawCommand() {
            @Override
            public void execute(Invocation invocation) {
                registration.getExecutor().execute(wrapper.commandSender(invocation.source()), invocation.arguments());
            }

            @Override
            public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
                return registration.getExecutor().tabComplete(wrapper.commandSender(invocation.source()), invocation.arguments());
            }

            @Override
            public boolean hasPermission(Invocation invocation) {
                if (registration.getRootPermission() == null) {
                    return true;
                }

                return invocation.source().hasPermission(registration.getRootPermission());
            }
        });
    }
}
