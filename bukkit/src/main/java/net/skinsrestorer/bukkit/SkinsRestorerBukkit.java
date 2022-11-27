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

import co.aikar.commands.CommandManager;
import co.aikar.commands.PaperCommandManager;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.serverinfo.ServerVersion;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.commands.SkinCommand;
import net.skinsrestorer.bukkit.commands.SrCommand;
import net.skinsrestorer.bukkit.listener.InventoryListener;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.listener.PlayerResourcePackStatus;
import net.skinsrestorer.bukkit.utils.*;
import net.skinsrestorer.paper.PaperPlayerJoinEvent;
import net.skinsrestorer.paper.PaperUtil;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerServerShared;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.YamlConfig;
import net.skinsrestorer.shared.utils.LocaleParser;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.spigot.SpigotUtil;
import net.skinsrestorer.v1_7.BukkitLegacyProperty;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.skinsrestorer.bukkit.utils.WrapperBukkit.wrapPlayer;

@Getter
public class SkinsRestorerBukkit extends SkinsRestorerServerShared {
    private final Server server;
    private final JavaPlugin pluginInstance; // Only for platform API use
    private final SkinCommand skinCommand = new SkinCommand(this);
    private final UpdateDownloaderGithub updateDownloader = new UpdateDownloaderGithub(this);
    private boolean isUpdaterInitialized = false;
    private SkinApplierBukkit skinApplierBukkit;
    private boolean updateDownloaded = false;

    public SkinsRestorerBukkit(JavaPlugin plugin) {
        super(
                new JavaLoggerImpl(new BukkitConsoleImpl(plugin.getServer().getConsoleSender()), plugin.getServer().getLogger()),
                true,
                plugin.getDescription().getVersion(),
                "SkinsRestorerUpdater/Bukkit",
                plugin.getDataFolder().toPath()
        );
        this.server = plugin.getServer();
        this.pluginInstance = plugin;
        registerAPI();
    }

