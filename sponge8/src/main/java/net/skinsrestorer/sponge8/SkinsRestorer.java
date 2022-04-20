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
package net.skinsrestorer.sponge8;

import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.serverinfo.Platform;
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
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.sponge8.commands.SkinCommand;
import net.skinsrestorer.sponge8.commands.SrCommand;
import net.skinsrestorer.sponge8.listeners.LoginListener;
import net.skinsrestorer.sponge8.utils.SpongeLoggerIml;
import net.skinsrestorer.sponge8.utils.WrapperSponge;
import org.apache.logging.log4j.Logger;
import org.bstats.charts.SingleLineChart;
import org.bstats.sponge.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// @Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"Blackfire62", "McLive"})
@Getter
@Plugin("skinsrestorer")
public class SkinsRestorer implements ISRPlugin {
    private static final boolean BUNGEE_ENABLED = false;
    private final Metrics metrics;
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);
    private final Path dataFolderPath;
    private final SRLogger srLogger;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SkinsRestorerAPI skinsRestorerAPI;
    private final MineSkinAPI mineSkinAPI;
    @Inject
    protected Game game;
    private UpdateChecker updateChecker;
    private SpongeCommandManager manager;
    @Inject
    private PluginContainer container;

    @Inject
    public SkinsRestorer(@SuppressWarnings("SpongeInjection") Metrics.Factory metricsFactory, @ConfigDir(sharedRoot = false) Path dataFolderPath, Logger log) {
        metrics = metricsFactory.make(2337);
        this.dataFolderPath = dataFolderPath;
        srLogger = new SRLogger(new SpongeLoggerIml(log));
        mojangAPI = new MojangAPI(srLogger, Platform.SPONGE, metricsCounter);
        mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
        skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
        skinsRestorerAPI = new SkinsRestorerSpongeAPI(mojangAPI, skinStorage);
    }

    @Listener
    public void onInitialize(StartingEngineEvent<Server> e) {
        srLogger.load(getDataFolderPath());
        Path updaterDisabled = dataFolderPath.resolve("noupdate.txt");

        // Check for updates
        if (!Files.exists(updaterDisabled)) {
            updateChecker = new UpdateCheckerGitHub(2124, getVersion(), srLogger, "SkinsRestorerUpdater/Sponge");
            checkUpdate();

            Random rn = new Random();
            int delayInt = 60 + rn.nextInt(240 - 60 + 1);
            Sponge.server().scheduler().submit(Task.builder().execute(() ->
                    checkUpdate(false)).interval(delayInt, TimeUnit.MINUTES).delay(delayInt, TimeUnit.MINUTES).plugin(container).build());
        } else {
            srLogger.info("Updater Disabled");
        }

        // Init config files
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        Locale.load(dataFolderPath, srLogger);

        // Init storage
        if (!initStorage())
            return;

        // Init commands
        initCommands();

        // Run connection check
        Sponge.asyncScheduler().executor(container).execute(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    @Listener
    public void onServerStarted(StartedEngineEvent<Server> event) {
        Sponge.eventManager().registerListener(EventListenerRegistration
                .builder(ServerSideConnectionEvent.Login.class)
                .plugin(container).listener(new LoginListener(this)).build());

        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));
    }

    private void initCommands() {
        manager = new SpongeCommandManager(container);

        prepareACF(manager, srLogger);

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolderPath)) return false;

        // Preload default skins
        Sponge.asyncScheduler().executor(container).execute(skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        checkUpdate(true);
    }

    private void checkUpdate(boolean showUpToDate) {
        Sponge.asyncScheduler().executor(container).execute(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), SkinsRestorer.BUNGEE_ENABLED).forEach(srLogger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), SkinsRestorer.BUNGEE_ENABLED).forEach(srLogger::info);
            }
        }));
    }

    @Override
    public String getVersion() {
        return container.metadata().version().getQualifier();
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        game.asyncScheduler().executor(container).execute(runnable);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return game.server().onlinePlayers().stream().map(WrapperSponge::wrapPlayer).collect(Collectors.toList());
    }

    private static class WrapperFactorySponge extends WrapperFactory {
        @Override
        public ISRPlayer wrapPlayer(Object playerInstance) {
            if (playerInstance instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) playerInstance;

                return WrapperSponge.wrapPlayer(player);
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private class SkinsRestorerSpongeAPI extends SkinsRestorerAPI {
        public SkinsRestorerSpongeAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactorySponge());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(Player.class).name());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name));
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierSponge.applySkin(playerWrapper.get(ServerPlayer.class), props);
        }
    }
}
