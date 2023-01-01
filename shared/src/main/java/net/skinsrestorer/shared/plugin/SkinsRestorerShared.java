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
package net.skinsrestorer.shared.plugin;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import co.aikar.commands.CommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.ISkinApplier;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.commands.SRCommand;
import net.skinsrestorer.shared.commands.SkinCommand;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.config.DatabaseConfig;
import net.skinsrestorer.shared.config.MineSkinConfig;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.*;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.storage.adapter.FileAdapter;
import net.skinsrestorer.shared.storage.adapter.MySQLAdapter;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.CommandPropertiesManager;
import net.skinsrestorer.shared.utils.CommandReplacements;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.MetricsBase;
import org.bstats.charts.SingleLineChart;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public abstract class SkinsRestorerShared implements ISRPlugin {
    protected final boolean unitTest = System.getProperty("sr.unit.test") != null;
    protected final SRLogger logger;
    protected final UpdateCheckerGitHub updateChecker;
    @Getter
    protected final Path dataFolder;
    @Getter
    protected final String version;
    protected final Injector injector;
    @Getter
    private final Platform platform;
    @Getter
    private final String updateCheckerAgent;
    private CommandManager<?, ?, ?, ?, ?, ?> manager;
    @Getter
    private boolean outdated = false;

    protected SkinsRestorerShared(ISRLogger isrLogger, boolean loggerColor,
                                  String version, String updateCheckerAgent, Path dataFolder,
                                  IWrapperFactory wrapperFactory, IPropertyFactory propertyFactory,
                                  Platform platform) {
        this.injector = new InjectorBuilder().addDefaultHandlers("net.skinsrestorer").create();

        injector.register(ISRPlugin.class, this);

        injector.register(MetricsCounter.class, new MetricsCounter());
        injector.register(CooldownStorage.class, new CooldownStorage());
        injector.register(SRLogger.class, (logger = new SRLogger(isrLogger, loggerColor)));

        injector.register(IWrapperFactory.class, wrapperFactory);
        injector.register(IPropertyFactory.class, propertyFactory);

        this.updateChecker = injector.getSingleton(UpdateCheckerGitHub.class);
        this.version = version;
        this.dataFolder = dataFolder;
        this.platform = platform;
        this.updateCheckerAgent = updateCheckerAgent;
    }

    protected CommandManager<?, ?, ?, ?, ?, ?> sharedInitCommands() {
        this.manager = createCommandManager();
        injector.register(CommandManager.class, manager);

        registerConditions();

        prepareACF();

        runRepeatAsync(injector.getSingleton(CooldownStorage.class)::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(injector.newInstance(SkinCommand.class));
        manager.registerCommand(injector.newInstance(SRCommand.class));

        return manager;
    }

    protected abstract CommandManager<?, ?, ?, ?, ?, ?> createCommandManager();

    private void registerConditions() {
        SettingsManager settings = injector.getSingleton(SettingsManager.class);
        CooldownStorage cooldownStorage = injector.getSingleton(CooldownStorage.class);

        manager.getCommandConditions().addCondition("allowed-server", context -> {
            ISRCommandSender sender = convertCommandSender(context.getIssuer().getIssuer());
            if (!(sender instanceof ISRProxyPlayer)) {
                return;
            }

            if (!settings.getProperty(Config.NOT_ALLOWED_COMMAND_SERVERS_ENABLED)) {
                return;
            }

            Optional<String> optional = ((ISRProxyPlayer) sender).getCurrentServer();
            if (!optional.isPresent()) {
                if (!settings.getProperty(Config.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND)) {
                    throw new ConditionFailedException("You are not on a server!");
                }
                return;
            }

            String server = optional.get();
            boolean inList = settings.getProperty(Config.NOT_ALLOWED_COMMAND_SERVERS).contains(server);
            boolean shouldBlock = settings.getProperty(Config.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST) != inList;

            if (shouldBlock) {
                throw new ConditionFailedException("You are not allowed to use this command on this server!");
            }
        });

        manager.getCommandConditions().addCondition("cooldown", context -> {
            ISRCommandSender sender = convertCommandSender(context.getIssuer().getIssuer());
            if (sender instanceof ISRPlayer) {
                UUID senderUUID = ((ISRPlayer) sender).getUniqueId();
                if (!sender.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(senderUUID)) {
                    sender.sendMessage(Message.SKIN_COOLDOWN, cooldownStorage.getCooldownSeconds(senderUUID));
                    throw new ConditionFailedException();
                }
            }
        });

        manager.getCommandConditions().addCondition("console-only", context -> {
            if (context.getIssuer().isPlayer()) {
                ISRCommandSender sender = convertCommandSender(context.getIssuer().getIssuer());
                sender.sendMessage(Message.ONLY_ALLOWED_ON_CONSOLE);
                throw new ConditionFailedException();
            }
        });
    }

    protected abstract ISRCommandSender convertCommandSender(Object sender);

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version)
                        .forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages().forEach(logger::info);
            }
        }));
    }

    public void loadConfig() {
        SettingsManager settings = injector.getIfAvailable(SettingsManager.class);
        if (settings == null) {
            settings = SettingsManagerBuilder
                    .withYamlFile(dataFolder.resolve("config.yml"))
                    .configurationData(Config.class, DatabaseConfig.class, StorageConfig.class, MineSkinConfig.class)
                    .useDefaultMigrationService()
                    .create();
            injector.register(SettingsManager.class, settings);
        } else {
            settings.reload();
        }

        //__Default__Skins
        if (settings.getProperty(StorageConfig.DEFAULT_SKINS_ENABLED) && settings.getProperty(StorageConfig.DEFAULT_SKINS).isEmpty()) {
            logger.warning("[Config] no DefaultSkins found! Disabling DefaultSkins.");
            settings.setProperty(StorageConfig.DEFAULT_SKINS_ENABLED, false);
        }

        //__Disabled__Skins
        if (settings.getProperty(Config.DISABLED_SKINS_ENABLED) && settings.getProperty(Config.DISABLED_SKINS).isEmpty()) {
            logger.warning("[Config] no DisabledSkins found! Disabling DisabledSkins.");
            settings.setProperty(Config.DISABLED_SKINS_ENABLED, false);
        }

        if (settings.getProperty(Config.RESTRICT_SKIN_URLS_ENABLED) && settings.getProperty(Config.RESTRICT_SKIN_URLS_LIST).isEmpty()) {
            logger.warning("[Config] no RestrictSkinUrls found! Disabling RestrictSkinUrls.");
            settings.setProperty(Config.RESTRICT_SKIN_URLS_ENABLED, false);
        }

        if (!settings.getProperty(StorageConfig.CUSTOM_GUI_ENABLED))
            settings.setProperty(StorageConfig.CUSTOM_GUI_ONLY, false);

        if (!settings.getProperty(Config.DISMOUNT_PLAYER_ON_UPDATE)) {
            settings.setProperty(Config.REMOUNT_PLAYER_ON_UPDATE, false);
        }

        if (settings.getProperty(MineSkinConfig.MINESKIN_API_KEY).equals("key")) {
            settings.setProperty(MineSkinConfig.MINESKIN_API_KEY, "");
        }

        logger.setDebug(settings.getProperty(Config.DEBUG));
        SkinsRestorerLocale locale = injector.getIfAvailable(SkinsRestorerLocale.class);
        if (locale != null) {
            locale.setDefaultLocale(settings.getProperty(Config.LANGUAGE));
        }
    }

    public void loadLocales() {
        LocaleManager<ISRForeign> localeManager = LocaleManager.create(ISRForeign::getLocale, Locale.ENGLISH);
        injector.register(LocaleManager.class, localeManager);
        Message.load(localeManager, dataFolder, this);
        injector.getSingleton(SkinsRestorerLocale.class);
    }

    protected void initStorage() throws InitializeException {
        // Initialise SkinStorage
        SkinStorage skinStorage = injector.getSingleton(SkinStorage.class);
        SettingsManager settings = injector.getSingleton(SettingsManager.class);
        try {
            if (settings.getProperty(DatabaseConfig.MYSQL_ENABLED)) {
                MySQL mysql = new MySQL(
                        logger,
                        settings.getProperty(DatabaseConfig.MYSQL_HOST),
                        settings.getProperty(DatabaseConfig.MYSQL_PORT),
                        settings.getProperty(DatabaseConfig.MYSQL_DATABASE),
                        settings.getProperty(DatabaseConfig.MYSQL_USERNAME),
                        settings.getProperty(DatabaseConfig.MYSQL_PASSWORD),
                        settings.getProperty(DatabaseConfig.MYSQL_MAX_POOL_SIZE),
                        settings.getProperty(DatabaseConfig.MYSQL_CONNECTION_OPTIONS)
                );

                mysql.connectPool();
                mysql.createTable(settings);

                logger.info("Connected to MySQL!");
                skinStorage.setStorageAdapter(new MySQLAdapter(mysql, settings));
            } else {
                skinStorage.setStorageAdapter(new FileAdapter(dataFolder, settings));
            }

            // Preload default skins
            runAsync(skinStorage::preloadDefaultSkins);
        } catch (SQLException e) {
            logger.severe("§cCan't connect to MySQL! Disabling SkinsRestorer.", e);
            throw new InitializeException(e);
        } catch (IOException e) {
            logger.severe("§cCan't create data folders! Disabling SkinsRestorer.", e);
            throw new InitializeException(e);
        }
    }

    @SuppressWarnings({"deprecation"})
    public void prepareACF() {
        SettingsManager settings = injector.getSingleton(SettingsManager.class);
        SkinsRestorerLocale locale = injector.getSingleton(SkinsRestorerLocale.class);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");
        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call(settings)));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, locale.getMessage(locale.getDefaultForeign(), v)));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, locale.getMessage(locale.getDefaultForeign(), v)));
        CommandReplacements.completions.forEach((k, v) -> manager.getCommandCompletions().registerAsyncCompletion(k, c ->
                Arrays.asList(locale.getMessage(locale.getDefaultForeign(), v).split(", "))));

        CommandPropertiesManager.load(manager, dataFolder, getResource("command.properties"), logger);

        SharedMethods.allowIllegalACFNames();
    }

    public void checkUpdateInit(Runnable check) {
        Path updaterDisabled = dataFolder.resolve("noupdate.txt");
        if (Files.exists(updaterDisabled)) {
            logger.info("Updater Disabled");
        } else {
            check.run();
        }
    }

    protected void setOutdated() {
        outdated = true;
    }

    protected void initMineSkinAPI() {
        injector.getSingleton(MineSkinAPI.class);
    }

    protected void registerAPI(ISkinApplier skinApplier) {
        injector.register(SkinsRestorerAPI.class, new SkinsRestorerAPI(
                injector.getSingleton(MojangAPI.class),
                injector.getSingleton(MineSkinAPI.class),
                injector.getSingleton(SkinStorage.class),
                injector.getSingleton(IWrapperFactory.class),
                injector.getSingleton(IPropertyFactory.class), skinApplier));
    }

    protected void registerMetrics(Object metricsParent) {
        MetricsCounter metricsCounter = injector.getSingleton(MetricsCounter.class);
        try {
            Field field = metricsParent.getClass().getDeclaredField("metricsBase");
            field.setAccessible(true);
            MetricsBase metrics = (MetricsBase) field.get(metricsParent);

            metrics.addCustomChart(new SingleLineChart("mineskin_calls", () -> metricsCounter.collect(MetricsCounter.Service.MINE_SKIN)));
            metrics.addCustomChart(new SingleLineChart("minetools_calls", () -> metricsCounter.collect(MetricsCounter.Service.MINE_TOOLS)));
            metrics.addCustomChart(new SingleLineChart("mojang_calls", () -> metricsCounter.collect(MetricsCounter.Service.MOJANG)));
            metrics.addCustomChart(new SingleLineChart("ashcon_calls", () -> metricsCounter.collect(MetricsCounter.Service.ASHCON)));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected abstract void pluginStartup() throws InitializeException;

    protected abstract Object createMetricsInstance();

    protected void startupStart() {
        logger.load(dataFolder);

        if (!unitTest) {
            registerMetrics(createMetricsInstance());
        }
    }

    protected void updateCheck() {
        checkUpdateInit(() -> {
            checkUpdate(true);

            int delayInt = 60 + ThreadLocalRandom.current().nextInt(240 - 60 + 1);
            runRepeatAsync(this::checkUpdate, delayInt, delayInt, TimeUnit.MINUTES);
        });
    }
}
