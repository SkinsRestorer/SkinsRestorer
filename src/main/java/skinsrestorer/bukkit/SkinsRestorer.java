package skinsrestorer.bukkit;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;

import org.inventivetalent.update.spiget.SpigetUpdate;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import skinsrestorer.bukkit.commands.GUICommand;
import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
import skinsrestorer.bukkit.menu.SkinsGUI;
import skinsrestorer.bukkit.skinfactory.SkinFactory;
import skinsrestorer.bukkit.skinfactory.UniversalSkinFactory;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;

public class SkinsRestorer extends JavaPlugin {

    private static SkinsRestorer instance;
    Logger logger = Bukkit.getLogger();
    private SkinFactory factory;
    private MySQL mysql;
    private boolean bungeeEnabled;
    private boolean outdated;

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public void log(String msg) {
        logger.info("[SkinsRestorer] " + msg);
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

    public boolean isOutdated() {
        return outdated;
    }

    @Override
    public void onEnable() {

        Metrics metrics = new Metrics(this);
        SpigetUpdate updater = new SpigetUpdate(this, 2124);
        updater.setVersionComparator(VersionComparator.EQUAL);
        updater.setVersionComparator(VersionComparator.SEM_VER_SNAPSHOT);

        instance = this;

        try {
            // Doesn't support Cauldron and stuff..
            Class.forName("net.minecraftforge.cauldron.CauldronHooks");
            log("SkinsRestorer doesn't support Cauldron, Thermos or KCauldron, Sorry :(");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        } catch (Exception e) {
            try {
                // Checking for old versions
                factory = (SkinFactory) Class
                        .forName("skinsrestorer.bukkit.skinfactory.SkinFactory_" + ReflectionUtil.serverVersion)
                        .newInstance();
            } catch (Exception ex) {
                // 1.8+++
                factory = new UniversalSkinFactory();
            }
        }
        log("Detected Minecraft " + ReflectionUtil.serverVersion + ", using "
                + factory.getClass().getSimpleName());


        // Multiverse Core support.
        MCoreAPI.init();
        if (MCoreAPI.check())
            log("Detected Multiverse-Core! Using it for dimensions.");


        // Bungeecord stuff
        try {
            bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml"))
                    .getBoolean("settings.bungeecord");
        } catch (Exception e) {
            bungeeEnabled = false;
        }

        if (bungeeEnabled) {

            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "SkinsRestorer", new PluginMessageListener() {
                @Override
                public void onPluginMessageReceived(String channel, final Player player, final byte[] message) {
                    if (!channel.equals("SkinsRestorer"))
                        return;

                    Bukkit.getScheduler().runTaskAsynchronously(getInstance(), new Runnable() {

                        @Override
                        public void run() {

                            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                            try {
                                String subchannel = in.readUTF();

                                if (subchannel.equalsIgnoreCase("SkinUpdate")) {
                                    try {
                                        factory.applySkin(player,
                                                SkinStorage.createProperty(in.readUTF(), in.readUTF(), in.readUTF()));
                                    } catch (Exception e) {
                                    }
                                    factory.updateSkin(player);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            if (Config.UPDATER_ENABLED) {
                updater.checkForUpdate(new UpdateCallback() {
                    @Override
                    public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                        if (hasDirectDownload) {
                            log("----------------------------------------------");
                            log("    +===============+");
                            log("    | SkinsRestorer |");
                            log("    |---------------|");
                            log("    |  Bungee Mode  |");
                            log("    +===============+");
                            log("----------------------------------------------");
                            log("    Current version: " + getVersion());
                            log("    A new version is available! Downloading it now...");
                            log("----------------------------------------------");
                            if (updater.downloadUpdate()) {
                                log("Update download successful, will be applied on next restart");
                            } else {
                                // Update failed
                                log("Update download failed, reason is " + updater.getFailReason());
                            }
                        }
                    }

                    @Override
                    public void upToDate() {
                        log("----------------------------------------------");
                        log("    +===============+");
                        log("    | SkinsRestorer |");
                        log("    |---------------|");
                        log("    |  Bungee Mode  |");
                        log("    +===============+");
                        log("----------------------------------------------");
                        log("    Current version: " + getVersion());
                        log("    This is the latest version!");
                        log("----------------------------------------------");
                    }
                });
            }

            return;
        }

        // Config stuff
        Config.load(getResource("config.yml"));
        Locale.load();

        if (Config.USE_MYSQL)
            SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
                    Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
        else
            SkinStorage.init(getDataFolder());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new CooldownStorage(), 0, 1 * 20);

        // Commands
        getCommand("skinsrestorer").setExecutor(new SrCommand());
        getCommand("skin").setExecutor(new SkinCommand());
        getCommand("skins").setExecutor(new GUICommand());

        Bukkit.getPluginManager().registerEvents(new SkinsGUI(), this);
        Bukkit.getPluginManager().registerEvents(new Listener() {

            // LoginEvent happens on attemptLogin so its the best place to set
            // the skin
            @EventHandler
            public void onLogin(PlayerJoinEvent e) {
                try {
                    if (Config.DISABLE_ONJOIN_SKINS) {
                        factory.applySkin(e.getPlayer(),
                                SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.getPlayer().getName())));
                        return;
                    }
                    if (Config.DEFAULT_SKINS_ENABLED)
                        if (SkinStorage.getPlayerSkin(e.getPlayer().getName()) == null) {
                            List<String> skins = Config.DEFAULT_SKINS;
                            int randomNum = 0 + (int) (Math.random() * skins.size());
                            factory.applySkin(e.getPlayer(),
                                    SkinStorage.getOrCreateSkinForPlayer(skins.get(randomNum)));
                            return;
                        }
                    factory.applySkin(e.getPlayer(), SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName()));
                } catch (Exception ex) {
                }
            }
        }, this);

        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

            @Override
            public void run() {

                if (Config.UPDATER_ENABLED) {
                    updater.checkForUpdate(new UpdateCallback() {
                        @Override
                        public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                            if (hasDirectDownload) {
                                log("----------------------------------------------");
                                log("    +===============+");
                                log("    | SkinsRestorer |");
                                log("    +===============+");
                                log("----------------------------------------------");
                                log("    Current version: " + getVersion());
                                log("    A new version is available! Downloading it now...");
                                log("----------------------------------------------");
                                if (updater.downloadUpdate()) {
                                    log("Update download successful, will be applied on next restart");
                                } else {
                                    // Update failed
                                    log("Update download failed, reason is " + updater.getFailReason());
                                }
                            }
                        }

                        @Override
                        public void upToDate() {
                            log("----------------------------------------------");
                            log("    +===============+");
                            log("    | SkinsRestorer |");
                            log("    +===============+");
                            log("----------------------------------------------");
                            log("    Current version: " + getVersion());
                            log("    This is the latest version!");
                            log("----------------------------------------------");
                        }
                    });
                }

                if (Config.DEFAULT_SKINS_ENABLED)
                    for (String skin : Config.DEFAULT_SKINS)
                        try {
                            SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
                        } catch (SkinRequestException e) {
                            if (SkinStorage.getSkinData(skin) == null)
                                log( "Default Skin '" + skin + "' request error: " + e.getReason());
                        }
            }

        });

    }
}
