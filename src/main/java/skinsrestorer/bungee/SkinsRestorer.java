package skinsrestorer.bungee;

import co.aikar.commands.BungeeCommandIssuer;
import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.ConditionFailedException;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.bungee.commands.SrCommand;
import skinsrestorer.bungee.commands.SkinCommand;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.*;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SkinsRestorer extends Plugin {

    private static SkinsRestorer instance;
    private MySQL mysql;
    private boolean multibungee;
    private boolean outdated;
    private UpdateChecker updateChecker;
    private CommandSender console;
    private String configPath = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public MySQL getMySQL() {
        return mysql;
    }

    public String getVersion() {
        return getDescription().getVersion().replace("-SNAPSHOT", "");
    }

    public boolean isMultiBungee() {
        return multibungee;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public String getConfigPath() {
        return configPath;
    }

    @Override
    public void onEnable() {
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));

        console = getProxy().getConsole();

        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateChecker(2124, this.getDescription().getVersion(), this.getLogger(), "SkinsRestorerUpdater/BungeeCord");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                this.getProxy().getScheduler().schedule(this, this::checkUpdate, 30, 30, TimeUnit.MINUTES);
        }

        instance = this;

        // Init config files
        Config.load(configPath, getResourceAsStream("config.yml"));
        Locale.load(configPath);

        // Init storage
        if (!this.initStorage())
            return;

        // Init listener
        getProxy().getPluginManager().registerListener(this, new LoginListener(this));

        // Init commands
        this.initCommands();

        getProxy().registerChannel("sr:skinchange");
        SkinApplier.init();

        multibungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;
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

        manager.registerCommand(new SkinCommand());
        manager.registerCommand(new SrCommand());
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (Config.USE_MYSQL) {
            try {
                this.mysql = new MySQL(
                        Config.MYSQL_HOST,
                        Config.MYSQL_PORT,
                        Config.MYSQL_DATABASE,
                        Config.MYSQL_USERNAME,
                        Config.MYSQL_PASSWORD
                );

                this.mysql.openConnection();
                this.mysql.createTable();

                SkinStorage.init(this.mysql);
                return true;

            } catch (Exception e) {
                console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer."));
                getProxy().getPluginManager().unregisterListeners(this);
                getProxy().getPluginManager().unregisterCommands(this);
                return false;
            }
        }

        SkinStorage.init(getDataFolder());

        // Preload default skins
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), SkinStorage::preloadDefaultSkins);
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
