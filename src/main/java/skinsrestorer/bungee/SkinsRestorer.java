package skinsrestorer.bungee;

import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.ConditionFailedException;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.bungee.commands.GUICommand;
import skinsrestorer.bungee.commands.SrCommand;
import skinsrestorer.bungee.commands.SkinCommand;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.PluginMessageListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.shared.update.UpdateCheckerGitHub;
import skinsrestorer.shared.utils.*;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin {
    @Getter
    private static SkinsRestorer instance;
    @Getter
    private boolean multiBungee;
    @Getter
    private boolean outdated;
    @Getter
    private String configPath = getDataFolder().getPath();

    private CommandSender console;
    private UpdateChecker updateChecker;

    @Getter
    private SkinApplier skinApplier;

    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;
    @Getter
    private SRLogger srLogger;
    @Getter
    private PluginMessageListener pluginMessageListener;
    @Getter
    private SkinCommand skinCommand;
    @Getter
    private SkinsRestorerBungeeAPI skinsRestorerBungeeAPI;

    public String getVersion() {
        return getDescription().getVersion();
    }


    @Override
    public void onEnable() {
        srLogger = new SRLogger(getDataFolder());

        int pluginId = 1686; // SkinsRestorer's ID on bStats, for Bungeecord
        Metrics metrics = new Metrics(this, pluginId);
        if (metrics.isEnabled()) {
            metrics.addCustomChart(new Metrics.SingleLineChart("mineskin_calls", MetricsCounter::collectMineskin_calls));
            metrics.addCustomChart(new Metrics.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
            metrics.addCustomChart(new Metrics.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
            metrics.addCustomChart(new Metrics.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));
        }

        console = getProxy().getConsole();

        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getDescription().getVersion(), this.srLogger, "SkinsRestorerUpdater/BungeeCord");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                this.getProxy().getScheduler().schedule(this, this::checkUpdate, 10, 10, TimeUnit.MINUTES);
        }

        instance = this;

        this.skinStorage = new SkinStorage();

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
        this.initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init SkinApplier
        this.skinApplier = new SkinApplier(this);
        this.skinApplier.init();

        // Init message channel
        this.getProxy().registerChannel("sr:messagechannel");
        this.pluginMessageListener = new PluginMessageListener(this);
        this.getProxy().getPluginManager().registerListener(this, this.pluginMessageListener);

        multiBungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        // Init API
        this.skinsRestorerBungeeAPI = new SkinsRestorerBungeeAPI(this, this.mojangAPI, this.skinStorage);

        // Run connection check
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(this.mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- "));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] §cPlugin currently can't fetch new skins."));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for wiki "));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] §cFor support, visit our discord at https://discord.me/servers/skinsrestorer "));
            console.sendMessage(TextComponent.fromLegacyText("§c[§4Critical§c] ------------------------------------------------------------------------------------------- "));
        }
    }

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

        this.skinCommand = new SkinCommand(this);
        manager.registerCommand(this.skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
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
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                    outdated = true;

                    updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(msg -> {
                        console.sendMessage(TextComponent.fromLegacyText(msg));
                    });
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    updateChecker.getUpToDateMessages(getVersion(), false).forEach(msg -> {
                        console.sendMessage(TextComponent.fromLegacyText(msg));
                    });
                }
            });
        });
    }
}
