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
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.PlayerWrapper;
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
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerProxyShared;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerBungee extends SkinsRestorerProxyShared {
    private static final String NEW_PROPERTY_CLASS = "net.md_5.bungee.protocol.Property";
    private final SkinApplierBungeeShared skinApplierBungee = selectSkinApplier(this, logger);
    private final SkinCommand skinCommand = new SkinCommand(this);
    private final ProxyServer proxy;
    private final Plugin pluginInstance; // Only for platform API use
    private boolean outdated;

    public SkinsRestorerBungee(Plugin plugin) {
        super(
                new JavaLoggerImpl(new BungeeConsoleImpl(plugin.getProxy().getConsole()), plugin.getProxy().getLogger()),
                true,
                plugin.getDescription().getVersion(),
                "SkinsRestorer/BungeeCord",
                plugin.getDataFolder().toPath()
        );
        this.proxy = plugin.getProxy();
        this.pluginInstance = plugin;
        registerAPI();
    }

    /*
     * Starting the 1.19 builds of BungeeCord, the Property class has changed.
     * This method will check if the new class is available and return the appropriate class that was compiled for it.
     */
    private static SkinApplierBungeeShared selectSkinApplier(ISRPlugin plugin, SRLogger srLogger) {
        if (ReflectionUtil.classExists(NEW_PROPERTY_CLASS)) {
            return new SkinApplierBungeeNew(plugin, srLogger);
        } else {
            return new SkinApplierBungeeOld(plugin, srLogger);
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
        Config.load(dataFolder, getResource("config.yml"), logger);
        Message.load(localeManager, dataFolder, this);

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        // Init listener
        proxy.getPluginManager().registerListener(pluginInstance, new LoginListener(this));
        proxy.getPluginManager().registerListener(pluginInstance, new ConnectListener(this));

        // Init commands
        initCommands();

        // Init message channel
        proxy.registerChannel("sr:skinchange");
        proxy.registerChannel("sr:messagechannel");
        proxy.getPluginManager().registerListener(pluginInstance, new PluginMessageListener(this));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, logger));
    }

    private void initCommands() {
        sharedInitCommands();

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version, false)
                        .forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(version, false).forEach(logger::info);
            }
        }));
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
    public Collection<ISRPlayer> getOnlinePlayers() {
        return proxy.getPlayers().stream().map(WrapperBungee::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public Optional<ISRProxyPlayer> getPlayer(String playerName) {
        return Optional.ofNullable(proxy.getPlayer(playerName)).map(WrapperBungee::wrapPlayer);
    }

    @Override
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        return new BungeeCommandManager(pluginInstance);
    }

    @Override
    protected void registerAPI() {
        new SkinsRestorerBungeeAPI();
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

    private class SkinsRestorerBungeeAPI extends SkinsRestorerAPIShared {
        public SkinsRestorerBungeeAPI() {
            super(SkinsRestorerBungee.this, new WrapperFactoryBungee(), new PropertyFactoryBungee());
        }

        @SneakyThrows
        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierBungee.applySkin(playerWrapper.get(ProxiedPlayer.class), property);
        }
    }
}
