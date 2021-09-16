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
package net.skinsrestorer.sponge;

import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
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
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.shared.utils.log.Slf4LoggerImpl;
import net.skinsrestorer.sponge.commands.SkinCommand;
import net.skinsrestorer.sponge.commands.SrCommand;
import net.skinsrestorer.sponge.listeners.LoginListener;
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
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Getter
@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = "{version}", description = "{description}", url = "{url}", authors = {"Blackfire62", "McLive"})
public class SkinsRestorer implements ISRPlugin {
    private static final boolean BUNGEE_ENABLED = false;
    private final Metrics metrics;
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);
    @Inject
    protected Game game;
    private final File dataFolder;
    private final SRLogger srLogger;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SkinsRestorerAPI skinsRestorerAPI;
    private final MineSkinAPI mineSkinAPI;
    private UpdateChecker updateChecker;
    private SpongeCommandManager manager;
    @Inject
    private PluginContainer container;

    // The metricsFactory parameter gets injected using @Inject
    @Inject
    public SkinsRestorer(Metrics.Factory metricsFactory, @ConfigDir(sharedRoot = false) Path dataFolderPath, Logger log) {
        metrics = metricsFactory.make(2337);
        dataFolder = dataFolderPath.toFile();
        srLogger = new SRLogger(this.dataFolder, new Slf4LoggerImpl(log));
        mojangAPI = new MojangAPI(srLogger, Platform.SPONGE, metricsCounter);
        skinStorage = new SkinStorage(srLogger, mojangAPI);
        skinsRestorerAPI = new SkinsRestorerSpongeAPI(mojangAPI, skinStorage);
        mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
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
        SharedMethods.runServiceCheck(mojangAPI, srLogger);
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

            manager.registerCommand(new SkinCommand(this, srLogger));
            manager.registerCommand(new SrCommand(this, srLogger));
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

    public Text parseMessage(String msg) {
        return Text.builder(msg).build();
    }

    @Override
    public String getVersion() {
        Optional<PluginContainer> plugin = Sponge.getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getVersion();

        return version.orElse("");
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    private class SkinsRestorerSpongeAPI extends SkinsRestorerAPI {
        public SkinsRestorerSpongeAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, mineSkinAPI, skinStorage);
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(Player.class).getName());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name, false));
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierSponge.applySkin(playerWrapper.get(Player.class), props);
        }
    }
}
