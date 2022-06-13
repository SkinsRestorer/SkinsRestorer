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
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.serverinfo.Platform;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.BungeeConsoleImpl;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.WrapperFactory;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.LoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin implements ISRPlugin {
    private final Path dataFolderPath = getDataFolder().toPath();
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SRLogger srLogger = new SRLogger(new LoggerImpl(getProxy().getLogger(), new BungeeConsoleImpl(getProxy().getConsole())), true);
    private final MojangAPI mojangAPI = new MojangAPI(srLogger, Platform.BUNGEECORD, metricsCounter);
    private final MineSkinAPI mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
    private final SkinStorage skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
    private final SkinsRestorerAPI skinsRestorerAPI = new SkinsRestorerBungeeAPI(mojangAPI, skinStorage);
    private final SkinApplierBungeeShared skinApplierBungee = selectSkinApplier(this, srLogger);
    private boolean outdated;
    private UpdateChecker updateChecker;
    private PluginMessageListener pluginMessageListener;
    private SkinCommand skinCommand;
    private BungeeCommandManager manager;
    private static final String NEW_PROPERTY_CLASS = "net.md_5.bungee.protocol.Property";

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

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void onEnable() {
        srLogger.load(dataFolderPath);
        Path updaterDisabled = dataFolderPath.resolve("noupdate.txt");

        Metrics metrics = new Metrics(this, 1686);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        if (!Files.exists(updaterDisabled)) {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/BungeeCord");
            checkUpdate(true);

            Random rn = new Random();
            int delayInt = 60 + rn.nextInt(240 - 60 + 1);
            getProxy().getScheduler().schedule(this, this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        } else {
            srLogger.info("Updater Disabled");
        }

        // Init config files
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        Locale.load(dataFolderPath, srLogger);

        // Init storage
        if (!initStorage())
            return;

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this));
        getProxy().getPluginManager().registerListener(this, new ConnectListener(this));

        // Init commands
        initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init message channel
        getProxy().registerChannel("sr:messagechannel");
        pluginMessageListener = new PluginMessageListener(this);
        getProxy().getPluginManager().registerListener(this, pluginMessageListener);

        // Run connection check
        getProxy().getScheduler().runAsync(this, () -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    private void initCommands() {
        manager = new BungeeCommandManager(this);

        prepareACF(manager, srLogger);

        this.skinCommand = new SkinCommand(this);
        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolderPath)) {
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            return false;
        }

        // Preload default skins
        ProxyServer.getInstance().getScheduler().runAsync(this, skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        checkUpdate(false);
    }

    private void checkUpdate(boolean showUpToDate) {
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(srLogger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), false).forEach(srLogger::info);
            }
        }));
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return getProxy().getPlayers().stream().map(WrapperBungee::wrapPlayer).collect(Collectors.toList());
    }

    private static class WrapperFactoryBungee extends WrapperFactory {
        @Override
        public ISRPlayer wrapPlayer(Object playerInstance) {
            if (playerInstance instanceof ProxiedPlayer) {
                ProxiedPlayer player = (ProxiedPlayer) playerInstance;

                return WrapperBungee.wrapPlayer(player);
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

    private class SkinsRestorerBungeeAPI extends SkinsRestorerAPI {
        public SkinsRestorerBungeeAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactoryBungee(), new PropertyFactoryBungee());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(ProxiedPlayer.class).getName());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name));
        }

        @SneakyThrows
        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierBungee.applySkin(playerWrapper.get(ProxiedPlayer.class), props);
        }
    }
}
