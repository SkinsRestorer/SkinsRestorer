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
import co.aikar.locales.LocaleManager;
import com.google.inject.Inject;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"knat", "AlexProgrammerDE", "Blackfire62", "McLive"})
public class SkinsRestorer implements ISRPlugin {
    private static final boolean BUNGEE_ENABLED = false;
    private final Metrics metrics;
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final CooldownStorage cooldownStorage = new CooldownStorage();
    private final SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);
    private final Path dataFolderPath;
    private final SRLogger srLogger;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SkinsRestorerAPI skinsRestorerAPI;
    private final MineSkinAPI mineSkinAPI;
    private final SkinCommand skinCommand;
    @Inject
    protected Game game;
    private LocaleManager<ISRForeign> localeManager;
    private UpdateChecker updateChecker;
    private SpongeCommandManager manager;
    @Inject
    private PluginContainer container;

    @Inject
    public SkinsRestorer(@SuppressWarnings("SpongeInjection") Metrics.Factory metricsFactory, @ConfigDir(sharedRoot = false) Path dataFolderPath, Logger log) {
        metrics = metricsFactory.make(2337);
        this.dataFolderPath = dataFolderPath;
        srLogger = new SRLogger(new Slf4jLoggerImpl(log));
        mojangAPI = new MojangAPI(srLogger, metricsCounter);
        mineSkinAPI = new MineSkinAPI(srLogger, metricsCounter);
        skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
        skinsRestorerAPI = new SkinsRestorerSpongeAPI();
        skinCommand = new SkinCommand(this);
    }

    @Listener
    public void onInitialize(GameInitializationEvent e) {
        srLogger.load(getDataFolderPath());

        checkUpdateInit(() -> {
            updateChecker = new UpdateCheckerGitHub(2124, getVersion(), srLogger, "SkinsRestorerUpdater/Sponge");
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeat(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        });

        // Init config files
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        localeManager = LocaleManager.create(ISRForeign::getLocale, Config.LANGUAGE);
        Message.load(localeManager, dataFolderPath, this);

        // Init storage
        if (!initStorage())
            return;

        // Init commands
        initCommands();

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Auth.class, new LoginListener(this));

        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));
    }

    private void initCommands() {
        manager = new SpongeCommandManager(container);

        prepareACF(manager, srLogger);

        runRepeat(cooldownStorage::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
    }

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
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
    public void runRepeat(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.getScheduler().createTaskBuilder().execute(runnable).interval(interval, timeUnit).delay(delay, timeUnit).submit(this);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return game.getServer().getOnlinePlayers().stream().map(WrapperSponge::wrapPlayer).collect(Collectors.toList());
    }

    private static class WrapperFactorySponge implements IWrapperFactory {
        @Override
        public String getPlayerName(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return player.getName();
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private static class PropertyFactorySponge implements IPropertyFactory {
        @Override
        public IProperty createProperty(String name, String value, String signature) {
            return new GenericProperty(name, value, signature);
        }
    }

    private class SkinsRestorerSpongeAPI extends SkinsRestorerAPIShared {
        public SkinsRestorerSpongeAPI() {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactorySponge(), new PropertyFactorySponge());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierSponge.applySkin(playerWrapper.get(Player.class), property);
        }

        @Override
        protected LocaleManager<ISRForeign> getLocaleManager() {
            return localeManager;
        }
    }
}
