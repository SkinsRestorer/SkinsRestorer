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
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.shared.commands.SharedSkinCommand;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.injector.GetPlayerMethod;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.listeners.SharedPluginMessageListener;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
import net.skinsrestorer.velocity.command.GUICommand;
import net.skinsrestorer.velocity.command.SkinCommand;
import net.skinsrestorer.velocity.command.SrCommand;
import net.skinsrestorer.velocity.listener.ConnectListener;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.listener.PluginMessageListener;
import net.skinsrestorer.velocity.utils.VelocityProperty;
import net.skinsrestorer.velocity.utils.WrapperVelocity;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
                dataFolder
        );
        injector.register(SkinsRestorerVelocity.class, this);
        injector.register(ProxyServer.class, proxy);
        this.pluginInstance = pluginInstance;
        this.proxy = proxy;
        this.metricsFactory = metricsFactory;
    }

    public void pluginStartup() {
        logger.load(dataFolder);

        Metrics metrics = metricsFactory.make(pluginInstance, 10606);
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

        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        injector.provide(GetPlayerMethod.class, (Function<String, Optional<ISRProxyPlayer>>)
                name -> proxy.getPlayer(name).map(wrapper::player));
        injector.provide(OnlinePlayersMethod.class, (CallableValue<Collection<ISRPlayer>>)
                () -> proxy.getAllPlayers().stream().map(wrapper::player).collect(Collectors.toList()));

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
        registerAPI(new WrapperFactoryVelocity(), VelocityProperty::new, skinApplierVelocity);

        // Init listener
        proxy.getEventManager().register(pluginInstance, injector.newInstance(ConnectListener.class));
        proxy.getEventManager().register(pluginInstance, injector.newInstance(GameProfileRequest.class));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();

        SkinCommand skinCommand = injector.getSingleton(SkinCommand.class);
        injector.register(SharedSkinCommand.class, skinCommand);
        manager.registerCommand(skinCommand);
        PluginMessageListener pluginMessageListener = injector.getSingleton(PluginMessageListener.class);
        injector.register(SharedPluginMessageListener.class, pluginMessageListener);
        manager.registerCommand(injector.newInstance(SrCommand.class));
        manager.registerCommand(injector.newInstance(GUICommand.class));

        // Init message channel
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:skinchange"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:messagechannel"));
        proxy.getEventManager().register(pluginInstance, pluginMessageListener);

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
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
        return new VelocityCommandManager(proxy, pluginInstance);
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
