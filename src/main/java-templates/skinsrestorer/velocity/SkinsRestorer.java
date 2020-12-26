package skinsrestorer.velocity;

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
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.update.UpdateCheckerGitHub;
import skinsrestorer.shared.utils.*;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.velocity.command.SkinCommand;
import skinsrestorer.velocity.command.SrCommand;
import skinsrestorer.velocity.listener.GameProfileRequest;
import skinsrestorer.velocity.utils.SkinApplier;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created by McLive on 16.02.2019.
 */
@Plugin(id = "skinsrestorer", name = "${project.name}", version = "${project.version}", description = "${project.description}", authors = "McLive")
public class SkinsRestorer {
    @Getter
    private final ProxyServer proxy;
    @Getter
    private SRLogger logger;
    @Getter
    private final Path dataFolder;
    @Getter
    private SkinApplier skinApplier;
    @Getter
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    @Getter
    private String configPath = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

    private boolean outdated;
    private CommandSource console;
    private UpdateChecker updateChecker;

    @Getter
    private SkinStorage skinStorage;
    @Getter
    private MojangAPI mojangAPI;
    @Getter
    private MineSkinAPI mineSkinAPI;
    @Getter
    private SkinsRestorerVelocityAPI skinsRestorerVelocityAPI;

    @Inject
    public SkinsRestorer(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = new SRLogger(dataFolder.toFile());
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        logger.logAlways("Enabling SkinsRestorer v" + getVersion());
        console = this.proxy.getConsoleCommandSource();

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getVersion(), this.getLogger(), "SkinsRestorerUpdater/Velocity");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                this.getProxy().getScheduler().buildTask(this, this::checkUpdate).repeat(10, TimeUnit.MINUTES).delay(10, TimeUnit.MINUTES).schedule();
        }

        this.skinStorage = new SkinStorage();

        // Init config files
        Config.load(configPath, getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load(configPath);

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
        this.skinApplier = new SkinApplier(this);

        // Init API
        this.skinsRestorerVelocityAPI = new SkinsRestorerVelocityAPI(this, this.mojangAPI, this.skinStorage);

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

        new CommandPropertiesManager(manager, configPath, getClass().getClassLoader().getResourceAsStream("command-messages.properties"));

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
                logger.logAlways("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
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
        getService().execute(() -> {
            updateChecker.checkForUpdate(new UpdateCallback() {
                @Override
                public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                    outdated = true;

                    updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), false).forEach(msg -> {
                        console.sendMessage(deserialize(msg));
                    });
                }

                @Override
                public void upToDate() {
                    if (!showUpToDate)
                        return;

                    updateChecker.getUpToDateMessages(getVersion(), false).forEach(msg -> {
                        console.sendMessage(deserialize(msg));
                    });
                }
            });
        });
    }

    public TextComponent deserialize(String string) {
        return LegacyComponentSerializer.legacy().deserialize(string);
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = this.getProxy().getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getDescription().getVersion();

        return version.orElse("");
    }
}
