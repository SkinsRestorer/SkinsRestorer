package skinsrestorer.bukkit;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
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
import skinsrestorer.shared.utils.*;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.io.*;

public class SkinsRestorer extends JavaPlugin {

    private static SkinsRestorer instance;
    private SkinFactory factory;
    private MySQL mysql;
    private boolean bungeeEnabled;
    private UpdateChecker updateChecker;
    private UpdateDownloader updateDownloader;
    private CommandSender console;

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public SkinFactory getFactory() {
        return factory;
    }

    public MySQL getMySQL() {
        return mysql;
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

    public UpdateChecker getUpdateChecker() {
        return this.updateChecker;
    }

    public void onEnable() {
        console = getServer().getConsoleSender();

        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));

        instance = this;
        factory = new UniversalSkinFactory();

        console.sendMessage("§e[§2SkinsRestorer§e] §aDetected Minecraft §e" + ReflectionUtil.serverVersion + "§a, using §e" + factory.getClass().getSimpleName() + "§a.");

        // Detect ChangeSkin
        if (getServer().getPluginManager().getPlugin("ChangeSkin") != null) {
            console.sendMessage("§e[§2SkinsRestorer§e] §cWe have detected ChangeSkin on your server, disabling SkinsRestorer.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check if we are running in bungee mode
        this.checkBungeeMode();

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateChecker(2124, this.getDescription().getVersion(), this.getLogger(), "SkinsRestorerUpdater/Bukkit");
            this.updateDownloader = new UpdateDownloader(this);
            this.checkUpdate(bungeeEnabled);

            if (Config.UPDATER_PERIODIC)
                this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                    this.checkUpdate(bungeeEnabled, false);
                }, 20 * 60 * 30, 20 * 60 * 30);
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
                                factory.applySkin(player,
                                        SkinStorage.createProperty(in.readUTF(), in.readUTF(), in.readUTF()));
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
        Config.load(getResource("config.yml"));
        Locale.load();

        // Init storage
        if (!this.initStorage())
            return;

        // Init commands
        this.initCommands();

        // Init listener
        Bukkit.getPluginManager().registerEvents(new SkinsGUI(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(), this);

        // Preload default skins
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            if (Config.DEFAULT_SKINS_ENABLED)
                for (String skin : Config.DEFAULT_SKINS)
                    try {
                        SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
                    } catch (SkinRequestException e) {
                        if (SkinStorage.getSkinData(skin) == null)
                            console.sendMessage("§e[§2SkinsRestorer§e] §cDefault Skin '" + skin + "' request error: " + e.getReason());
                    }
        });

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

        if (Config.USE_NEW_PERMISSIONS)
            CommandReplacements.newPermissions.entrySet().forEach(e -> manager.getCommandReplacements().addReplacement(e.getKey(), e.getValue()));
        else
            CommandReplacements.oldPermissions.entrySet().forEach(e -> manager.getCommandReplacements().addReplacement(e.getKey(), e.getValue()));

        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, getResource("command-messages.properties"));

        manager.registerCommand(new SkinCommand());
        manager.registerCommand(new SrCommand());
        manager.registerCommand(new GUICommand());
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
                console.sendMessage("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            }
        }

        SkinStorage.init(getDataFolder());
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
    }

    private void checkUpdate(boolean bungeeMode) {
        this.checkUpdate(bungeeMode, true);
    }

    private void checkUpdate(boolean bungeeMode, boolean showUpToDate) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                    if (bungeeMode) {
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |---------------|");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |  §eBungee Mode§a  |");
                    }
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §c" + getVersion());
                    if (hasDirectDownload) {
                        console.sendMessage("§e[§2SkinsRestorer§e]     A new version is available! Downloading it now...");
                        if (updateDownloader.downloadUpdate()) {
                            console.sendMessage("§e[§2SkinsRestorer§e] Update downloaded successfully, it will be applied on the next restart.");
                        } else {
                            // Update failed
                            console.sendMessage("§e[§2SkinsRestorer§e] §cCould not download the update, reason: " + updateDownloader.getFailReason());
                        }
                    } else {
                        console.sendMessage("§e[§2SkinsRestorer§e] §e    A new version is available! Download it at:");
                        console.sendMessage("§e[§2SkinsRestorer§e] §e    " + downloadUrl);
                    }
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                    if (bungeeMode) {
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |---------------|");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |  §eBungee Mode§a  |");
                    }
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §a" + getVersion());
                    console.sendMessage("§e[§2SkinsRestorer§e] §a    This is the latest version!");
                    console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                }
            });
        });
    }
}
