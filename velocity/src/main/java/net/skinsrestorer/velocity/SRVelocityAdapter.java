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
import co.aikar.commands.VelocityCommandManager;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.acf.OnlineSRPlayer;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRProxyAdapter;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;
import net.skinsrestorer.velocity.utils.WrapperVelocity;
import org.bstats.velocity.Metrics;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SRVelocityAdapter implements SRProxyAdapter {
    private final Injector injector;
    private final Object pluginInstance; // Only for platform API use
    private final ProxyServer proxy;
    private final Metrics.Factory metricsFactory;

    public SRVelocityAdapter(Injector injector, Object pluginInstance, ProxyServer proxy, Metrics.Factory metricsFactory) {
        this.injector = injector;
        this.pluginInstance = pluginInstance;
        this.proxy = proxy;
        this.metricsFactory = metricsFactory;

        injector.register(SRVelocityAdapter.class, this);
        injector.register(SRProxyAdapter.class, this);
        injector.register(ProxyServer.class, proxy);
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
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        VelocityCommandManager manager = new VelocityCommandManager(proxy, pluginInstance);

        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            Object playerObject = playerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return wrapper.player((Player) playerObject);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSource.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            Object commandSenderObject = commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender((CommandSource) commandSenderObject);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineSRPlayer.class, c -> {
            Object playerObject = onlinePlayerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return new OnlineSRPlayer(wrapper.player(((OnlinePlayer) playerObject).getPlayer()));
        });

        return manager;
    }

    @Override
    public SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperVelocity.class).commandSender((CommandSource) sender);
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion().getVersion();
    }

    @Override
    public List<SkinProperty> getPropertiesOfPlayer(SRPlayer player) {
        List<GameProfile.Property> prop = player.getAs(Player.class).getGameProfileProperties();

        if (prop == null) {
            return Collections.emptyList();
        }

        return prop.stream().filter(p -> p.getName().equals(SkinProperty.TEXTURES_NAME))
                .map(p -> SkinProperty.of(p.getValue(), p.getSignature())).collect(Collectors.toList());
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        return proxy.getAllPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRProxyPlayer> getPlayer(String name) {
        return proxy.getPlayer(name).map(injector.getSingleton(WrapperVelocity.class)::player);
    }
}