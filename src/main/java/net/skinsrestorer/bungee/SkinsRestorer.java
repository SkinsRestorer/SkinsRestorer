/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.LoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.shared.utils.log.console.BungeeConsoleImpl;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.File;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Getter
@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin implements ISRPlugin {
    private final File configPath = getDataFolder();
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SRLogger srLogger = new SRLogger(getDataFolder(), new LoggerImpl(getProxy().getLogger(), new BungeeConsoleImpl(getProxy().getConsole())), true);
    private final MojangAPI mojangAPI = new MojangAPI(srLogger, Platform.BUNGEECORD, metricsCounter);
    private final SkinStorage skinStorage = new SkinStorage(srLogger, mojangAPI);
    private final SkinsRestorerAPI skinsRestorerAPI = new SkinsRestorerBungeeAPI(mojangAPI, skinStorage);
    private final MineSkinAPI mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
    private final SkinApplierBungee skinApplierBungee = new SkinApplierBungee(this, srLogger);
    private boolean multiBungee;
    private boolean outdated;
    private UpdateChecker updateChecker;
    private PluginMessageListener pluginMessageListener;
    private SkinCommand skinCommand;
    private BungeeCommandManager manager;

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void onEnable() {
        srLogger.load(getDataFolder());
        File updaterDisabled = new File(getDataFolder(), "noupdate.txt");

        Metrics metrics = new Metrics(this, 1686);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        if (!updaterDisabled.exists()) {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/BungeeCord");
            checkUpdate(true);

            Random rn = new Random();
            int delayInt = 60 + rn.nextInt(240 - 60 + 1);
            getProxy().getScheduler().schedule(this, this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        } else {
            srLogger.info("Updater Disabled");
        }

        // Init config files
        Config.load(getDataFolder(), getResource("config.yml"), srLogger);
        Locale.load(getDataFolder(), srLogger);

        // Init storage
        if (!initStorage())
            return;

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this, srLogger));

        // Init commands
        initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init message channel
        getProxy().registerChannel("sr:messagechannel");
        pluginMessageListener = new PluginMessageListener(this);
        getProxy().getPluginManager().registerListener(this, pluginMessageListener);

        multiBungee = Config.MULTI_BUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        // Run connection check
        SharedMethods.runServiceCheck(mojangAPI, srLogger);
    }

    private void initCommands() {
        manager = new BungeeCommandManager(this);

        prepareACF(manager, srLogger);

        this.skinCommand = new SkinCommand(this, srLogger);
        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this, srLogger));
        manager.registerCommand(new GUICommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, getDataFolder())) {
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

    private class SkinsRestorerBungeeAPI extends SkinsRestorerAPI {
        public SkinsRestorerBungeeAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, mineSkinAPI, skinStorage);
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(ProxiedPlayer.class).getName());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name, false).get()); // FIXME
        }

        @SneakyThrows
        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierBungee.applySkin(playerWrapper.get(ProxiedPlayer.class), props);
        }
    }
}
