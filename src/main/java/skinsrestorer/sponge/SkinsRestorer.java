package skinsrestorer.sponge;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import skinsrestorer.shared.utils.MetricsCounter;
import skinsrestorer.sponge.commands.SetSkinCommand;
import skinsrestorer.sponge.commands.SkinCommand;
import skinsrestorer.sponge.listeners.LoginListener;
import skinsrestorer.sponge.utils.SkinApplier;

import java.io.File;

@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = "13.5.3-SNAPSHOT")

public class SkinsRestorer {

    private static SkinsRestorer instance;

    private String directory;
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private ConfigurationLoader<CommentedConfigurationNode> dataManager;
    private CommentedConfigurationNode configRoot;
    private CommentedConfigurationNode dataRoot;

    private SkinApplier skinApplier;

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public CommentedConfigurationNode getConfigRoot() {
        return configRoot;
    }

    public CommentedConfigurationNode getDataRoot() {
        return dataRoot;
    }

    @Inject
    private Metrics2 metrics;

    @Listener
    public void onInitialize(GameInitializationEvent e) {
        instance = this;

        directory = Sponge.getGame().getConfigManager().getPluginConfig(this).getDirectory().toString();

        try {
            reloadConfigs();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CommandSpec skinCommand = CommandSpec.builder().description(Text.of("Set your skin"))
                .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("skin"))))
                .permission("skinsrestorer.playercmds")
                .executor(new SkinCommand(this)).build();

        Sponge.getCommandManager().register(this, skinCommand, "skin");

        CommandSpec setskinCommand = CommandSpec.builder().description(Text.of("Set someone skin"))
                .arguments(GenericArguments.onlyOne(GenericArguments.player(Text.of("player"))),
                        GenericArguments.remainingJoinedStrings(Text.of("skin")))
                .permission("skinsrestorer.admincmds")
                .executor(new SetSkinCommand(this)).build();

        Sponge.getCommandManager().register(this, setskinCommand, "setskin");

        this.skinApplier = new SkinApplier(this);
    }

    @Listener
    public void onServerStared(GameStartedServerEvent event) {
        if (!Sponge.getServer().getOnlineMode()) {
            Sponge.getEventManager().registerListener(this, ClientConnectionEvent.Login.class, new LoginListener());
        }

        metrics.addCustomChart(new Metrics2.SingleLineChart("minetools_calls", MetricsCounter::collectMinetools_calls));
        metrics.addCustomChart(new Metrics2.SingleLineChart("mojang_calls", MetricsCounter::collectMojang_calls));
        metrics.addCustomChart(new Metrics2.SingleLineChart("backup_calls", MetricsCounter::collectBackup_calls));
    }

    public void reloadConfigs() {
        try {
            File dir = new File(directory);

            if (!dir.exists())
                dir.mkdirs();

            File config = new File(directory + File.separator + "config.conf");
            File data = new File(directory + File.separator + "data.conf");

            // Throws
            // [21:31:02 ERROR] [STDERR]: java.lang.NullPointerException
            //         [21:31:02 ERROR] [STDERR]:      at skinsrestorer.sponge.SkinsRestorer.reloadConfigs(SkinsRestorer.java:84)
            //         [21:31:02 ERROR] [STDERR]:      at skinsrestorer.sponge.SkinsRestorer.onInitialize(SkinsRestorer.java:52)
            /*if (!config.exists()) {
                config.createNewFile();
                try {
                    Files.copy(this.getClass().getResource("config.conf").openStream(),
                            config.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/

            if (!data.exists())
                data.createNewFile();

            configManager = HoconConfigurationLoader.builder().setPath(config.toPath()).build();
            dataManager = HoconConfigurationLoader.builder().setPath(data.toPath()).build();

            configRoot = configManager.load();
            dataRoot = dataManager.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfigs() {
        try {
            configManager.save(configRoot);
            dataManager.save(dataRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SkinApplier getSkinApplier() {
        return skinApplier;
    }

    public void setSkinApplier(SkinApplier skinApplier) {
        this.skinApplier = skinApplier;
    }
}
