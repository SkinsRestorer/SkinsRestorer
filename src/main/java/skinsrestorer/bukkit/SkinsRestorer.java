package skinsrestorer.bukkit;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.bukkit.commands.GUICommand;
import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
import skinsrestorer.bukkit.listener.PlayerJoin;
import skinsrestorer.bukkit.skinfactory.SkinFactory;
import skinsrestorer.bukkit.skinfactory.UniversalSkinFactory;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.shared.update.UpdateCheckerGitHub;
import skinsrestorer.shared.utils.*;

import java.io.*;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends JavaPlugin {
    @Getter
    private static SkinsRestorer instance;
    @Getter
    private SkinFactory factory;
    @Getter
    private UpdateChecker updateChecker;
    @Getter
    private String configPath = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

    private boolean bungeeEnabled;
    private boolean updateDownloaded = false;
    private UpdateDownloaderGithub updateDownloader;
    private CommandSender console;
    @Getter
    private SRLogger srLogger;
    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;

    public String getVersion() {
        return getDescription().getVersion();
    }

    public void onEnable() {
        console = getServer().getConsoleSender();
        srLogger = new SRLogger();

        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SingleLineChart("mineskin_calls", MetricsCounter::collectMineskin_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));

        instance = this;
        factory = new UniversalSkinFactory();

        console.sendMessage("§e[§2SkinsRestorer§e] §aDetected Minecraft §e" + ReflectionUtil.serverVersion + "§a, using §e" + factory.getClass().getSimpleName() + "§a.");

        // Detect MundoSK
        if (getServer().getPluginManager().getPlugin("MundoSK") != null) {
            try {
                YamlConfig mundoConfig = new YamlConfig("plugins" + File.separator + "MundoSK" + File.separator, "config", false);
                mundoConfig.reload();
                if (mundoConfig.getBoolean("enable_custom_skin_and_tablist")) {
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cWe have detected MundoSK on your server with §e'enable_custom_skin_and_tablist: true'§c.");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cThat setting is located in §e/plugins/MundoSK/config.yml");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cYou have to disable it to get SkinsRestorer to work.");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                }
            } catch (Exception ignored) {
            }
        }

        // Check if we are running in bungee mode
        this.checkBungeeMode();

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getDescription().getVersion(), this.srLogger, "SkinsRestorerUpdater/Bukkit");
            this.updateDownloader = new UpdateDownloaderGithub(this);
            this.checkUpdate(bungeeEnabled);

            if (Config.UPDATER_PERIODIC)
                this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                    this.checkUpdate(bungeeEnabled, false);
                }, 20 * 60 * 10, 20 * 60 * 10);
        }

        if (bungeeEnabled) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:skinchange");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "sr:skinchange", (channel, player, message) -> {
                if (!channel.equals("sr:skinchange"))
                    return;

                Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subchannel = in.readUTF();

                        if (subchannel.equalsIgnoreCase("SkinUpdate")) {
                            try {
                                factory.applySkin(player, this.skinStorage.createProperty(in.readUTF(), in.readUTF(), in.readUTF()));
                            } catch (IOException ignored) {
                            }
                            factory.updateSkin(player);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
            return;
        }

        // Init config files
        Config.load(configPath, getResource("config.yml"));
        Locale.load(configPath, this);

        this.mojangAPI = new MojangAPI(this.srLogger);
        this.mineSkinAPI = new MineSkinAPI();
        // Init storage
        if (!this.initStorage())
            return;

        this.mojangAPI.setSkinStorage(this.skinStorage);
        this.mineSkinAPI.setSkinStorage(this.skinStorage);

        // Init commands
        this.initCommands();

        // Init listener
        Bukkit.getPluginManager().registerEvents(new SkinsGUI(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);
    }

    private void initCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            BukkitCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.playercmds") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.getPermissionReplacements().forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, configPath, getResource("command-messages.properties"));

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    private boolean initStorage() {
        this.skinStorage = new SkinStorage();
        this.skinStorage.setMojangAPI(mojangAPI);

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

                this.skinStorage.setMysql(mysql);
            } catch (Exception e) {
                console.sendMessage("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            }
        } else {
            this.skinStorage.loadFolders(getDataFolder());
        }

        // Preload default skins
        Bukkit.getScheduler().runTaskAsynchronously(this, this.skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkBungeeMode() {
        File bungeeModeDisabled = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "disableBungeeMode");
        if (bungeeModeDisabled.exists()) {
            bungeeEnabled = false;
            return;
        }

        try {
            bungeeEnabled = getServer().spigot().getConfig().getBoolean("settings.bungeecord");

            // sometimes it does not get the right "bungeecord: true" setting
            // we will try it again with the old function from SR 13.3
            // https://github.com/DoNotSpamPls/SkinsRestorerX/blob/cbddd95ac36acb5b1afff2b9f48d0fc5b5541cb0/src/main/java/skinsrestorer/bukkit/SkinsRestorer.java#L109
            if (!bungeeEnabled) {
                bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml")).getBoolean("settings.bungeecord");
            }
        } catch (Throwable e) {
            bungeeEnabled = false;
        }

        try {
            File warning = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "Use bungee config for settings!");
            if (!warning.exists() && bungeeEnabled)
                warning.createNewFile();

            if (warning.exists() && !bungeeEnabled)
                warning.delete();
        } catch (Exception ignored) {
        }

        if (bungeeEnabled) {
            this.srLogger.logAlways("-------------------------/Warning\\-------------------------");
            this.srLogger.logAlways("This plugin is running in Bungee mode!");
            this.srLogger.logAlways("You have to do all configuration at config file");
            this.srLogger.logAlways("inside your Bungeecord server.");
            this.srLogger.logAlways("(Bungeecord-Server/plugins/SkinsRestorer/).");
            this.srLogger.logAlways("-------------------------\\Warning/-------------------------");
        }

    }

    private void checkUpdate(boolean bungeeMode) {
        this.checkUpdate(bungeeMode, true);
    }

    private void checkUpdate(boolean bungeeMode, boolean showUpToDate) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                    if (updateDownloaded)
                        return;

                    String failReason = null;
                    if (hasDirectDownload) {
                        if (updateDownloader.downloadUpdate()) {
                            updateDownloaded = true;
                        } else {
                            failReason = updateDownloader.getFailReason().toString();
                        }
                    }
                    updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), bungeeMode, true, failReason).forEach(msg -> {
                        console.sendMessage(msg);
                    });
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    updateChecker.getUpToDateMessages(getVersion(), bungeeMode).forEach(msg -> {
                        console.sendMessage(msg);
                    });
                }
            });
        });
    }
}
