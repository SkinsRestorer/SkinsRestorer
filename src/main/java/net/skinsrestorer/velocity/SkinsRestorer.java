/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.velocity;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.VelocityCommandIssuer;
import co.aikar.commands.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.data.PluginData;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.*;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.shared.utils.log.Slf4LoggerImpl;
import net.skinsrestorer.velocity.command.SkinCommand;
import net.skinsrestorer.velocity.command.SrCommand;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.utils.SkinApplierVelocity;
import org.bstats.charts.SingleLineChart;
import org.bstats.velocity.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Plugin(id = "skinsrestorer", name = PluginData.NAME, version = PluginData.VERSION, description = PluginData.DESCRIPTION, url = PluginData.URL, authors = {"Blackfire62", "McLive"})
public class SkinsRestorer implements ISRPlugin {
    @Getter
    private final ProxyServer proxy;
    @Getter
    private final SRLogger srLogger;
    @Getter
    private final File dataFolder;
    @Getter
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Metrics.Factory metricsFactory;
    private CommandSource console;
    private UpdateChecker updateChecker;
    @Getter
    private SkinApplierVelocity skinApplierVelocity;
    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;
    @Getter
    private SkinsRestorerAPI skinsRestorerAPI;

    @Inject
    public SkinsRestorer(ProxyServer proxy, Logger logger, Metrics.Factory metricsFactory, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        srLogger = new SRLogger(dataFolder.toFile(), new Slf4LoggerImpl(logger));
        this.dataFolder = dataFolder.toFile();
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        srLogger.log("Enabling SkinsRestorer v" + getVersion());
        console = proxy.getConsoleCommandSource();
        File updaterDisabled = new File(dataFolder, "noupdate.txt");

        Metrics metrics = metricsFactory.make(this, 10606);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", MetricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", MetricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", MetricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("backup_calls", MetricsCounter::collectBackupCalls));

        // Check for updates
        if (!updaterDisabled.exists()) {
            updateChecker = new UpdateCheckerGitHub(2124, getVersion(), getSrLogger(), "SkinsRestorerUpdater/Velocity");
            checkUpdate(true);

            getProxy().getScheduler().buildTask(this, this::checkUpdate).repeat(10, TimeUnit.MINUTES).delay(10, TimeUnit.MINUTES).schedule();
        } else {
            srLogger.log("Updater Disabled");
        }

        skinStorage = new SkinStorage(srLogger, SkinStorage.Platform.VELOCITY);

        // Init config files
        Config.load(dataFolder, getClass().getClassLoader().getResourceAsStream("config.yml"), srLogger);
        Locale.load(dataFolder, srLogger);

        mojangAPI = new MojangAPI(srLogger);
        mineSkinAPI = new MineSkinAPI(srLogger);

        skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!initStorage())
            return;

        mojangAPI.setSkinStorage(skinStorage);
        mineSkinAPI.setSkinStorage(skinStorage);

        // Init listener
        proxy.getEventManager().register(this, new GameProfileRequest(this));

        // Init commands
        initCommands();

        // Init SkinApplier
        skinApplierVelocity = new SkinApplierVelocity(this);

        // Init API
        skinsRestorerAPI = new SkinsRestorerVelocityAPI(mojangAPI, skinStorage);

        srLogger.log("Enabled SkinsRestorer v" + getVersion());

        // Run connection check
        SharedMethods.runServiceCheck(mojangAPI, srLogger);
    }

    @SuppressWarnings({"deprecation"})
    private void initCommands() {
        VelocityCommandManager manager = new VelocityCommandManager(getProxy(), this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            VelocityCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.command") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, dataFolder, getClass().getClassLoader().getResourceAsStream("command-messages.properties"), srLogger);

        SharedMethods.allowIllegalACFNames();

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolder)) return false;

        // Preload default skins
        getService().execute(skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        checkUpdate(false);
    }

    private void checkUpdate(boolean showUpToDate) {
        getService().execute(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false)
                        .forEach(msg -> console.sendMessage(deserialize(msg)));
            }

            @Override
            public void upToDate() {
                if (showUpToDate)
                    updateChecker.getUpToDateMessages(getVersion(), false)
                            .forEach(msg -> console.sendMessage(deserialize(msg)));
            }
        }));
    }

    public TextComponent deserialize(String string) {
        return LegacyComponentSerializer.legacySection().deserialize(string);
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = getProxy().getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getDescription().getVersion();

        return version.orElse("");
    }

    private class SkinsRestorerVelocityAPI extends SkinsRestorerAPI {
        public SkinsRestorerVelocityAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, skinStorage);
        }

        @Override
        public void applySkin(PlayerWrapper player, Object props) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applySkin(PlayerWrapper player) {
            try {
                skinApplierVelocity.applySkin(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
