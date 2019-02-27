package skinsrestorer.velocity;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.VelocityCommandIssuer;
import co.aikar.commands.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.optional.qual.MaybePresent;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.CommandPropertiesManager;
import skinsrestorer.shared.utils.CommandReplacements;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.velocity.command.SkinCommand;
import skinsrestorer.velocity.command.SrCommand;
import skinsrestorer.velocity.listener.GameProfileRequest;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by McLive on 16.02.2019.
 */
@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = "13.6.1",
        description = "Skins for offline mode servers.",
        authors = "McLive")
public class SkinsRestorer {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataFolder;
    private MySQL mysql;
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject
    public SkinsRestorer(ProxyServer proxy, Logger logger, @DataDirectory Path dataFolder) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        logger.info("Enabling SkinsRestorer v" + getVersion());

        // Init config files
        Config.load(getClass().getClassLoader().getResourceAsStream("config.yml"));
        Locale.load();

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

        if (Config.USE_NEW_PERMISSIONS)
            CommandReplacements.newPermissions.entrySet().forEach(e -> manager.getCommandReplacements().addReplacement(e.getKey(), e.getValue()));
        else
            CommandReplacements.oldPermissions.entrySet().forEach(e -> manager.getCommandReplacements().addReplacement(e.getKey(), e.getValue()));

        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, getClass().getClassLoader().getResourceAsStream("command-messages.properties"));

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
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
                this.getLogger().info(("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer."));
                return false;
            }
        }

        SkinStorage.init(this.getDataFolder().toFile());
        return true;
    }

    public TextComponent deserialize(String string) {
        return ComponentSerializers.LEGACY.deserialize(string);
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public ExecutorService getService() {
        return service;
    }

    public String getVersion() {
        Optional<PluginContainer> plugin = this.getProxy().getPluginManager().getPlugin("skinsrestorer");

        if (!plugin.isPresent())
            return "";

        Optional<String> version = plugin.get().getDescription().getVersion();

        return version.orElse("");
    }
}
