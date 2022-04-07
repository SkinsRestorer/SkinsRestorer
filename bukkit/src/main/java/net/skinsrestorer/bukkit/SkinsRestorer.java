/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
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
 */
package net.skinsrestorer.bukkit;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.serverinfo.Platform;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.commands.SkinCommand;
import net.skinsrestorer.bukkit.commands.SrCommand;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.listener.ProtocolLibJoinListener;
import net.skinsrestorer.bukkit.utils.BukkitConsoleImpl;
import net.skinsrestorer.bukkit.utils.UpdateDownloaderGithub;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.storage.YamlConfig;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.WrapperFactory;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.LoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("Duplicates")
public class SkinsRestorer extends JavaPlugin implements ISRPlugin {
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final SRLogger srLogger = new SRLogger(new LoggerImpl(getServer().getLogger(), new BukkitConsoleImpl(getServer().getConsoleSender())), true);
    private final MojangAPI mojangAPI = new MojangAPI(srLogger, Platform.BUKKIT, metricsCounter);
    private final MineSkinAPI mineSkinAPI = new MineSkinAPI(srLogger, mojangAPI, metricsCounter);
    private final SkinStorage skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
    private final SkinsRestorerAPI skinsRestorerAPI = new SkinsRestorerBukkitAPI(mojangAPI, skinStorage);
    private final Path dataFolderPath = getDataFolder().toPath();
    private SkinApplierBukkit skinApplierBukkit;
    private boolean bungeeEnabled;
    private boolean updateDownloaded = false;
    private UpdateChecker updateChecker;
    private UpdateDownloaderGithub updateDownloader;
    private SkinCommand skinCommand;
    private PaperCommandManager manager;

