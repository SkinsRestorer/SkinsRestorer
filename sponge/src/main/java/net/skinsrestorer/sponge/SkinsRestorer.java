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
package net.skinsrestorer.sponge;

import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.serverinfo.Platform;
import net.skinsrestorer.builddata.BuildData;
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
import net.skinsrestorer.shared.utils.log.Slf4LoggerImpl;
import net.skinsrestorer.sponge.commands.SkinCommand;
import net.skinsrestorer.sponge.commands.SrCommand;
import net.skinsrestorer.sponge.listeners.LoginListener;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.bstats.charts.SingleLineChart;
import org.bstats.sponge.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"Blackfire62", "McLive"})
public class SkinsRestorer implements ISRPlugin {
    private static final boolean BUNGEE_ENABLED = false;
    private final Metrics metrics;
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);
    private final File dataFolder;
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
        dataFolder = dataFolderPath.toFile();
        srLogger = new SRLogger(this.dataFolder, new Slf4LoggerImpl(log));
        mojangAPI = new MojangAPI(srLogger, Platform.SPONGE, metricsCounter);
        mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
        skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
        skinsRestorerAPI = new SkinsRestorerSpongeAPI(mojangAPI, skinStorage);
    }

    @Listener
    public void onInitialize(GameInitializationEvent e) {
        srLogger.load(getDataFolder());
        File updaterDisabled = new File(dataFolder, "noupdate.txt");

        // Check for updates
        if (!updaterDisabled.exists()) {
            updateChecker = new UpdateCheckerGitHub(2124, getVersion(), srLogger, "SkinsRestorerUpdater/Sponge");
            checkUpdate();

            Random rn = new Random();
            int delayInt = 60 + rn.nextInt(240 - 60 + 1);
            Sponge.getScheduler().createTaskBuilder().execute(() ->
                    checkUpdate(false)).interval(delayInt, TimeUnit.MINUTES).delay(delayInt, TimeUnit.MINUTES);
        } else {
            srLogger.info("Updater Disabled");
        }

        // Init config files
        Config.load(dataFolder, getResource("config.yml"), srLogger);
        Locale.load(dataFolder, srLogger);

        // Init storage
        if (!initStorage())
            return;

        // Init commands
        initCommands();

        // Run connection check
        Sponge.getScheduler().createAsyncExecutor(this).execute(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        if (!Sponge.getServer().getOnlineMode()) {
            Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Auth.class, new LoginListener(this, srLogger));
        }

        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));
    }

    private void initCommands() {
        Sponge.getPluginManager().getPlugin("skinsrestorer").ifPresent(pluginContainer -> {
            manager = new SpongeCommandManager(pluginContainer);

            prepareACF(manager, srLogger);

            manager.registerCommand(new SkinCommand(this));
            manager.registerCommand(new SrCommand(this));
        });
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolder)) return false;

        // Preload default skins
        Sponge.getScheduler().createAsyncExecutor(this).execute(skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        checkUpdate(true);
    }

    private void checkUpdate(boolean showUpToDate) {
        Sponge.getScheduler().createAsyncExecutor(this).execute(() -> updateChecker.checkForUpdate(new UpdateCallback() {
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
        return container.getVersion().orElse("Unknown");
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        game.getScheduler().createAsyncExecutor(this).execute(runnable);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return game.getServer().getOnlinePlayers().stream().map(WrapperSponge::wrapPlayer).collect(Collectors.toList());
    }

    private static class WrapperFactorySponge extends WrapperFactory {
        @Override
        public ISRPlayer wrapPlayer(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

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
            applySkin(playerWrapper, playerWrapper.get(Player.class).getName());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name));
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierSponge.applySkin(playerWrapper.get(Player.class), props);
        }
    }
}
