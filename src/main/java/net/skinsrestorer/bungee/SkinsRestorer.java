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
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.bungee.commands.GUICommand;
import net.skinsrestorer.bungee.commands.SkinCommand;
import net.skinsrestorer.bungee.commands.SrCommand;
import net.skinsrestorer.bungee.listeners.LoginListener;
import net.skinsrestorer.bungee.listeners.PluginMessageListener;
import net.skinsrestorer.bungee.utils.SkinApplierBungee;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.*;
import net.skinsrestorer.shared.utils.log.LoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.File;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin implements ISRPlugin {
    @Getter
    private final File configPath = getDataFolder();
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
        srLogger = new SRLogger(getDataFolder(), new LoggerImpl(getProxy().getLogger()), true);
        console = getProxy().getConsole();
        File updaterDisabled = new File(getDataFolder(), "noupdate.txt");

        int pluginId = 1686; // SkinsRestorer's ID on bStats, for Bungeecord
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", MetricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", MetricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", MetricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("backup_calls", MetricsCounter::collectBackupCalls));

        if (!updaterDisabled.exists()) {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/BungeeCord");
            checkUpdate(true);

            getProxy().getScheduler().schedule(this, this::checkUpdate, 10, 10, TimeUnit.MINUTES);
        } else {
            srLogger.log("Updater Disabled");
        }

        skinStorage = new SkinStorage(SkinStorage.Platform.BUNGEECORD);

        // Init config files
        Config.load(getDataFolder(), getResourceAsStream("config.yml"));
        Locale.load(getDataFolder());

        mojangAPI = new MojangAPI(srLogger);
        mineSkinAPI = new MineSkinAPI(srLogger);

        skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!initStorage())
            return;

        mojangAPI.setSkinStorage(skinStorage);
        mineSkinAPI.setSkinStorage(skinStorage);

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this, srLogger));

        // Init commands
        initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init SkinApplier
        skinApplierBungee = new SkinApplierBungee(this);
        SkinApplierBungee.init();

        // Init message channel
        getProxy().registerChannel("sr:messagechannel");
        pluginMessageListener = new PluginMessageListener(this);
        getProxy().getPluginManager().registerListener(this, pluginMessageListener);

        multiBungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        // Init API
        skinsRestorerBungeeAPI = new SkinsRestorerBungeeAPI(mojangAPI, skinStorage);

        // Run connection check
        SharedMethods.runServiceCheck(mojangAPI, srLogger);
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
                srLogger.log("§cCan't connect to MySQL! Disabling SkinsRestorer.");
                getProxy().getPluginManager().unregisterListeners(this);
                getProxy().getPluginManager().unregisterCommands(this);
                return false;
            }
        } else {
            this.skinStorage.loadFolders(getDataFolder());
        }

        // Preload default skins
        ProxyServer.getInstance().getScheduler().runAsync(this, this.skinStorage::preloadDefaultSkins);
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

    private class SkinsRestorerBungeeAPI extends SkinsRestorerAPI {
        public SkinsRestorerBungeeAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, skinStorage);
        }

        @Override
        public void applySkin(PlayerWrapper player, Object props) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applySkin(PlayerWrapper player) {
            try {
                skinApplierBungee.applySkin(player, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
