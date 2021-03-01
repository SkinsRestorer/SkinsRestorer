package skinsrestorer.sponge;

import co.aikar.commands.SpongeCommandManager;
import com.google.inject.Inject;
import lombok.Getter;
import org.bstats.sponge.Metrics2;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.shared.update.UpdateCheckerGitHub;
import skinsrestorer.shared.utils.*;
import skinsrestorer.sponge.commands.SkinCommand;
import skinsrestorer.sponge.commands.SrCommand;
import skinsrestorer.sponge.listeners.LoginListener;
import skinsrestorer.sponge.utils.SkinApplier;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "skinsrestorer", name = "${project.name}", version = "${project.version}")
public class SkinsRestorer {
    @Getter
    private static SkinsRestorer instance;
    @Getter
    private String configPath;
    @Getter
    private SkinApplier skinApplier;
    @Getter
    private SRLogger srLogger;
    @Getter
    private boolean bungeeEnabled = false;
    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;
    @Getter
    private SkinsRestorerSpongeAPI skinsRestorerSpongeAPI;

    private UpdateChecker updateChecker;
    private CommandSource console;

    private final Metrics2 metrics;

    // The metricsFactory parameter gets injected using @Inject
    @Inject
    public SkinsRestorer(Metrics2.Factory metricsFactory) {
        int pluginId = 2337; // SkinsRestorer's ID on bStats, for Sponge
        metrics = metricsFactory.make(pluginId);
    }

    @Listener
    public void onInitialize(GameInitializationEvent e) {
        instance = this;
        console = Sponge.getServer().getConsole();
        configPath = Sponge.getGame().getConfigManager().getPluginConfig(this).getDirectory().toString();
        this.srLogger = new SRLogger(new File(configPath));

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getVersion(), this.srLogger, "SkinsRestorerUpdater/Sponge");
            this.checkUpdate(bungeeEnabled);

            if (Config.UPDATER_PERIODIC)
                Sponge.getScheduler().createTaskBuilder().execute(() -> {
                    this.checkUpdate(bungeeEnabled, false);
                }).interval(10, TimeUnit.MINUTES).delay(10, TimeUnit.MINUTES);
        }

        this.skinStorage = new SkinStorage();

        // Init config files
        Config.load(configPath, getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load(configPath);

        this.mojangAPI = new MojangAPI(this.srLogger);
        this.mineSkinAPI = new MineSkinAPI(this.srLogger);

        this.skinStorage.setMojangAPI(mojangAPI);
        // Init storage
        if (!this.initStorage())
            return;

        this.mojangAPI.setSkinStorage(this.skinStorage);
        this.mineSkinAPI.setSkinStorage(this.skinStorage);

        // Init commands
        this.initCommands();

        // Init SkinApplier
        this.skinApplier = new SkinApplier(this);

        // Init API
        this.skinsRestorerSpongeAPI = new SkinsRestorerSpongeAPI(this, this.mojangAPI, this.skinStorage);

        // Run connection check
        ServiceChecker checker = new ServiceChecker();
        checker.setMojangAPI(this.mojangAPI);
        checker.checkServices();
        ServiceChecker.ServiceCheckResponse response = checker.getResponse();

        if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
            System.out.println("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- ");
            System.out.println("§c[§4Critical§c] §cPlugin currently can't fetch new skins.");
            System.out.println("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for wiki ");
            System.out.println("§c[§4Critical§c] §cFor support, visit our discord at https://discord.me/servers/skinsrestorer ");
            System.out.println("§c[§4Critical§c] ------------------------------------------------------------------------------------------- ");
        }
    }

    @Listener
    public void onServerStarted(GameStartedServerEvent event) {
        if (!Sponge.getServer().getOnlineMode()) {
            Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Auth.class, new LoginListener(this));
        }

        metrics.addCustomChart(new Metrics2.SingleLineChart("mineskin_calls", MetricsCounter::collectMineskin_calls));
        metrics.addCustomChart(new Metrics2.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics2.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics2.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));
    }

    private void initCommands() {
        Sponge.getPluginManager().getPlugin("skinsrestorer").ifPresent(pluginContainer -> {
            SpongeCommandManager manager = new SpongeCommandManager(pluginContainer);
            // optional: enable unstable api to use help
            manager.enableUnstableAPI("help");

            CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
            CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

            new CommandPropertiesManager(manager, configPath, getClass().getClassLoader().getResourceAsStream("command-messages.properties"));

            manager.registerCommand(new SkinCommand(this));
            manager.registerCommand(new SrCommand(this));
            //manager.registerCommand(new GUICommand());
        });
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
                System.out.println("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
                return false;
            }
        } else {
            this.skinStorage.loadFolders(new File(configPath));
        }

        // Preload default skins
        Sponge.getScheduler().createAsyncExecutor(this).execute(this.skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkUpdate(boolean bungeeMode) {
        this.checkUpdate(bungeeMode, true);
    }

    private void checkUpdate(boolean bungeeMode, boolean showUpToDate) {
        Sponge.getScheduler().createAsyncExecutor(this).execute(() -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                    updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), bungeeMode).forEach(msg -> {
                        console.sendMessage(parseMessage(msg));
                    });
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    updateChecker.getUpToDateMessages(getVersion(), bungeeMode).forEach(msg -> {
                        console.sendMessage(parseMessage(msg));
                    });
                }
            });
        });
    }

    public Text parseMessage(String msg) {
        return Text.builder(msg).build();
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = Sponge.getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getVersion();

        return version.orElse("");
    }
}
