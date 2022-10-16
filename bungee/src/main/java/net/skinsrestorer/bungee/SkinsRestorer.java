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
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SkullCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.ConnectListener;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.BungeeConsoleImpl;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.*;
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
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin implements ISRProxyPlugin {
    private static final String NEW_PROPERTY_CLASS = "net.md_5.bungee.protocol.Property";
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final CooldownStorage cooldownStorage = new CooldownStorage();
    private final BungeeConsoleImpl bungeeConsole = new BungeeConsoleImpl(getProxy() == null ? null : getProxy().getConsole());
    private final JavaLoggerImpl javaLogger = new JavaLoggerImpl(bungeeConsole, getProxy() == null ? null : getProxy().getLogger());
    private final SRLogger srLogger = new SRLogger(javaLogger, true);
    private final MojangAPI mojangAPI = new MojangAPI(srLogger, metricsCounter);
    private final MineSkinAPI mineSkinAPI = new MineSkinAPI(srLogger, metricsCounter);
    private final SkinStorage skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
    private final SkinApplierBungeeShared skinApplierBungee = selectSkinApplier(this, srLogger);
    private final SkinsRestorerAPI skinsRestorerAPI = new SkinsRestorerBungeeAPI();
    private final SkinCommand skinCommand = new SkinCommand(this);
    private LocaleManager<ISRForeign> localeManager;
    private Path dataFolderPath;
    private boolean outdated;
    private UpdateChecker updateChecker;
    private BungeeCommandManager manager;

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
        bungeeConsole.setCommandSender(getProxy().getConsole());
        javaLogger.setLogger(getProxy().getLogger());
        dataFolderPath = getDataFolder().toPath();
        srLogger.load(dataFolderPath);

        Metrics metrics = new Metrics(this, 1686);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        checkUpdateInit(() -> {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/BungeeCord");
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeat(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        });

        // Init config files
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        localeManager = LocaleManager.create(ISRForeign::getLocale, SkinsRestorerAPIShared.getApi().getDefaultForeign().getLocale());
        Message.load(localeManager, dataFolderPath, this);

        // Init storage
        try {
            initStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
            return;
        }

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this));
        getProxy().getPluginManager().registerListener(this, new ConnectListener(this));

        // Init commands
        initCommands();

        // Init message channel
        getProxy().registerChannel("sr:skinchange");
        getProxy().registerChannel("sr:messagechannel");
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

        // Run connection check
        runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
    }

    private void initCommands() {
        manager = new BungeeCommandManager(this);

        prepareACF(manager, srLogger);

        runRepeat(cooldownStorage::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
        manager.registerCommand(new SkullCommand(this));
    }

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false)
                        .forEach(srLogger::info);
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
    public void runRepeat(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        getProxy().getScheduler().schedule(this, runnable, delay, interval, timeUnit);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return getProxy().getPlayers().stream().map(WrapperBungee::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public Optional<ISRProxyPlayer> getPlayer(String playerName) {
        return Optional.ofNullable(getProxy().getPlayer(playerName)).map(WrapperBungee::wrapPlayer);
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
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactoryBungee(), new PropertyFactoryBungee());
        }

        @SneakyThrows
        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierBungee.applySkin(playerWrapper.get(ProxiedPlayer.class), property);
        }

        @Override
        protected LocaleManager<ISRForeign> getLocaleManager() {
            return localeManager;
        }
    }
}
