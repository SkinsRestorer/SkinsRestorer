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

import co.aikar.commands.CommandManager;
import co.aikar.commands.SpongeCommandManager;
import lombok.Getter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerServerShared;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
import net.skinsrestorer.sponge.commands.SkinCommand;
import net.skinsrestorer.sponge.commands.SrCommand;
import net.skinsrestorer.sponge.listeners.LoginListener;
import net.skinsrestorer.sponge.utils.WrapperSponge;
import org.bstats.charts.SingleLineChart;
import org.bstats.sponge.Metrics;
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
    private final Object pluginInstance; // Only for platform API use
    private final Metrics metrics;
    private final PluginContainer pluginContainer;
    protected Game game;

    public SkinsRestorerSponge(Object pluginInstance, Metrics.Factory metricsFactory, Path dataFolder, Logger log, PluginContainer container) {
        super(
                new Slf4jLoggerImpl(log),
                false,
                container.getVersion().orElse("Unknown"),
                "SkinsRestorerUpdater/Sponge",
                dataFolder
        );
        this.pluginInstance = pluginInstance;
        this.metrics = metricsFactory.make(2337);
        this.pluginContainer = container;
    }

    @Override
    protected void pluginStartup() {
        logger.load(dataFolder);

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
        loadConfig();
        loadLocales();

        WrapperSponge wrapper = injector.getSingleton(WrapperSponge.class);

        injector.provide(OnlinePlayersMethod.class,
                (CallableValue<Collection<ISRPlayer>>) () -> game.getServer().getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList()));

        initMineSkinAPI();

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        SkinApplierSponge skinApplierSponge = new SkinApplierSponge(this);

        // Init API
        registerAPI(new WrapperFactorySponge(), GenericProperty::new, skinApplierSponge);

        Sponge.getEventManager().registerListener(pluginInstance, ClientConnectionEvent.Auth.class, new LoginListener(skinStorage, settings, this, logger, skinApplierSponge));

        // Init commands
        CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands(locale);

        manager.registerCommand(injector.getSingleton(SkinCommand.class));
        manager.registerCommand(injector.newInstance(SrCommand.class));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, logger));
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
        game.getScheduler().createAsyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        Sponge.getScheduler().createSyncExecutor(pluginInstance).execute(runnable);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        game.getScheduler().createTaskBuilder().execute(runnable).interval(interval, timeUnit).delay(delay, timeUnit).submit(pluginInstance);
    }

    @Override
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        return new SpongeCommandManager(pluginContainer);
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
}
