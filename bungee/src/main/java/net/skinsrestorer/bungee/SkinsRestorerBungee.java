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

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.CommandManager;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.BungeeConsoleImpl;
import net.skinsrestorer.bungee.utils.SkinApplierBungee;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.acf.OnlineISRPlayer;
import net.skinsrestorer.shared.commands.ProxyGUICommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import org.bstats.bungeecord.Metrics;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerBungee extends SkinsRestorerProxyShared {
    private final ProxyServer proxy;
    private final Plugin pluginInstance; // Only for platform API use

    public SkinsRestorerBungee(Plugin pluginInstance, ProxyServer proxy, Path dataFolder, String version) {
        super(
                new JavaLoggerImpl(new BungeeConsoleImpl(proxy.getConsole()), proxy.getLogger()),
                true,
                version,
                "SkinsRestorer/BungeeCord",
                dataFolder,
                new WrapperFactoryBungee(),
                new PropertyFactoryBungee(),
                Platform.BUNGEECORD
        );
        injector.register(SkinsRestorerBungee.class, this);
        injector.register(ProxyServer.class, proxy);
        this.proxy = proxy;
        this.pluginInstance = pluginInstance;
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

        // Init API
        registerAPI(injector.getSingleton(SkinApplierBungee.class));

        // Init listener
        proxy.getPluginManager().registerListener(pluginInstance, injector.newInstance(LoginListener.class));
        proxy.getPluginManager().registerListener(pluginInstance, injector.newInstance(ConnectListener.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();
        manager.registerCommand(injector.newInstance(ProxyGUICommand.class));

        // Init message channel
        proxy.registerChannel("sr:skinchange");
        proxy.registerChannel("sr:messagechannel");
        proxy.getPluginManager().registerListener(pluginInstance, injector.getSingleton(PluginMessageListener.class));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
    }

    @Override
    protected Object createMetricsInstance() {
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
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        BungeeCommandManager manager = new BungeeCommandManager(pluginInstance);

        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);

        val playerResolver = manager.getCommandContexts().getResolver(ProxiedPlayer.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            Object playerObject = playerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return wrapper.player((ProxiedPlayer) playerObject);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSender.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            Object commandSenderObject = commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender((CommandSender) commandSenderObject);
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
    protected SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperBungee.class).commandSender((CommandSender) sender);
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion();
    }

    @Override
    public String getProxyModeInfo() {
        return "Bungee-Plugin";
    }

    @Override
    public List<IProperty> getPropertiesOfPlayer(SRPlayer player) {
        List<IProperty> props = injector.getSingleton(SkinApplierBungee.class).getAdapter().getProperties(player.getWrapper().get(ProxiedPlayer.class));

        if (props == null) {
            return Collections.emptyList();
        } else {
            return props.stream()
                    .map(property -> new GenericProperty(property.getName(), property.getValue(), property.getSignature()))
                    .collect(Collectors.toList());
        }
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

    private static class WrapperFactoryBungee implements IWrapperFactory {
        @Override
        public String getPlayerName(Object playerInstance) {
            if (playerInstance instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) playerInstance;

                return player.getName();
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private static class PropertyFactoryBungee implements IPropertyFactory {
        @Override
        public IProperty createProperty(String name, String value, String signature) {
            if (ReflectionUtil.classExists(SkinApplierBungee.NEW_PROPERTY_CLASS)) {
                return new BungeePropertyNew(name, value, signature);
            } else {
                return new BungeePropertyOld(name, value, signature);
            }
        }
    }
}
