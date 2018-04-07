package skinsrestorer.bukkit;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.SpigetUpdate;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;
import skinsrestorer.bukkit.commands.GUICommand;
import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class SkinsRestorer extends JavaPlugin {

    private static SkinsRestorer instance;
    private SkinFactory factory;
    private MySQL mysql;
    private boolean bungeeEnabled;

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

    public void onEnable() {

        ConsoleCommandSender console = getServer().getConsoleSender();

        @SuppressWarnings("unused")
        MetricsLite metrics = new MetricsLite(this);

        SpigetUpdate updater = new SpigetUpdate(this, 2124);
        updater.setVersionComparator(VersionComparator.EQUAL);
        updater.setVersionComparator(VersionComparator.SEM_VER);

        instance = this;
        factory = new UniversalSkinFactory();
        Config.load(getResource("config.yml"));
        Locale.load();

        console.sendMessage("§e[§2SkinsRestorer§e] §aDetected Minecraft §e" + ReflectionUtil.serverVersion + "§a, using §e" + factory.getClass().getSimpleName() + "§a.");

        // Detect ChangeSkin
        if (getServer().getPluginManager().getPlugin("ChangeSkin") != null) {
            console.sendMessage("§e[§2SkinsRestorer§e] §cWe have detected ChangeSkin on your server, disabling SkinsRestorer.");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Bungeecord stuff
        try {
            bungeeEnabled = getServer().spigot().getConfig().getBoolean("settings.bungeecord");

            // sometimes it does not get the right "bungeecord: true" setting
            // we will try it again with the old function from SR 13.3
            // https://github.com/DoNotSpamPls/SkinsRestorerX/blob/cbddd95ac36acb5b1afff2b9f48d0fc5b5541cb0/src/main/java/skinsrestorer/bukkit/SkinsRestorer.java#L109
            if (!bungeeEnabled) {
                bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml")).getBoolean("settings.bungeecord");
            }
        } catch (Exception e) {
            bungeeEnabled = false;
        }

        if (bungeeEnabled) {

            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "SkinsRestorer", (channel, player, message) -> {
                if (!channel.equals("SkinsRestorer"))
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

            if (Config.UPDATER_ENABLED) {
                updater.checkForUpdate(new UpdateCallback() {
                    @Override
                    public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                        if (hasDirectDownload) {
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    |---------------|");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    |  §eBungee Mode§a  |");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §c" + getVersion());
                            console.sendMessage("§e[§2SkinsRestorer§e]     A new version is available! Downloading it now...");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            if (updater.downloadUpdate()) {
                                console.sendMessage("§e[§2SkinsRestorer§e] Update downloaded successfully, it will be applied on the next restart.");
                            } else {
                                // Update failed
                                console.sendMessage("§e[§2SkinsRestorer§e] §cCould not download the update, reason: " + updater.getFailReason());
                            }
                        }
                    }

                    @Override
                    public void upToDate() {
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |---------------|");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    |  §eBungee Mode§a  |");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                        console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §a" + getVersion());
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    This is the latest version!");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    }
                });
            }

            return;
        }

        // Initialise MySQL
        if (Config.USE_MYSQL)
            SkinStorage.init(mysql = new MySQL(
                    Config.MYSQL_HOST,
                    Config.MYSQL_PORT,
                    Config.MYSQL_DATABASE,
                    Config.MYSQL_USERNAME,
                    Config.MYSQL_PASSWORD
            ));
        else
            SkinStorage.init(getDataFolder());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new CooldownStorage(), 0, 20);

        // Commands
        getCommand("skinsrestorer").setExecutor(new SrCommand());
        getCommand("skin").setExecutor(new SkinCommand());
        getCommand("skins").setExecutor(new GUICommand());

        Bukkit.getPluginManager().registerEvents(new SkinsGUI(), this);
        Bukkit.getPluginManager().registerEvents(new Listener() {

            // LoginEvent happens on attemptLogin so its the best place to set the skin
            @EventHandler
            public void onLogin(PlayerJoinEvent e) {
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    try {
                        if (Config.DISABLE_ONJOIN_SKINS) {
                            // factory.applySkin(e.getPlayer(), SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.getPlayer().getName())));
                            // shouldn't it just skip it if it's true?
                            return;
                        }
                        if (Config.DEFAULT_SKINS_ENABLED)
                            if (SkinStorage.getPlayerSkin(e.getPlayer().getName()) == null) {
                                List<String> skins = Config.DEFAULT_SKINS;
                                int randomNum = (int) (Math.random() * skins.size());
                                factory.applySkin(e.getPlayer(),
                                        SkinStorage.getOrCreateSkinForPlayer(skins.get(randomNum)));
                                return;
                            }

                        factory.applySkin(e.getPlayer(), SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName()));
                    } catch (SkinRequestException ignored) {
                    }
                });
            }
        }, this);

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {

            if (Config.UPDATER_ENABLED) {
                updater.checkForUpdate(new UpdateCallback() {
                    @Override
                    public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                        if (hasDirectDownload) {
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §c" + getVersion());
                            console.sendMessage("§e[§2SkinsRestorer§e]     A new version is available! Downloading it now...");
                            console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                            if (updater.downloadUpdate()) {
                                console.sendMessage("§e[§2SkinsRestorer§e] Update downloaded successfully, it will be applied on the next restart.");
                            } else {
                                // Update failed
                                console.sendMessage("§e[§2SkinsRestorer§e] §cCould not download the update, reason: " + updater.getFailReason());
                            }
                        }
                    }

                    @Override
                    public void upToDate() {
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    +===============+");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                        console.sendMessage("§e[§2SkinsRestorer§e] §b    Current version: §a" + getVersion());
                        console.sendMessage("§e[§2SkinsRestorer§e] §a    This is the latest version!");
                        console.sendMessage("§e[§2SkinsRestorer§e] §a----------------------------------------------");
                    }
                });
            }

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
}
