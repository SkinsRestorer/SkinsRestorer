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
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
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
import net.skinsrestorer.velocity.command.SkinCommand;
import net.skinsrestorer.velocity.command.SrCommand;
import net.skinsrestorer.velocity.listener.GameProfileRequest;
import net.skinsrestorer.velocity.utils.SkinApplierVelocity;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "skinsrestorer", name = PluginData.NAME, version = PluginData.VERSION, description = PluginData.DESCRIPTION, url = PluginData.URL, authors = {"Blackfire62", "McLive"})
public class SkinsRestorer implements SRPlugin {
    private final @Getter ProxyServer proxy;
    private final @Getter SRLogger logger;
    private final @Getter Path dataFolder;

    private @Getter SkinApplierVelocity skinApplierVelocity;
    private final @Getter ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final @Getter String CONFIG_PATH = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

    private CommandSource console;
    private UpdateChecker updateChecker;

    private @Getter SkinStorage skinStorage;
    private @Getter MojangAPI mojangAPI;
    private @Getter MineSkinAPI mineSkinAPI;
    private @Getter SkinsRestorerAPI skinsRestorerVelocityAPI;

    @Inject
    public SkinsRestorer(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = new SRLogger(dataFolder.toFile());
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        logger.logAlways("Enabling SkinsRestorer v" + getVersion());
        console = proxy.getConsoleCommandSource();

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getVersion(), this.getLogger(), "SkinsRestorerUpdater/Velocity");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                this.getProxy().getScheduler().buildTask(this, this::checkUpdate).repeat(10, TimeUnit.MINUTES).delay(10, TimeUnit.MINUTES).schedule();
        }

        this.skinStorage = new SkinStorage(SkinStorage.Platform.VELOCITY);

        // Init config files
        Config.load(CONFIG_PATH, getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load(CONFIG_PATH);

        this.mojangAPI = new MojangAPI(this.logger);
        this.mineSkinAPI = new MineSkinAPI(this.logger);

        this.skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!this.initStorage())
            return;

        this.mojangAPI.setSkinStorage(this.skinStorage);
        this.mineSkinAPI.setSkinStorage(this.skinStorage);

        // Init listener
        proxy.getEventManager().register(this, new GameProfileRequest(this));

        // Init commands
        this.initCommands();

        // Init SkinApplier
        this.skinApplierVelocity = new SkinApplierVelocity(this);

        // Init API
        this.skinsRestorerVelocityAPI = new SkinsRestorerAPI(this.mojangAPI, this.skinStorage, this);

        logger.logAlways("Enabled SkinsRestorer v" + getVersion());

        // Run connection check
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(this.mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            console.sendMessage(deserialize("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- "));
            console.sendMessage(deserialize("§c[§4Critical§c] §cPlugin currently can't fetch new skins."));
            console.sendMessage(deserialize("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for wiki "));
            console.sendMessage(deserialize("§c[§4Critical§c] §cFor support, visit our discord at https://discord.me/servers/skinsrestorer "));
            console.sendMessage(deserialize("§c[§4Critical§c] ------------------------------------------------------------------------------------------- "));
        }
    }

    @Subscribe
    public void onShutDown(ProxyShutdownEvent ev) {
        this.logger.logAlways("Disabling SkinsRestorer v" + getVersion());
        this.logger.logAlways("Disabled SkinsRestorer v" + getVersion());
    }

    @SuppressWarnings({"deprecation"})
    private void initCommands() {
        VelocityCommandManager manager = new VelocityCommandManager(this.getProxy(), this);
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

        new CommandPropertiesManager(manager, CONFIG_PATH, getClass().getClassLoader().getResourceAsStream("command-messages.properties"));

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
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
                logger.logAlways("§cCan't connect to MySQL! Disabling SkinsRestorer.");
                return false;
            }
        } else {
            this.skinStorage.loadFolders(this.getDataFolder().toFile());
        }

        // Preload default skins
        this.getService().execute(this.skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        this.checkUpdate(false);
    }

    private void checkUpdate(boolean showUpToDate) {
        getService().execute(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(msg ->
                        console.sendMessage(deserialize(msg)));
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), false).forEach(msg ->
                        console.sendMessage(deserialize(msg)));
            }
        }));
    }

    public TextComponent deserialize(String string) {
        return LegacyComponentSerializer.legacy().deserialize(string);
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = getProxy().getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getDescription().getVersion();

        return version.orElse("");
    }

    @Override
    public SRApplier getApplier() {
        return skinApplierVelocity;
    }
}
