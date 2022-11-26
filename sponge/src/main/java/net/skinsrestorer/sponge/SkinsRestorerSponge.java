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
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerServerShared;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.utils.SharedMethods;
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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerSponge extends SkinsRestorerServerShared {
    private static final boolean BUNGEE_ENABLED = false;
    private final Object pluginInstance; // Only for platform API use
    private final Metrics metrics;
    private final SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);
    private final SkinCommand skinCommand;
    private final PluginContainer pluginContainer;
    protected Game game;
    private SpongeCommandManager manager;

    public SkinsRestorerSponge(Object pluginInstance, Metrics.Factory metricsFactory, Path dataFolder, Logger log, PluginContainer container, Game game) {
        super(
                new Slf4jLoggerImpl(log),
                false,
                container.getVersion().orElse("Unknown"),
                "SkinsRestorerUpdater/Sponge",
                dataFolder
        );
        this.pluginInstance = pluginInstance;
        this.metrics = metricsFactory.make(2337);
        this.skinCommand = new SkinCommand(this);
        this.pluginContainer = container;
        new SkinsRestorerSpongeAPI(); // Register API
    }

    public void onInitialize() {
        logger.load(dataFolder);

        checkUpdateInit(() -> {
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeat(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
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

        // Init commands
        initCommands();

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, logger));
    }

    public void onServerStarted() {
        Sponge.getEventManager().registerListener(pluginInstance, ClientConnectionEvent.Auth.class, new LoginListener(this));

        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));
    }

    private void initCommands() {
        manager = new SpongeCommandManager(pluginContainer);

        prepareACF(manager, logger);

        runRepeat(cooldownStorage::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
    }

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version, SkinsRestorerSponge.BUNGEE_ENABLED).forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(version, SkinsRestorerSponge.BUNGEE_ENABLED).forEach(logger::info);
            }
        }));
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return Sponge.getPluginManager().isLoaded(pluginName);
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
    public void runSync(Runnable runnable) {
        Sponge.getScheduler().createSyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public void runRepeat(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.getScheduler().createTaskBuilder().execute(runnable).interval(interval, timeUnit).delay(delay, timeUnit).submit(pluginInstance);
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
            super(SkinsRestorerSponge.this, new WrapperFactorySponge(), new PropertyFactorySponge());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierSponge.applySkin(playerWrapper.get(Player.class), property);
        }
    }
}
