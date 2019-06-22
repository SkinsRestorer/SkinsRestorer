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
import skinsrestorer.shared.utils.CommandPropertiesManager;
import skinsrestorer.shared.utils.CommandReplacements;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.update.UpdateChecker;
import skinsrestorer.velocity.command.SkinCommand;
import skinsrestorer.velocity.command.SrCommand;
import skinsrestorer.velocity.listener.GameProfileRequest;

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
    private final Logger logger;
    @Getter
    private final Path dataFolder;
    @Getter
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    @Getter
    private String configPath = "plugins" + File.separator + "SkinsRestorer" + File.separator + "";

    private boolean outdated;
    private CommandSource console;
    private UpdateChecker updateChecker;

    @Inject
    public SkinsRestorer(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        logger.info("Enabling SkinsRestorer v" + getVersion());
        console = this.proxy.getConsoleCommandSource();

        // Check for updates
        if (Config.UPDATER_ENABLED) {
            this.updateChecker = new UpdateCheckerGitHub(2124, this.getVersion(), this.getLogger(), "SkinsRestorerUpdater/Velocity");
            this.checkUpdate(true);

            if (Config.UPDATER_PERIODIC)
                this.getProxy().getScheduler().buildTask(this, this::checkUpdate).repeat(5, TimeUnit.MINUTES).delay(5, TimeUnit.MINUTES).schedule();
        }

        // Init config files
        Config.load(configPath, getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load(configPath);

        // Init storage
        if (!this.initStorage())
            return;

        // Init listener
        proxy.getEventManager().register(this, new GameProfileRequest(this));

        // Init commands
        this.initCommands();

        // Init SkinApplier

        logger.info("Enabled SkinsRestorer v" + getVersion());
    }

    @Subscribe
    public void onShutDown(ProxyShutdownEvent ev) {
        logger.info("Disabling SkinsRestorer v" + getVersion());
        logger.info("Disabled SkinsRestorer v" + getVersion());
    }

    private void initCommands() {
        VelocityCommandManager manager = new VelocityCommandManager(this.getProxy(), this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            VelocityCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.playercmds") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.getPermissionReplacements().forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

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
                        Config.MYSQL_PASSWORD
                );

                mysql.openConnection();
                mysql.createTable();

                SkinStorage.init(mysql);
                return true;

            } catch (Exception e) {
                this.getLogger().info(("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer."));
                return false;
            }
        }

        SkinStorage.init(this.getDataFolder().toFile());

        // Preload default skins
        this.getService().execute(SkinStorage::preloadDefaultSkins);
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
