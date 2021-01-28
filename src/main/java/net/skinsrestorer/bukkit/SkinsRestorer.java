/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.bukkit;

import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import com.google.common.annotations.Beta;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.commands.SkinCommand;
import net.skinsrestorer.bukkit.commands.SrCommand;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.skinfactory.SkinFactory;
import net.skinsrestorer.bukkit.skinfactory.UniversalSkinFactory;
import net.skinsrestorer.bukkit.utils.UpdateDownloaderGithub;
import net.skinsrestorer.shared.storage.*;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.*;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("Duplicates")
public class SkinsRestorer extends JavaPlugin {
    private static @Getter SkinsRestorer instance;
    private @Getter SkinFactory factory;
    private @Getter UpdateChecker updateChecker;
    private final @Getter String configPath = getDataFolder().getPath();

    private @Getter boolean bungeeEnabled;
    private boolean updateDownloaded = false;
    private UpdateDownloaderGithub updateDownloader;
    private CommandSender console;
    private @Getter SRLogger srLogger;
    private @Getter SkinStorage skinStorage;
    private @Getter MojangAPI mojangAPI;
    private @Getter MineSkinAPI mineSkinAPI;
    private @Getter SkinsRestorerAPI skinsRestorerBukkitAPI;

    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void onEnable() {
        console = getServer().getConsoleSender();
        srLogger = new SRLogger(getDataFolder());

        int pluginId = 1669; // SkinsRestorer's ID on bStats, for Bukkit
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", MetricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", MetricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", MetricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("backup_calls", MetricsCounter::collectBackupCalls));

        instance = this;
        factory = new UniversalSkinFactory(this);

        console.sendMessage("§e[§2SkinsRestorer§e] §aDetected Minecraft §e" + ReflectionUtil.serverVersion + "§a, using §e" + factory.getClass().getSimpleName() + "§a.");

        // Detect MundoSK
        if (getServer().getPluginManager().getPlugin("MundoSK") != null) {
            try {
                YamlConfig mundoConfig = new YamlConfig("plugins" + File.separator + "MundoSK" + File.separator, "config", false);
                mundoConfig.reload();
                if (mundoConfig.getBoolean("enable_custom_skin_and_tablist")) {
                    console.sendMessage("§e[§2SkinsRestorer§e] §4----------------------------------------------");
                    console.sendMessage("§e[§2SkinsRestorer§e] §4             [CRITICAL WARNING]");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cWe have detected MundoSK on your server with §e'enable_custom_skin_and_tablist: §4§ntrue§e'§c.");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cThat setting is located in §e/plugins/MundoSK/config.yml");
                    console.sendMessage("§e[§2SkinsRestorer§e] §cYou have to disable ('false') it to get SkinsRestorer to work!");
                    console.sendMessage("§e[§2SkinsRestorer§e] §4----------------------------------------------");
                }
            } catch (Exception ignored) {
            }
        }

        // Check if we are running in bungee mode
        checkBungeeMode();

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

        this.skinStorage = new SkinStorage(SkinStorage.Platform.BUKKIT);

        // Init SkinsGUI click listener even when on bungee
        Bukkit.getPluginManager().registerEvents(new SkinsGUI(this), this);

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

            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:messagechannel");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "sr:messagechannel", (channel, player, message) -> {
                if (!channel.equals("sr:messagechannel"))
                    return;

                Bukkit.getScheduler().runTaskAsynchronously(getInstance(), () -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("OPENGUI")) {
                            Player p = Bukkit.getPlayer(in.readUTF());
                            if (p == null)
                                return;

                            SkinsGUI.getMenus().put(p.getName(), 0);

                            this.requestSkinsFromBungeeCord(p, 0);
                        }

                        if (subChannel.equalsIgnoreCase("returnSkins")) {
                            Player p = Bukkit.getPlayer(in.readUTF());
                            if (p == null)
                                return;

                            int page = in.readInt();

                            short len = in.readShort();
                            byte[] msgBytes = new byte[len];
                            in.readFully(msgBytes);

                            Map<String, Property> skinList = convertToObject(msgBytes);

                            //convert
                            Map<String, Object> newSkinList = new TreeMap<>();

                            skinList.forEach((name, property) -> {
                                newSkinList.put(name, this.getSkinStorage().createProperty(property.getName(), property.getValue(), property.getSignature()));
                            });

                            SkinsGUI skinsGUI = new SkinsGUI(this);
                            ++page; // start counting from 1
                            Inventory inventory = skinsGUI.getGUI(p, page, newSkinList);

                            Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), () -> {
                                p.openInventory(inventory);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            return;
        }

        /* ***************************************** *
         * [!] below is skipped if bungeeEnabled [!] *
         * ***************************************** */

        // Init config files
        Config.load(configPath, getResource("config.yml"));
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
        initCommands();

