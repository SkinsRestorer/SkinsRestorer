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
package net.skinsrestorer.shared.platform;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.SettingsManagerBuilder;
import ch.jalu.injector.Injector;
import co.aikar.commands.CommandManager;
import co.aikar.commands.ConditionFailedException;
import co.aikar.locales.LocaleManager;
import lombok.Getter;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.acf.CommandPropertiesManager;
import net.skinsrestorer.shared.acf.CommandReplacements;
import net.skinsrestorer.shared.api.NameGetter;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.api.SharedSkinsRestorer;
import net.skinsrestorer.shared.api.SkinApplierAccess;
import net.skinsrestorer.shared.api.event.EventBusImpl;
import net.skinsrestorer.shared.commands.ProxyGUICommand;
import net.skinsrestorer.shared.commands.SRCommand;
import net.skinsrestorer.shared.commands.ServerGUICommand;
import net.skinsrestorer.shared.commands.SkinCommand;
import net.skinsrestorer.shared.config.*;
import net.skinsrestorer.shared.connections.MineSkinAPIImpl;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.*;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.MySQL;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.adapter.FileAdapter;
import net.skinsrestorer.shared.storage.adapter.MySQLAdapter;
import net.skinsrestorer.shared.update.UpdateCheck;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.MetricsBase;
import org.bstats.charts.SingleLineChart;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SRPlugin {
    private static final String USER_AGENT = "SkinsRestorer/%s (%s)";
    @Getter
    private static final boolean unitTest = System.getProperty("sr.unit.test") != null;
    private final SRPlatformAdapter adapter;
    private final SRLogger logger;
    private final UpdateCheck updateCheck;
    @Getter
    private final Path dataFolder;
    @Getter
    private final String version;
    @Getter
    private final Injector injector;
    @Getter
    private final Platform platform;
    private CommandManager<?, ?, ?, ?, ?, ?> manager;
    @Getter
    private boolean outdated = false;
    @Getter
    private boolean updaterInitialized = false;

    public SRPlugin(Injector injector, String version, Path dataFolder, Platform platform, Class<? extends UpdateCheck> updateCheck) {
        injector.register(SRPlugin.class, this);
        injector.register(MetricsCounter.class, new MetricsCounter());
        injector.register(CooldownStorage.class, new CooldownStorage());

        this.injector = injector;
        this.adapter = injector.getSingleton(SRPlatformAdapter.class);
        this.logger = injector.getSingleton(SRLogger.class);
        this.updateCheck = injector.getSingleton(updateCheck);
        this.version = version;
        this.dataFolder = dataFolder;
        this.platform = platform;
    }

    public void initCommands() {
        this.manager = adapter.createCommandManager();
        injector.register(CommandManager.class, manager);

        registerConditions();

        prepareACF();

        adapter.runRepeatAsync(injector.getSingleton(CooldownStorage.class)::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(injector.newInstance(SkinCommand.class));
        manager.registerCommand(injector.newInstance(SRCommand.class));

        if (injector.getIfAvailable(SRServerPlugin.class) != null) {
            manager.registerCommand(injector.newInstance(ServerGUICommand.class));
        } else if (injector.getIfAvailable(SRProxyPlugin.class) != null) {
            manager.registerCommand(injector.newInstance(ProxyGUICommand.class));
        } else {
            throw new IllegalStateException("Unknown platform");
        }

    }

    public void registerConditions() {
        SettingsManager settings = injector.getSingleton(SettingsManager.class);
        CooldownStorage cooldownStorage = injector.getSingleton(CooldownStorage.class);

        manager.getCommandConditions().addCondition("allowed-server", context -> {
            SRCommandSender sender = adapter.convertCommandSender(context.getIssuer().getIssuer());
            if (!(sender instanceof SRProxyPlayer)) {
                return;
            }

            if (!settings.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ENABLED)) {
                return;
            }

            Optional<String> optional = ((SRProxyPlayer) sender).getCurrentServer();
            if (!optional.isPresent()) {
                if (!settings.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND)) {
                    throw new ConditionFailedException("You are not on a server!");
                }
                return;
            }

            String server = optional.get();
            boolean inList = settings.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS).contains(server);
            boolean shouldBlock = settings.getProperty(ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST) != inList;

            if (shouldBlock) {
                throw new ConditionFailedException("You are not allowed to use this command on this server!");
            }
        });

        manager.getCommandConditions().addCondition("cooldown", context -> {
            SRCommandSender sender = adapter.convertCommandSender(context.getIssuer().getIssuer());
            if (sender instanceof SRPlayer) {
                UUID senderUUID = ((SRPlayer) sender).getUniqueId();
                if (!sender.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(senderUUID)) {
                    sender.sendMessage(Message.SKIN_COOLDOWN, cooldownStorage.getCooldownSeconds(senderUUID));
                    throw new ConditionFailedException();
                }
            }
        });

        manager.getCommandConditions().addCondition("console-only", context -> {
            if (context.getIssuer().isPlayer()) {
                SRCommandSender sender = adapter.convertCommandSender(context.getIssuer().getIssuer());
                sender.sendMessage(Message.ONLY_ALLOWED_ON_CONSOLE);
                throw new ConditionFailedException();
            }
        });
    }

    public void loadConfig() {
        SettingsManager settings = injector.getIfAvailable(SettingsManager.class);
        if (settings == null) {
            settings = SettingsManagerBuilder
                    .withYamlFile(dataFolder.resolve("config.yml"))
                    .configurationData(
                            CommentsConfig.class,
                            MessageConfig.class,
                            DatabaseConfig.class,
                            CommandConfig.class,
                            GUIConfig.class,
                            StorageConfig.class,
                            ProxyConfig.class,
                            ServerConfig.class,
                            LoginConfig.class,
                            APIConfig.class,
                            AdvancedConfig.class,
                            DevConfig.class
                    )
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
        if (settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED) && settings.getProperty(CommandConfig.DISABLED_SKINS).isEmpty()) {
            logger.warning("[Config] no DisabledSkins found! Disabling DisabledSkins.");
            settings.setProperty(CommandConfig.DISABLED_SKINS_ENABLED, false);
        }

        if (settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED) && settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_LIST).isEmpty()) {
            logger.warning("[Config] no RestrictSkinUrls found! Disabling RestrictSkinUrls.");
            settings.setProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED, false);
        }

        if (!settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED))
            settings.setProperty(GUIConfig.CUSTOM_GUI_ONLY, false);

        if (!settings.getProperty(ServerConfig.DISMOUNT_PLAYER_ON_UPDATE)) {
            settings.setProperty(ServerConfig.REMOUNT_PLAYER_ON_UPDATE, false);
        }

        if (settings.getProperty(APIConfig.MINESKIN_API_KEY).equals("key")) {
            settings.setProperty(APIConfig.MINESKIN_API_KEY, "");
        }

        logger.setDebug(settings.getProperty(DevConfig.DEBUG));
        SkinsRestorerLocale locale = injector.getIfAvailable(SkinsRestorerLocale.class);
        if (locale != null) {
            locale.setDefaultLocale(settings.getProperty(MessageConfig.LANGUAGE));
        }
    }

    public void loadLocales() {
        LocaleManager<SRForeign> localeManager = LocaleManager.create(SRForeign::getLocale, Locale.ENGLISH);
        try {
            Message.load(localeManager, dataFolder, adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        injector.register(LocaleManager.class, localeManager);
        injector.getSingleton(SkinsRestorerLocale.class);
    }

    public void initStorage() throws InitializeException {
        // Initialise SkinStorage
        SkinStorageImpl skinStorage = injector.getSingleton(SkinStorageImpl.class);
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
            adapter.runAsync(skinStorage::preloadDefaultSkins);
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
        manager.allowInvalidName(true);

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v.call(settings)));
        CommandReplacements.descriptions.forEach((key, value) -> manager.getCommandReplacements().addReplacement(key, locale.getMessage(locale.getDefaultForeign(), value)));
        CommandReplacements.syntax.forEach((key, value) -> manager.getCommandReplacements().addReplacement(key, locale.getMessage(locale.getDefaultForeign(), value)));
        CommandReplacements.completions.forEach((k, v) -> manager.getCommandCompletions().registerAsyncCompletion(k, c ->
                Arrays.asList(locale.getMessage(locale.getDefaultForeign(), v).split(", "))));

        CommandPropertiesManager.load(manager, dataFolder, adapter, logger);
    }

    public void initUpdateCheck() {
        if (updaterInitialized) {
            return;
        }

        updaterInitialized = true;
        Path updaterDisabled = dataFolder.resolve("noupdate.txt");
        if (Files.exists(updaterDisabled)) {
            logger.info("Updater Disabled");
        } else {
            updateCheck.run();
        }
    }

    public void setOutdated() {
        outdated = true;
    }

    private void initMineSkinAPI() {
        MineSkinAPI mineSkinAPI = injector.getSingleton(MineSkinAPIImpl.class);
        injector.register(MineSkinAPI.class, mineSkinAPI);
    }

    public void registerAPI() {
        SkinsRestorer api = new SharedSkinsRestorer(injector.getSingleton(SkinStorageImpl.class),
                injector.getSingleton(MojangAPIImpl.class),
                injector.getSingleton(MineSkinAPIImpl.class),
                injector.getSingleton(SharedSkinApplier.class),
                injector.getSingleton(EventBusImpl.class));
        SkinsRestorerProvider.setApi(api);
        injector.register(SkinsRestorer.class, api);
    }

    public <P> void registerSkinApplier(SkinApplierAccess<P> skinApplier, Class<P> playerClass, NameGetter<P> nameGetter) {
        SharedSkinApplier<P> sharedSkinApplier = new SharedSkinApplier<>(playerClass, skinApplier, nameGetter,
                injector.getSingleton(SkinStorageImpl.class));
        injector.register(SharedSkinApplier.class, sharedSkinApplier);
        injector.register(SkinApplier.class, sharedSkinApplier);
    }

    public void registerMetrics(Object metricsParent) {
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

    public void startup(SRPlatformInit platformInit) throws InitializeException {
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        SRProxyPlugin proxyPlugin = injector.getIfAvailable(SRProxyPlugin.class);

        logger.load(dataFolder);

        if (!unitTest) {
            registerMetrics(adapter.createMetricsInstance());
        }

        EventBusImpl sharedEventBus = new EventBusImpl();
        injector.register(EventBusImpl.class, sharedEventBus);


        if (serverPlugin != null) {
            // Check if we are running in proxy mode
            serverPlugin.checkProxyMode();
        }

        // Init config files
        loadConfig();
        loadLocales();

        // Load MineSkinAPI with config values
        initMineSkinAPI();

        platformInit.initSkinApplier();

        platformInit.checkPluginSupport();

        if (serverPlugin != null) {
            serverPlugin.startupPlatform((SRServerPlatformInit) platformInit);
        } else if (proxyPlugin != null) {
            proxyPlugin.startupPlatform((SRProxyPlatformInit) platformInit);
        } else {
            throw new IllegalStateException("No platform class available!");
        }

        initUpdateCheck();

        if (serverPlugin == null || !serverPlugin.isProxyMode()) {
            adapter.runAsync(this::runServiceCheck);
        }
    }

    private void runServiceCheck() {
        ServiceCheckerService.ServiceCheckResponse response = injector.getSingleton(ServiceCheckerService.class).checkServices();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            logger.info("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§r§c] -------------------------");
            logger.info("§c[§4Critical§c] §cPlugin currently can't fetch new skins due to blocked connection!");
            logger.info("§c[§4Critical§c] §cSee https://skinsrestorer.net/firewall for steps to resolve your issue!");
            logger.info("§c[§4Critical§c] ----------------------------------------------------------------------");
        }
    }

    public String getUserAgent() {
        return String.format(USER_AGENT, version, platform);
    }
}
