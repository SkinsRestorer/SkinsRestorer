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
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.interfaces.ISRProxyPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.serverinfo.Platform;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;
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
import net.skinsrestorer.velocity.command.GUICommand;
import net.skinsrestorer.velocity.command.SkinCommand;
import net.skinsrestorer.velocity.command.SrCommand;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.utils.VelocityProperty;
import net.skinsrestorer.velocity.utils.WrapperVelocity;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"knat", "AlexProgrammerDE", "Blackfire62", "McLive"})
public class SkinsRestorer implements ISRProxyPlugin {
    private final ProxyServer proxy;
    private final Metrics.Factory metricsFactory;
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final Path dataFolderPath;
    private final SRLogger srLogger;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SkinsRestorerAPI skinsRestorerAPI;
    private final MineSkinAPI mineSkinAPI;
    private final SkinApplierVelocity skinApplierVelocity;
    private UpdateChecker updateChecker;
    private final SkinCommand skinCommand = new SkinCommand(this);
    private CommandManager<?, ?, ?, ?, ?, ?> manager;
    @Inject
    private PluginContainer container;

    @Inject
    public SkinsRestorer(ProxyServer proxy, Metrics.Factory metricsFactory, @DataDirectory Path dataFolderPath, Logger logger) {
        this.proxy = proxy;
        this.metricsFactory = metricsFactory;
        this.dataFolderPath = dataFolderPath;
        srLogger = new SRLogger(new Slf4LoggerImpl(logger));
        mojangAPI = new MojangAPI(srLogger, Platform.VELOCITY, metricsCounter);
        mineSkinAPI = new MineSkinAPI(srLogger, metricsCounter);
        skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
        skinsRestorerAPI = new SkinsRestorerVelocityAPI();
        skinApplierVelocity = new SkinApplierVelocity(this, srLogger);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        srLogger.load(dataFolderPath);
        Path updaterDisabled = dataFolderPath.resolve("noupdate.txt");

        Metrics metrics = metricsFactory.make(this, 10606);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        // Check for updates
        if (!Files.exists(updaterDisabled)) {
            updateChecker = new UpdateCheckerGitHub(2124, getVersion(), srLogger, "SkinsRestorerUpdater/Velocity");
            checkUpdate(true);

            Random rn = new Random();
            int delayInt = 60 + rn.nextInt(240 - 60 + 1);
            proxy.getScheduler().buildTask(this, this::checkUpdate).repeat(delayInt, TimeUnit.MINUTES).delay(delayInt, TimeUnit.MINUTES).schedule();
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
        proxy.getEventManager().register(this, new GameProfileRequest(this));

        // Init commands
        initCommands();

        srLogger.info("Enabled SkinsRestorer v" + getVersion());

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    private void initCommands() {
        manager = new VelocityCommandManager(proxy, this);

        prepareACF(manager, srLogger);

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolderPath)) return false;

        // Preload default skins
        runAsync(skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        checkUpdate(false);
    }

    private void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false)
                        .forEach(srLogger::info);
            }

            @Override
            public void upToDate() {
                if (showUpToDate)
                    updateChecker.getUpToDateMessages(getVersion(), false)
                            .forEach(srLogger::info);
            }
        }));
    }

    @Override
    public String getVersion() {
        return container.getDescription().getVersion().orElse("Unknown");
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        proxy.getScheduler().buildTask(this, runnable).schedule();
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return proxy.getAllPlayers().stream().map(WrapperVelocity::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public void sendGuiOpenRequest(ISRProxyPlayer player) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("OPENGUI");
            out.writeUTF(player.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendDataToServer("sr:messagechannel", b.toByteArray());
    }

    @Override
    public Optional<ISRProxyPlayer> getPlayer(String playerName) {
        return proxy.getPlayer(playerName).map(WrapperVelocity::wrapPlayer);
    }

    private static class WrapperFactoryVelocity extends WrapperFactory {
        @Override
        public ISRPlayer wrapPlayer(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return WrapperVelocity.wrapPlayer(player);
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private static class PropertyFactoryVelocity implements IPropertyFactory {
        @Override
        public IProperty createProperty(String name, String value, String signature) {
            return new VelocityProperty(name, value, signature);
        }
    }

    private class SkinsRestorerVelocityAPI extends SkinsRestorerAPI {
        public SkinsRestorerVelocityAPI() {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactoryVelocity(), new PropertyFactoryVelocity());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(Player.class).getUsername());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String playerName) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(playerName));
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierVelocity.applySkin(playerWrapper.get(Player.class), property);
        }
    }
}