        // Init listener
        Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);

        // Init API
        this.skinsRestorerBukkitAPI = new SkinsRestorerBukkitAPI(this, this.mojangAPI, this.skinStorage);

        // Run connection check
        if (!bungeeEnabled) {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(this.mojangAPI);
            checker.checkServices();
            ServiceChecker.ServiceCheckResponse response = checker.getResponse();

            if (response.getWorkingUUID() == 0 || response.getWorkingProfile() == 0) {
                console.sendMessage("§c[§4Critical§c] ------------------[§2SkinsRestorer §cis §c§l§nOFFLINE§c] --------------------------------- ");
                console.sendMessage("§c[§4Critical§c] §cPlugin currently can't fetch new skins.");
                console.sendMessage("§c[§4Critical§c] §cSee http://skinsrestorer.net/firewall for wiki ");
                console.sendMessage("§c[§4Critical§c] §cFor support, visit our discord at https://discord.me/servers/skinsrestorer ");
                console.sendMessage("§c[§4Critical§c] ------------------------------------------------------------------------------------------- ");
            }
        }
    }

    public void requestSkinsFromBungeeCord(Player p, int page) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("getSkins");
            out.writeUTF(p.getName());
            out.writeInt(page); // Page

            p.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinClearFromBungeeCord(Player p) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("clearSkin");
            out.writeUTF(p.getName());

            p.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinSetFromBungeeCord(Player p, String skin) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("setSkin");
            out.writeUTF(p.getName());
            out.writeUTF(skin);

            p.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Property> convertToObject(byte[] byteArr) {
        Map<String, Property> map = new TreeMap<>();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
            ObjectInputStream ois = new ObjectInputStream(bis);

            while (bis.available() > 0) {
                map = (Map<String, Property>) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }

    private void initCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);
        // optional: enable unstable api to use help
        manager.enableUnstableAPI("help");

        manager.getCommandConditions().addCondition("permOrSkinWithoutPerm", (context -> {
            BukkitCommandIssuer issuer = context.getIssuer();
            if (issuer.hasPermission("skinsrestorer.command") || Config.SKINWITHOUTPERM)
                return;

            throw new ConditionFailedException("You don't have access to change your skin.");
        }));
        // Use with @Conditions("permOrSkinWithoutPerm")

        CommandReplacements.permissions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.descriptions.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));
        CommandReplacements.syntax.forEach((k, v) -> manager.getCommandReplacements().addReplacement(k, v));

        new CommandPropertiesManager(manager, configPath, getResource("command-messages.properties"));

        manager.registerCommand(new SkinCommand(this));
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
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
                console.sendMessage("§e[§2SkinsRestorer§e] §cCan't connect to MySQL! Disabling SkinsRestorer.");
                e.printStackTrace();
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
            if (!bungeeEnabled) {
                bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml")).getBoolean("settings.bungeecord");
            }
        } catch (Exception e) {
            bungeeEnabled = false;
        }

        StringBuilder sb1 = new StringBuilder("Server is in bungee mode!");

        sb1.append("\nif you are NOT using bungee in your network, set spigot.yml -> bungeecord: false");
        sb1.append("\n\nInstalling Bungee:");
        sb1.append("\nDownload the latest version from https://www.spigotmc.org/resources/skinsrestorer.2124/");
        sb1.append("\nPlace the SkinsRestorer.jar in ./plugins/ folders of every spigot server.");
        sb1.append("\nPlace the plugin in ./plugins/ folder of every BungeeCord server.");
        sb1.append("\nCheck & set on every Spigot server spigot.yml -> bungeecord: true");
        sb1.append("\nRestart (/restart or /stop) all servers [Plugman or /reload are NOT supported, use /stop or /end]");
        sb1.append("\n\nBungeeCord now has SkinsRestorer installed with the integration of Spigot!");
        sb1.append("\nYou may now Configure SkinsRestorer on Bungee (BungeeCord plugins folder /plugins/SkinsRestorer)");

        File warning = new File(getDataFolder() + File.separator + "(README) Use bungee config for settings! (README)");
        try {
            if (!warning.exists() && bungeeEnabled) {
                warning.getParentFile().mkdirs();
                warning.createNewFile();

                try (FileWriter writer = new FileWriter(warning)) {

                    writer.write(String.valueOf(sb1));
                }
            }
            if (warning.exists() && !bungeeEnabled)
                Files.delete(warning.toPath());
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
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> updateChecker.checkForUpdate(new UpdateCallback() {
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
                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), bungeeMode, true, failReason).forEach(msg ->
                        console.sendMessage(msg));
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), bungeeMode).forEach(msg -> console.sendMessage(msg));
            }
        }));
    }

    private static class SkinsRestorerBukkitAPI extends SkinsRestorerAPI {
        private final SkinsRestorer plugin;

        public SkinsRestorerBukkitAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, skinStorage, null);
            this.plugin = plugin;
        }

        // Todo: We need to refactor applySkin through all platforms to behave the same!
        @Beta
        @Override
        public void applySkin(PlayerWrapper player, Object props) {
            plugin.getFactory().applySkin(player.get(Player.class), props);
        }

        @Beta
        @Override
        public void applySkin(PlayerWrapper player) {
            plugin.getFactory().applySkin(player.get(Player.class), this.getSkinData(this.getSkinName(player.get(Player.class).getName())));
        }
    }
}