    public void pluginStartup() throws InitializeException {
        logger.load(dataFolder);

        Metrics metrics = new Metrics(pluginInstance, 1669);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        try {
            skinApplierBukkit = new SkinApplierBukkit(this);
        } catch (NoMappingException e) {
            logger.severe("Your Minecraft version is not supported by this version of SkinsRestorer! Is there a newer version available? If not, join our discord server!", e);
            throw e;
        } catch (InitializeException e) {
            logger.severe(ChatColor.RED + ChatColor.UNDERLINE.toString() + "Could not initialize SkinApplier! Please report this on our discord server!");
            throw e;
        }

        logger.info(ChatColor.GREEN + "Detected Minecraft " + ChatColor.YELLOW + ReflectionUtil.SERVER_VERSION_STRING + ChatColor.GREEN + ", using " + ChatColor.YELLOW + skinApplierBukkit.getRefresh().getClass().getSimpleName() + ChatColor.GREEN + ".");

        if (ReflectionUtil.SERVER_VERSION != null && !ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 7, 10))) {
            logger.warning(ChatColor.YELLOW + "Although SkinsRestorer allows using this ancient version, we will not provide full support for it. This version of Minecraft does not allow using all of SkinsRestorers features due to client side restrictions. Please be aware things WILL BREAK and not work!");
        }

        if (server.getPluginManager().getPlugin("ViaVersion") != null) {
            if (!ReflectionUtil.classExists("com.viaversion.viaversion.api.Via")) {
                server.getScheduler().runTaskTimerAsynchronously(pluginInstance, () -> logger.severe("Outdated ViaVersion found! Please update to at least ViaVersion 4.0.0 for SkinsRestorer to work again!"), 50, 20L * 60);
            }
        }

        // Detect MundoSK
        if (server.getPluginManager().getPlugin("MundoSK") != null) {
            try {
                YamlConfig mundoConfig = new YamlConfig(dataFolder.getParent().resolve("MundoSK").resolve("config.yml"));
                mundoConfig.load();
                if (mundoConfig.getBoolean("enable_custom_skin_and_tablist")) {
                    logger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                    logger.warning(ChatColor.DARK_RED + "             [CRITICAL WARNING]");
                    logger.warning(ChatColor.RED + "We have detected MundoSK on your server with " + ChatColor.YELLOW + "'enable_custom_skin_and_tablist: " + ChatColor.DARK_RED + ChatColor.UNDERLINE + "true" + ChatColor.YELLOW + "' " + ChatColor.RED + ".");
                    logger.warning(ChatColor.RED + "That setting is located in §e/plugins/MundoSK/config.yml");
                    logger.warning(ChatColor.RED + "You have to disable ('false') it to get SkinsRestorer to work!");
                    logger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                }
            } catch (Exception ignored) {
            }
        }

        // Check if we are running in proxy mode
        checkProxyMode();

        updateCheck();

        // Init locale
        Message.load(localeManager, dataFolder, this);

        // Init SkinsGUI click listener even when in ProxyMode
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), pluginInstance);

        if (proxyMode) {
            if (Files.exists(dataFolder.resolve("enableSkinStorageAPI.txt"))) {
                initConfigAndStorage();
            }

            Bukkit.getMessenger().registerOutgoingPluginChannel(pluginInstance, "sr:skinchange");
            Bukkit.getMessenger().registerIncomingPluginChannel(pluginInstance, "sr:skinchange", (channel, player, message) -> {
                if (!channel.equals("sr:skinchange"))
                    return;

                runAsync(() -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("SkinUpdate")) {
                            try {
                                SkinsRestorerAPIShared.getApi().applySkin(new PlayerWrapper(player), SkinsRestorerAPI.getApi().createPlatformProperty(in.readUTF(), in.readUTF(), in.readUTF()));
                            } catch (IOException ignored) {
                            }

                            skinApplierBukkit.updateSkin(player);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            Bukkit.getMessenger().registerOutgoingPluginChannel(pluginInstance, "sr:messagechannel");
            Bukkit.getMessenger().registerIncomingPluginChannel(pluginInstance, "sr:messagechannel", (channel, channelPlayer, message) -> {
                if (!channel.equals("sr:messagechannel"))
                    return;

                runAsync(() -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("OPENGUI")) { // LEGACY
                            Player player = Bukkit.getPlayer(in.readUTF());
                            if (player == null)
                                return;

                            requestSkinsFromProxy(player, 0);
                        } else if (subChannel.equalsIgnoreCase("returnSkins") || subChannel.equalsIgnoreCase("returnSkinsV2")) {
                            Player player = Bukkit.getPlayer(in.readUTF());
                            if (player == null)
                                return;

                            int page = in.readInt();

                            short len = in.readShort();
                            byte[] msgBytes = new byte[len];
                            in.readFully(msgBytes);

                            Map<String, String> skinList;
                            if (subChannel.equalsIgnoreCase("returnSkinsV2")) {
                                skinList = convertToObjectV2(msgBytes);
                            } else { // LEGACY
                                skinList = new TreeMap<>();

                                convertToObject(msgBytes).forEach((key, value) -> {
                                    skinList.put(key, value.getValue());
                                });
                            }

                            Inventory inventory = SkinsGUI.createGUI(this, wrapPlayer(player), page, skinList);

                            runSync(() -> player.openInventory(inventory));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } else {
            initConfigAndStorage();

            // Init commands
            initCommands();

            // Init listener
            if (Config.ENABLE_PAPER_JOIN_LISTENER
                    && ReflectionUtil.classExists("com.destroystokyo.paper.event.profile.PreFillProfileEvent")) {
                logger.info("Using paper join listener!");
                Bukkit.getPluginManager().registerEvents(new PaperPlayerJoinEvent(this), pluginInstance);
            } else {
                Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), pluginInstance);

                if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerResourcePackStatusEvent")) {
                    Bukkit.getPluginManager().registerEvents(new PlayerResourcePackStatus(this), pluginInstance);
                }
            }

            // Run connection check
            runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, logger));
        }
    }

    private void initConfigAndStorage() throws InitializeException {
        // Init config files
        Config.load(dataFolder, getResource("config.yml"), logger);
        // Set new default locale because we initialized localeManager early
        localeManager.setDefaultLocale(LocaleParser.getDefaultLocale());

        // Init storage
        initStorage();
    }

    protected void updateCheck() {
        // Check for updates
        isUpdaterInitialized = true;
        checkUpdateInit(() -> {
            checkUpdate(true);

            // Delay update between 5 & 30 minutes
            int delayInt = 300 + ThreadLocalRandom.current().nextInt(1800 + 1 - 300);
            // Repeat update between 1 & 4 hours
            int periodInt = 60 * (60 + ThreadLocalRandom.current().nextInt(240 + 1 - 60));
            runRepeatAsync(this::checkUpdate, delayInt, periodInt, TimeUnit.SECONDS);
        });
    }

    public void requestSkinsFromProxy(Player player, int page) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("getSkins");
            out.writeUTF(player.getName());
            out.writeInt(page); // Page

            player.sendPluginMessage(pluginInstance, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinClearFromProxy(Player player) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("clearSkin");
            out.writeUTF(player.getName());

            player.sendPluginMessage(pluginInstance, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestSkinSetFromProxy(Player player, String skin) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeUTF("setSkin");
            out.writeUTF(player.getName());
            out.writeUTF(skin);

            player.sendPluginMessage(pluginInstance, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCommands() {
        sharedInitCommands();

        manager.registerCommand(skinCommand);
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
    }

    private void checkProxyMode() {
        proxyMode = false;
        try {
            if (PaperLib.isSpigot()) {
                proxyMode = SpigotUtil.getSpigotConfig(server).getBoolean("settings.bungeecord");
            }
            // sometimes it does not get the right "bungeecord: true" setting
            // we will try it again with the old function from SR 13.3
            Path spigotFile = Paths.get("spigot.yml");
            if (!proxyMode && Files.exists(spigotFile)) {
                proxyMode = YamlConfiguration.loadConfiguration(spigotFile.toFile()).getBoolean("settings.bungeecord");
            }

            if (PaperLib.isPaper()) {
                //load paper velocity-support.enabled to allow velocity compatability.
                Path oldPaperFile = Paths.get("paper.yml");
                if (!proxyMode && Files.exists(oldPaperFile)) {
                    proxyMode = YamlConfiguration.loadConfiguration(oldPaperFile.toFile()).getBoolean("settings.velocity-support.enabled");
                }

                YamlConfiguration config = PaperUtil.getPaperConfig(server);

                if (config != null) {
                    if (!proxyMode && (config.getBoolean("settings.velocity-support.enabled")
                            || config.getBoolean("proxies.velocity.enabled"))) {
                        proxyMode = true;
                    }
                }
            }

            Path bungeeModeEnabled = dataFolder.resolve("enableBungeeMode"); // Legacy
            Path bungeeModeDisabled = dataFolder.resolve("disableBungeeMode"); // Legacy

            Path proxyModeEnabled = dataFolder.resolve("enableProxyMode.txt");
            Path proxyModeDisabled = dataFolder.resolve("disableProxyMode.txt");

            if (Files.exists(bungeeModeEnabled)) {
                Files.move(bungeeModeEnabled, proxyModeEnabled, StandardCopyOption.REPLACE_EXISTING);
            } else if (Files.exists(bungeeModeDisabled)) {
                Files.move(bungeeModeDisabled, proxyModeDisabled, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!proxyMode && Files.exists(proxyModeEnabled)) {
                proxyMode = true;
                return;
            }

            if (Files.exists(proxyModeDisabled)) {
                proxyMode = false;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Path warning = dataFolder.resolve("(README) Use proxy config for settings! (README).txt");
            if (proxyMode) {
                Files.createDirectories(warning.getParent());

                try (InputStream in = getResource("proxy_warning.txt")) {
                    if (in == null) {
                        throw new IllegalStateException("Could not find proxy_warning.txt in resources!");
                    }
                    // Always replace the file to make sure it's up-to-date.
                    Files.copy(in, warning, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Files.deleteIfExists(warning);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (proxyMode) {
            logger.info("-------------------------/Warning\\-------------------------");
            logger.info("This plugin is running in PROXY mode!");
            logger.info("You have to do all configuration at config file");
            logger.info("inside your BungeeCord/Velocity server.");
            logger.info("(<proxy>/plugins/SkinsRestorer/)");
            logger.info("-------------------------\\Warning/-------------------------");
        }
    }

    @Override
    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                outdated = true;

                if (updateDownloaded) {
                    return;
                }

                String failReason = null;
                if (hasDirectDownload) {
                    if (updateDownloader.downloadUpdate()) {
                        updateDownloaded = true;
                    } else {
                        failReason = updateDownloader.getFailReason().toString();
                    }
                }

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, version, proxyMode, true, failReason).forEach(logger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(version, proxyMode).forEach(logger::info);
            }
        }));
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return server.getPluginManager().getPlugin(pluginName) != null;
    }

    @Override
    public InputStream getResource(String resource) {
        return pluginInstance.getResource(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        server.getScheduler().runTaskAsynchronously(pluginInstance, runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        server.getScheduler().runTask(pluginInstance, runnable);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        server.getScheduler().runTaskTimerAsynchronously(pluginInstance, runnable, timeUnit.toSeconds(delay) * 20L, timeUnit.toSeconds(interval) * 20L);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return server.getOnlinePlayers().stream().map(WrapperBukkit::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        return new PaperCommandManager(pluginInstance);
    }

    @Override
    protected void registerAPI() {
        new SkinsRestorerBukkitAPI();
    }

    private static class WrapperFactoryBukkit implements IWrapperFactory {
        @Override
        public String getPlayerName(Object playerInstance) {
            if (playerInstance instanceof Player) {
                Player player = (Player) playerInstance;

                return player.getName();
            } else {
                throw new IllegalArgumentException("Player instance is not valid!");
            }
        }
    }

    private static class PropertyFactoryBukkit implements IPropertyFactory {
        @Override
        public IProperty createProperty(String name, String value, String signature) {
            if (ReflectionUtil.classExists("com.mojang.authlib.properties.Property")) {
                return new BukkitProperty(name, value, signature);
            } else {
                return new BukkitLegacyProperty(name, value, signature);
            }
        }
    }

    private class SkinsRestorerBukkitAPI extends SkinsRestorerAPIShared {
        private SkinsRestorerBukkitAPI() {
            super(SkinsRestorerBukkit.this, new WrapperFactoryBukkit(), new PropertyFactoryBukkit());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierBukkit.applySkin(playerWrapper.get(Player.class), property);
        }
    }
}
