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
package net.skinsrestorer.sponge;

import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;
import lombok.Getter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.data.PluginData;
import net.skinsrestorer.shared.interfaces.SRApplier;
import net.skinsrestorer.shared.interfaces.SRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.*;
import net.skinsrestorer.sponge.commands.SkinCommand;
import net.skinsrestorer.sponge.commands.SrCommand;
import net.skinsrestorer.sponge.listeners.LoginListener;
import net.skinsrestorer.sponge.utils.SkinApplierSponge;
import org.bstats.charts.SingleLineChart;
import org.bstats.sponge.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "skinsrestorer", name = PluginData.NAME, version = PluginData.VERSION, url = PluginData.URL, authors = {"Blackfire62", "McLive"})
public class SkinsRestorer implements SRPlugin {
    private static @Getter SkinsRestorer instance;
    private @Getter String configPath;
    private @Getter SkinApplierSponge skinApplierSponge;
    private @Getter SRLogger srLogger;

    private @Getter boolean bungeeEnabled = false;

    private @Getter SkinStorage skinStorage;

    private @Getter MojangAPI mojangAPI;
    private @Getter MineSkinAPI mineSkinAPI;
    private @Getter SkinsRestorerAPI skinsRestorerSpongeAPI;

    private UpdateChecker updateChecker;
    private CommandSource console;

    private final Metrics metrics;

    @Inject
    private Logger log;

    @Inject
    protected Game game;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path publicConfigDir;

    @Inject
    private PluginContainer container;

    // The metricsFactory parameter gets injected using @Inject
    @Inject
    public SkinsRestorer(Metrics.Factory metricsFactory) {
        metrics = metricsFactory.make(2337);
    }

    @Listener
    public void onInitialize(GameInitializationEvent e) {
        instance = this;
        console = Sponge.getServer().getConsole();
        configPath = Sponge.getGame().getConfigManager().getPluginConfig(this).getDirectory().toString();
        this.srLogger = new SRLogger(new File(configPath));

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getVersion(), this.srLogger, "SkinsRestorerUpdater/Sponge");
            this.checkUpdate(bungeeEnabled);

            if (Config.UPDATER_PERIODIC)
                Sponge.getScheduler().createTaskBuilder().execute(() -> {
                    this.checkUpdate(bungeeEnabled, false);
                }).interval(10, TimeUnit.MINUTES).delay(10, TimeUnit.MINUTES);
        }

        this.skinStorage = new SkinStorage(SkinStorage.Platform.SPONGE);

        // Init config files
        Config.load(configPath, getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load(configPath);

        this.mojangAPI = new MojangAPI(this.srLogger);
        this.mineSkinAPI = new MineSkinAPI(this.srLogger);

        this.skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!this.initStorage())
            return;

        this.mojangAPI.setSkinStorage(this.skinStorage);
        this.mineSkinAPI.setSkinStorage(this.skinStorage);

        // Init commands
        this.initCommands();

        // Init SkinApplier
        this.skinApplierSponge = new SkinApplierSponge(this);

        // Init API
        this.skinsRestorerSpongeAPI = new SkinsRestorerAPI(this.mojangAPI, this.skinStorage, this);

        // Run connection check
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(this.mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            System.out.println("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- ");
            System.out.println("§c[§4Critical§c] §cPlugin currently can't fetch new skins.");
            System.out.println("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for wiki ");
            System.out.println("§c[§4Critical§c] §cFor support, visit our discord at https://discord.me/servers/skinsrestorer ");
            System.out.println("§c[§4Critical§c] ------------------------------------------------------------------------------------------- ");
        }
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        if (!Sponge.getServer().getOnlineMode()) {
            Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Auth.class, new LoginListener(this));
        }

        metrics.addCustomChart(new SingleLineChart("mineskin_calls", MetricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", MetricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", MetricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("backup_calls", MetricsCounter::collectBackupCalls));
    }

    @SuppressWarnings({"deprecation"})
    private void initCommands() {
        Sponge.getPluginManager().getPlugin("skinsrestorer").ifPresent(pluginContainer -> {
            SpongeCommandManager manager = new SpongeCommandManager(pluginContainer);
            // optional: enable unstable api to use help
            manager.enableUnstableAPI("help");

            CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
            CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
            CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

            new CommandPropertiesManager(manager, configPath, getClass().getClassLoader().getResourceAsStream("command-messages.properties"));

            manager.registerCommand(new SkinCommand(this));
            manager.registerCommand(new SrCommand(this));
            //manager.registerCommand(new GUICommand());
        });
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (Config.USE_MYSQL) {
            try {
                MySQL mysql = new MySQL(
                        Config.MYSQL_HOST,
                        Config.MYSQL_PORT,
                        Config.MYSQL_DATABASE,
                        Config.MYSQL_USERNAME,
                        Config.MYSQL_PASSWORD,
                        Config.MYSQL_CONNECTIONOPTIONS
                );

                mysql.openConnection();
                mysql.createTable();

                this.skinStorage.setMysql(mysql);
            } catch (Exception e) {
                System.out.println("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
                return false;
            }
        } else {
            this.skinStorage.loadFolders(new File(configPath));
        }

        // Preload default skins
        Sponge.getScheduler().createAsyncExecutor(this).execute(this.skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate(boolean bungeeMode) {
        this.checkUpdate(bungeeMode, true);
    }

    private void checkUpdate(boolean bungeeMode, boolean showUpToDate) {
        Sponge.getScheduler().createAsyncExecutor(this).execute(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), bungeeMode).forEach(msg ->
                        console.sendMessage(parseMessage(msg)));
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), bungeeMode).forEach(msg -> console.sendMessage(parseMessage(msg)));
            }
        }));
    }

    public Text parseMessage(String msg) {
        return Text.builder(msg).build();
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = Sponge.getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getVersion();

        return version.orElse("");
    }

    @Override
    public SRApplier getApplier() {
        return skinApplierSponge;
    }
}
