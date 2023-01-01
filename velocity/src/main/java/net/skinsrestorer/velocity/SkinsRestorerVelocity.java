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

import co.aikar.commands.CommandManager;
import co.aikar.commands.VelocityCommandManager;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.util.GameProfile;
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.acf.OnlineISRPlayer;
import net.skinsrestorer.shared.commands.ProxyGUICommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
import net.skinsrestorer.velocity.listener.ConnectListener;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.listener.PluginMessageListener;
import net.skinsrestorer.velocity.utils.VelocityProperty;
import net.skinsrestorer.velocity.utils.WrapperVelocity;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerVelocity extends SkinsRestorerProxyShared {
    private final Object pluginInstance; // Only for platform API use
    private final ProxyServer proxy;
    private final Metrics.Factory metricsFactory;

    public SkinsRestorerVelocity(Object pluginInstance, ProxyServer proxy, Metrics.Factory metricsFactory, Path dataFolder, Logger logger, String version) {
        super(
                new Slf4jLoggerImpl(logger),
                false,
                version,
                "SkinsRestorerUpdater/Velocity",
                dataFolder,
                new WrapperFactoryVelocity(),
                VelocityProperty::new,
                Platform.VELOCITY
        );
        injector.register(SkinsRestorerVelocity.class, this);
        injector.register(ProxyServer.class, proxy);
        this.pluginInstance = pluginInstance;
        this.proxy = proxy;
        this.metricsFactory = metricsFactory;
    }

    public void pluginStartup() {
        startupStart();

        updateCheck();

        // Init config files
        loadConfig();
        loadLocales();

        initMineSkinAPI();

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        SkinApplierVelocity skinApplierVelocity = injector.getSingleton(SkinApplierVelocity.class);

        // Init API
        registerAPI(skinApplierVelocity);

        // Init listener
        proxy.getEventManager().register(pluginInstance, injector.newInstance(ConnectListener.class));
        proxy.getEventManager().register(pluginInstance, injector.newInstance(GameProfileRequest.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();
        manager.registerCommand(injector.newInstance(ProxyGUICommand.class));

        // Init message channel
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:skinchange"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:messagechannel"));
        proxy.getEventManager().register(pluginInstance, PluginMessageEvent.class, injector.getSingleton(PluginMessageListener.class));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
    }

    @Override
    protected Object createMetricsInstance() {
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
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        VelocityCommandManager manager = new VelocityCommandManager(proxy, pluginInstance);

        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(ISRPlayer.class, c -> {
            Object playerObject = playerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return wrapper.player((Player) playerObject);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSource.class);
        manager.getCommandContexts().registerIssuerAwareContext(ISRCommandSender.class, c -> {
            Object commandSenderObject = commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender((CommandSource) commandSenderObject);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineISRPlayer.class, c -> {
            Object playerObject = onlinePlayerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return new OnlineISRPlayer(wrapper.player(((OnlinePlayer) playerObject).getPlayer()));
        });

        return manager;
    }

    @Override
    protected ISRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperVelocity.class).commandSender((CommandSource) sender);
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion().getVersion();
    }

    @Override
    public String getProxyModeInfo() {
        return "Velocity-Plugin";
    }

    @Override
    public List<IProperty> getPropertiesOfPlayer(ISRPlayer player) {
        List<GameProfile.Property> prop = player.getWrapper().get(Player.class).getGameProfileProperties();

        if (prop == null) {
            return Collections.emptyList();
        }

        return prop.stream().map(VelocityProperty::new).collect(Collectors.toList());
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        return proxy.getAllPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<ISRProxyPlayer> getPlayer(String name) {
        return proxy.getPlayer(name).map(injector.getSingleton(WrapperVelocity.class)::player);
    }

    private static class WrapperFactoryVelocity implements IWrapperFactory {
        @Override
        public String getPlayerName(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return player.getUsername();
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }
}