    @SuppressWarnings("unchecked")
    private static Map<String, GenericProperty> convertToObject(byte[] byteArr) {
        Map<String, GenericProperty> map = new TreeMap<>();
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
            ObjectInputStream ois = new ObjectInputStream(bis);

            while (bis.available() > 0) {
                map = (Map<String, GenericProperty>) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void runAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return getServer().getOnlinePlayers().stream().map(WrapperBukkit::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public void onEnable() {
        srLogger.load(dataFolderPath);
        Path updaterDisabled = dataFolderPath.resolve("noupdate.txt");

        Metrics metrics = new Metrics(this, 1669);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        try {
            skinApplierBukkit = new SkinApplierBukkit(this, srLogger);
        } catch (InitializeException e) {
            srLogger.severe(ChatColor.RED + ChatColor.UNDERLINE.toString() + "Could not initialize SkinApplier! Please report this on our discord server! ", e);
        }

        srLogger.info(ChatColor.GREEN + "Detected Minecraft " + ChatColor.YELLOW + ReflectionUtil.SERVER_VERSION_STRING + ChatColor.GREEN + ", using " + ChatColor.YELLOW + skinApplierBukkit.getRefresh().getClass().getSimpleName() + ChatColor.GREEN + ".");

        if (getServer().getPluginManager().getPlugin("ViaVersion") != null) {
            try {
                Class.forName("com.viaversion.viaversion.api.Via");
            } catch (ClassNotFoundException e) {
                getServer().getScheduler().runTaskTimerAsynchronously(this, () -> srLogger.severe("Outdated ViaVersion found! Please update to at least ViaVersion 4.0.0 for SkinsRestorer to work again!"), 50, 20L * 60);
            }
        }

        // Detect MundoSK
        if (getServer().getPluginManager().getPlugin("MundoSK") != null) {
            try {
                YamlConfig mundoConfig = new YamlConfig(dataFolderPath.getParent().resolve("MundoSK").resolve("config.yml"));
                mundoConfig.reload();
                if (mundoConfig.getBoolean("enable_custom_skin_and_tablist")) {
                    srLogger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                    srLogger.warning(ChatColor.DARK_RED + "             [CRITICAL WARNING]");
                    srLogger.warning(ChatColor.RED + "We have detected MundoSK on your server with " + ChatColor.YELLOW + "'enable_custom_skin_and_tablist: " + ChatColor.DARK_RED + ChatColor.UNDERLINE + "true" + ChatColor.YELLOW + "' " + ChatColor.RED + ".");
                    srLogger.warning(ChatColor.RED + "That setting is located in §e/plugins/MundoSK/config.yml");
                    srLogger.warning(ChatColor.RED + "You have to disable ('false') it to get SkinsRestorer to work!");
                    srLogger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                }
            } catch (Exception ignored) {
            }
        }

        // Check if we are running in bungee mode
        checkBungeeMode();

        // Check for updates
        if (!Files.exists(updaterDisabled)) {
            updateChecker = new UpdateCheckerGitHub(2124, getDescription().getVersion(), srLogger, "SkinsRestorerUpdater/Bukkit");
            updateDownloader = new UpdateDownloaderGithub(this);
            checkUpdate(bungeeEnabled, true);

            int delayInt = 60 + new Random().nextInt(240 - 60 + 1);
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> checkUpdate(bungeeEnabled, false), 20L * 60 * delayInt, 20L * 60 * delayInt);
        } else {
            srLogger.info("Updater Disabled");
        }

        // Init SkinsGUI click listener even when on bungee
        Bukkit.getPluginManager().registerEvents(new SkinsGUI(this, srLogger), this);

        if (bungeeEnabled) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:skinchange");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "sr:skinchange", (channel, player, message) -> {
                if (!channel.equals("sr:skinchange"))
                    return;

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("SkinUpdate")) {
                            try {
                                skinsRestorerAPI.applySkin(new PlayerWrapper(player), mojangAPI.createProperty(in.readUTF(), in.readUTF(), in.readUTF()));
                            } catch (IOException ignored) {
                            }

                            skinApplierBukkit.updateSkin(player);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:messagechannel");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "sr:messagechannel", (channel, channelPlayer, message) -> {
                if (!channel.equals("sr:messagechannel"))
                    return;

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("OPENGUI")) {
                            Player player = Bukkit.getPlayer(in.readUTF());
                            if (player == null)
                                return;

                            SkinsGUI.getMenus().put(player.getName(), 0);

                            requestSkinsFromBungeeCord(player, 0);
                        }

                        if (subChannel.equalsIgnoreCase("returnSkins")) {
                            Player player = Bukkit.getPlayer(in.readUTF());
                            if (player == null)
                                return;

                            int page = in.readInt();

                            short len = in.readShort();
                            byte[] msgBytes = new byte[len];
                            in.readFully(msgBytes);

                            Map<String, GenericProperty> skinList = convertToObject(msgBytes);

                            //convert
                            Map<String, IProperty> newSkinList = new TreeMap<>();

                            skinList.forEach((name, property) -> newSkinList.put(name, mojangAPI.createProperty(property.getName(), property.getValue(), property.getSignature())));

                            SkinsGUI skinsGUI = new SkinsGUI(this, srLogger);
                            ++page; // start counting from 1
                            Inventory inventory = skinsGUI.getGUI(player, page, newSkinList);

                            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> player.openInventory(inventory));
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
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        Locale.load(dataFolderPath, srLogger);

        // Init storage
        if (!initStorage())
            return;

        // Init commands
        initCommands();

        // Init listener
        if (!Config.ENABLE_PROTOCOL_LISTENER || Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);
        } else {
            srLogger.info("Hooking into ProtocolLib for instant skins on join!");
            new ProtocolLibJoinListener(this);
        }

        // Run connection check
        if (!bungeeEnabled) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                SharedMethods.runServiceCheck(mojangAPI, srLogger);
            });
        }
    }

    public void requestSkinsFromBungeeCord(Player player, int page) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("getSkins");
            out.writeUTF(player.getName());
            out.writeInt(page); // Page

            player.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinClearFromBungeeCord(Player player) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("clearSkin");
            out.writeUTF(player.getName());

            player.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinSetFromBungeeCord(Player player, String skin) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("setSkin");
            out.writeUTF(player.getName());
            out.writeUTF(skin);

            player.sendPluginMessage(this, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCommands() {
        manager = new PaperCommandManager(this);

        prepareACF(manager, srLogger);

        skinCommand = new SkinCommand(this);
        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this, new SkinsGUI(this, srLogger)));
    }

    private boolean initStorage() {
        // Initialise MySQL
        if (!SharedMethods.initMysql(srLogger, skinStorage, dataFolderPath)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        // Preload default skins
        Bukkit.getScheduler().runTaskAsynchronously(this, skinStorage::preloadDefaultSkins);
        return true;
    }

    private void checkBungeeMode() {
        bungeeEnabled = false;
        try {
            try {
                bungeeEnabled = getServer().spigot().getConfig().getBoolean("settings.bungeecord");
            } catch (NoSuchMethodError ignored) {
                srLogger.warning("It is not recommended to use non spigot implementations! Use Paper/Spigot for SkinsRestorer! ");
            }
            // sometimes it does not get the right "bungeecord: true" setting
            // we will try it again with the old function from SR 13.3
            if (!bungeeEnabled && new File("spigot.yml").exists()) {
                bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml")).getBoolean("settings.bungeecord");
            }

            //load paper velocity-support.enabled to allow velocity compatability.
            if (!bungeeEnabled && new File("paper.yml").exists()) {
                bungeeEnabled = YamlConfiguration.loadConfiguration(new File("paper.yml")).getBoolean("settings.velocity-support.enabled");
            }

            //override bungeeModeEnabled
            File bungeeModeEnabled = new File(getDataFolder(), "enableBungeeMode");
            if (!bungeeEnabled && bungeeModeEnabled.exists()) {
                bungeeEnabled = true;
                return;
            }

            //override bungeeModeDisabled
            File bungeeModeDisabled = new File(getDataFolder(), "disableBungeeMode");
            if (bungeeModeDisabled.exists()) {
                bungeeEnabled = false;
                return;
            }
        } catch (Exception ignored) {
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

        File warning = new File(getDataFolder(), "(README) Use bungee config for settings! (README)");
        try {
            if (bungeeEnabled && !warning.exists()) {
                //noinspection ResultOfMethodCallIgnored
                warning.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                warning.createNewFile();

                try (FileWriter writer = new FileWriter(warning)) {
                    writer.write(String.valueOf(sb1));
                }
            }

            if (!bungeeEnabled)
                Files.deleteIfExists(warning.toPath());
        } catch (Exception ignored) {
        }

        if (bungeeEnabled) {
            srLogger.info("-------------------------/Warning\\-------------------------");
            srLogger.info("This plugin is running in Bungee mode!");
            srLogger.info("You have to do all configuration at config file");
            srLogger.info("inside your Bungeecord server.");
            srLogger.info("(Bungeecord-Server/plugins/SkinsRestorer/).");
            srLogger.info("-------------------------\\Warning/-------------------------");
        }
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

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), bungeeMode, true, failReason).forEach(srLogger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), bungeeMode).forEach(srLogger::info);
            }
        }));
    }

    private static class WrapperFactoryBukkit extends WrapperFactory {
        @Override
        public ISRPlayer wrapPlayer(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return WrapperBukkit.wrapPlayer(player);
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private class SkinsRestorerBukkitAPI extends SkinsRestorerAPI {
        public SkinsRestorerBukkitAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactoryBukkit());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException {
            applySkin(playerWrapper, playerWrapper.get(Player.class).getName());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException {
            applySkin(playerWrapper, skinStorage.getSkinForPlayer(name));
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty props) {
            skinApplierBukkit.applySkin(playerWrapper.get(Player.class), props);
        }
    }
}
