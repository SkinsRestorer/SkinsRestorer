package skinsrestorer.bungee;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.bstats.bungeecord.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;

import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.ConditionFailedException;
import lombok.Getter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.SkinCommand;
import skinsrestorer.bungee.commands.SrCommand;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.shared.update.UpdateCheckerGitHub;
import skinsrestorer.shared.utils.CommandPropertiesManager;
import skinsrestorer.shared.utils.CommandReplacements;
import skinsrestorer.shared.utils.MetricsCounter;
import skinsrestorer.shared.utils.MineSkinAPI;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.SRLogger;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends Plugin {
    @Getter
    private static SkinsRestorer instance;
    @Getter
    private boolean multiBungee;
    @Getter
    private boolean outdated;
    @Getter
    private String configPath = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

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

    public String getVersion() {
        return getDescription().getVersion();
    }


    @Override
    public void onEnable() {
        srLogger = new SRLogger();
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SingleLineChart("mineskin_calls", MetricsCounter::collectMineskin_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));

        console = getProxy().getConsole();

        if (Config.UPDATER_ENABLED) {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/BungeeCord");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                getProxy().getScheduler().schedule(this, this::checkUpdate, 10, 10, TimeUnit.MINUTES);
        }

        instance = this;

        // Init config files
        Config.load(configPath, getResourceAsStream("config.yml"));
        Locale.load(configPath, this);

        mojangAPI = new MojangAPI(srLogger);
        mineSkinAPI = new MineSkinAPI();
        // Init storage
        if (!initStorage())
            return;

        mojangAPI.setSkinStorage(skinStorage);
        mineSkinAPI.setSkinStorage(skinStorage);

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this));

        // Init commands
        initCommands();

        getProxy().registerChannel("sr:skinchange");

        // Init SkinApplier
        skinApplier = new SkinApplier(this);
        skinApplier.init();

        multiBungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;
    }

    private void initCommands() {
        BungeeCommandManager manager = new BungeeCommandManager(this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            BungeeCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.playercmds") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.getPermissionReplacements().forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, configPath, getResourceAsStream("command-messages.properties"));

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
    }

    private boolean initStorage() {
        skinStorage = new SkinStorage();
        skinStorage.setMojangAPI(mojangAPI);

        // Initialise MySQL
        if (Config.USE_MYSQL) {
            try {
                MySQL mysql = new MySQL(
                        Config.MYSQL_HOST,
                        Config.MYSQL_PORT,
                        Config.MYSQL_DATABASE,
                        Config.MYSQL_USERNAME,
                        Config.MYSQL_PASSWORD
                );

                mysql.openConnection();
                mysql.createTable();

                skinStorage.setMysql(mysql);
            } catch (Exception e) {
                console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer."));
                getProxy().getPluginManager().unregisterListeners(this);
                getProxy().getPluginManager().unregisterCommands(this);
                return false;
            }
        } else {
            skinStorage.loadFolders(getDataFolder());
        }

        // Preload default skins
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate() {
        this.checkUpdate(false);
    }

    private void checkUpdate(final boolean showUpToDate) {
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(final String newVersion, final String downloadUrl, final boolean hasDirectDownload) {
                    outdated = true;

                    updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(msg -> {
                        console.sendMessage(new TextComponent(msg));
                    });
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    updateChecker.getUpToDateMessages(getVersion(), false).forEach(msg -> {
                        console.sendMessage(new TextComponent(msg));
                    });
                }
            });
        });
    }
}
