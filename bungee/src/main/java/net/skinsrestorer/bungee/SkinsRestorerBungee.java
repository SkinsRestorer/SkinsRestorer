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

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.CommandManager;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.BungeeConsoleImpl;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.injector.GetPlayerMethod;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerBungee extends SkinsRestorerProxyShared {
    private static final String NEW_PROPERTY_CLASS = "net.md_5.bungee.protocol.Property";
    private final ProxyServer proxy;
    private final Plugin pluginInstance; // Only for platform API use

    public SkinsRestorerBungee(Plugin pluginInstance, ProxyServer proxy, Path dataFolder, String version) {
        super(
                new JavaLoggerImpl(new BungeeConsoleImpl(proxy.getConsole()), proxy.getLogger()),
                true,
                version,
                "SkinsRestorer/BungeeCord",
                dataFolder
        );
        this.proxy = proxy;
        this.pluginInstance = pluginInstance;
    }

    /*
     * Starting the 1.19 builds of BungeeCord, the Property class has changed.
     * This method will check if the new class is available and return the appropriate class that was compiled for it.
     */
    private static SkinApplierBungeeShared selectSkinApplier(SettingsManager settings, SRLogger srLogger) {
        if (ReflectionUtil.classExists(NEW_PROPERTY_CLASS)) {
            return new SkinApplierBungeeNew(settings, srLogger);
        } else {
            return new SkinApplierBungeeOld(settings, srLogger);
        }
    }

    public void pluginStartup() {
        logger.load(dataFolder);

        Metrics metrics = new Metrics(pluginInstance, 1686);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        checkUpdateInit(() -> {
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeatAsync(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        });

        // Init config files
        loadConfig();
        loadLocales();

        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);
        injector.provide(GetPlayerMethod.class, (Function<String, Optional<ISRProxyPlayer>>)
                name -> Optional.ofNullable(proxy.getPlayer(name)).map(wrapper::player));
        injector.provide(OnlinePlayersMethod.class, (CallableValue<Collection<ISRPlayer>>)
                () -> proxy.getPlayers().stream().map(wrapper::player).collect(Collectors.toList()));

        initMineSkinAPI();

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        SkinApplierBungeeShared skinApplierBungee = selectSkinApplier(injector.getSingleton(SettingsManager.class), logger);
        injector.register(SkinApplierBungeeShared.class, skinApplierBungee);

        // Init API
        registerAPI(new WrapperFactoryBungee(), new PropertyFactoryBungee(), skinApplierBungee);

        // Init listener
        proxy.getPluginManager().registerListener(pluginInstance, injector.newInstance(LoginListener.class));
        proxy.getPluginManager().registerListener(pluginInstance, injector.newInstance(ConnectListener.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();

        manager.registerCommand(injector.getSingleton(SkinCommand.class));

        PluginMessageListener pluginMessageListener = injector.getSingleton(PluginMessageListener.class);

        manager.registerCommand(injector.newInstance(SrCommand.class));
        manager.registerCommand(injector.newInstance(GUICommand.class));

        // Init message channel
        proxy.registerChannel("sr:skinchange");
        proxy.registerChannel("sr:messagechannel");
        proxy.getPluginManager().registerListener(pluginInstance, pluginMessageListener);

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
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
        return new BungeeCommandManager(pluginInstance);
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
            if (ReflectionUtil.classExists(NEW_PROPERTY_CLASS)) {
                return new BungeePropertyNew(name, value, signature);
            } else {
                return new BungeePropertyOld(name, value, signature);
            }
        }
    }
}
