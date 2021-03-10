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
package net.skinsrestorer.bungee;

import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.ConditionFailedException;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.SkinApplierBungee;
import net.skinsrestorer.shared.interfaces.SRApplier;
import net.skinsrestorer.shared.interfaces.SRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.*;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin implements SRPlugin {
    @Getter
    private static SkinsRestorer instance;
    @Getter
    private final String configPath = getDataFolder().getPath();
    @Getter
    private boolean multiBungee;
    @Getter
    private boolean outdated;
    private CommandSender console;
    private UpdateChecker updateChecker;
    @Getter
    private SkinApplierBungee skinApplierBungee;
    @Getter
    private SRLogger srLogger;
    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;
    @Getter
    private PluginMessageListener pluginMessageListener;
    @Getter
    private SkinCommand skinCommand;
    @Getter
    private SkinsRestorerAPI skinsRestorerBungeeAPI;

    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void onEnable() {
        srLogger = new SRLogger(getDataFolder());
        instance = this;
        console = getProxy().getConsole();
        File updaterDisabled = new File(this.configPath, "noupdate.txt");

        int pluginId = 1686; // SkinsRestorer's ID on bStats, for Bungeecord
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", MetricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", MetricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", MetricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("backup_calls", MetricsCounter::collectBackupCalls));

        if (!updaterDisabled.exists()) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getDescription().getVersion(), this.srLogger, "SkinsRestorerUpdater/BungeeCord");
            this.checkUpdate(true);

            this.getProxy().getScheduler().schedule(this, this::checkUpdate, 10, 10, TimeUnit.MINUTES);
        } else {
            srLogger.logAlways(Level.INFO, "Updater Disabled");
        }

        this.skinStorage = new SkinStorage(SkinStorage.Platform.BUNGEECORD);

        // Init config files
        Config.load(configPath, getResourceAsStream("config.yml"));
        Locale.load(configPath);

        this.mojangAPI = new MojangAPI(this.srLogger);
        this.mineSkinAPI = new MineSkinAPI(this.srLogger);

        this.skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!this.initStorage())
            return;

        this.mojangAPI.setSkinStorage(this.skinStorage);
        this.mineSkinAPI.setSkinStorage(this.skinStorage);

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this, this.srLogger));

        // Init commands
        initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init SkinApplier
        this.skinApplierBungee = new SkinApplierBungee(this);
        SkinApplierBungee.init();

        // Init message channel
        this.getProxy().registerChannel("sr:messagechannel");
        this.pluginMessageListener = new PluginMessageListener(this);
        this.getProxy().getPluginManager().registerListener(this, this.pluginMessageListener);

        multiBungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        // Init API
        this.skinsRestorerBungeeAPI = new SkinsRestorerAPI(this.mojangAPI, this.skinStorage, this);

        // Run connection check
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(this.mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- "));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] §cPlugin currently can't fetch new skins due to blocked connection!"));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for steps to resolve your issue!"));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] ------------------------------------------------------------------------------------------- "));
        }
    }

    @SuppressWarnings({"deprecation"})
    private void initCommands() {
        BungeeCommandManager manager = new BungeeCommandManager(this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            BungeeCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.command") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, configPath, getResourceAsStream("command-messages.properties"));

        SharedMethods.allowIllegalACFNames();

        this.skinCommand = new SkinCommand(this);
        manager.registerCommand(this.skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (Config.MYSQL_ENABLED) {
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
                console.sendMessage(TextComponent.fromLegacyText("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer."));
                getProxy().getPluginManager().unregisterListeners(this);
                getProxy().getPluginManager().unregisterCommands(this);
                return false;
            }
        } else {
            this.skinStorage.loadFolders(getDataFolder());
        }

        // Preload default skins
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), this.skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        this.checkUpdate(false);
    }

    private void checkUpdate(boolean showUpToDate) {
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(msg ->
                        console.sendMessage(TextComponent.fromLegacyText(msg)));
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), false).forEach(msg -> console.sendMessage(TextComponent.fromLegacyText(msg)));
            }
        }));
    }

    @Override
    public SRApplier getApplier() {
        return skinApplierBungee;
    }
}
