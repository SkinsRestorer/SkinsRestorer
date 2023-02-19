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
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.utils.SharedMethods;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerVelocity extends SkinsRestorerProxyShared {
    private final Object pluginInstance; // Only for platform API use
    private final ProxyServer proxy;
    private final SkinApplierVelocity skinApplierVelocity;
    private final Metrics.Factory metricsFactory;
    private final SkinCommand skinCommand = new SkinCommand(this);
    private boolean outdated;

    public SkinsRestorerVelocity(Object pluginInstance, ProxyServer proxy, Metrics.Factory metricsFactory, Path dataFolder, Logger logger, PluginContainer container) {
        super(
                new Slf4jLoggerImpl(logger),
                false,
                container.getDescription().getVersion().orElse("Unknown"),
                "SkinsRestorerUpdater/Velocity",
                dataFolder
        );
        this.pluginInstance = pluginInstance;
        this.proxy = proxy;
        this.metricsFactory = metricsFactory;
        this.skinApplierVelocity = new SkinApplierVelocity(this);
        registerAPI();
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
        Config.load(dataFolder, getResource("config.yml"), logger);
        try {
            Message.load(localeManager, dataFolder, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        // Init listener
        proxy.getEventManager().register(pluginInstance, new ConnectListener(this));
        proxy.getEventManager().register(pluginInstance, new GameProfileRequest(this));

        // Init commands
        initCommands();

        // Init message channel
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:skinchange"));
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("sr:messagechannel"));
        proxy.getEventManager().register(pluginInstance, new PluginMessageListener(this));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, logger));
    }

    private void initCommands() {
        sharedInitCommands();

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
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
    public Collection<ISRPlayer> getOnlinePlayers() {
        return proxy.getAllPlayers().stream().map(WrapperVelocity::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public Optional<ISRProxyPlayer> getPlayer(String playerName) {
        return proxy.getPlayer(playerName).map(WrapperVelocity::wrapPlayer);
    }

    @Override
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        return new VelocityCommandManager(proxy, pluginInstance);
    }

    @Override
    protected void registerAPI() {
        new SkinsRestorerVelocityAPI();
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

    private class SkinsRestorerVelocityAPI extends SkinsRestorerAPIShared {
        public SkinsRestorerVelocityAPI() {
            super(SkinsRestorerVelocity.this, new WrapperFactoryVelocity(), VelocityProperty::new);
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierVelocity.applySkin(playerWrapper.get(Player.class), property);
        }
    }
}
