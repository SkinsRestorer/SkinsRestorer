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
import co.aikar.locales.LocaleManager;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.api.serverinfo.ServerVersion;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.commands.SkinCommand;
import net.skinsrestorer.bukkit.commands.SkullCommand;
import net.skinsrestorer.bukkit.commands.SrCommand;
import net.skinsrestorer.bukkit.listener.InventoryListener;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.listener.PlayerResourcePackStatus;
import net.skinsrestorer.bukkit.utils.*;
import net.skinsrestorer.paper.PaperPlayerJoinEvent;
import net.skinsrestorer.paper.PaperUtil;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.ISRForeign;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.*;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.LocaleParser;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.spigot.SpigotUtil;
import net.skinsrestorer.v1_7.BukkitLegacyProperty;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static net.skinsrestorer.bukkit.utils.WrapperBukkit.wrapPlayer;

@Getter
@SuppressWarnings("Duplicates")
public class SkinsRestorer extends JavaPlugin implements ISRPlugin {
    private final MetricsCounter metricsCounter = new MetricsCounter();
    private final CooldownStorage cooldownStorage = new CooldownStorage();
    @SuppressWarnings("ConstantConditions")
    private final BukkitConsoleImpl bukkitConsole = new BukkitConsoleImpl(getServer() == null ? null : getServer().getConsoleSender());
    @SuppressWarnings("ConstantConditions")
    private final JavaLoggerImpl javaLogger = new JavaLoggerImpl(bukkitConsole, getServer() == null ? null : getServer().getLogger());
    private final SRLogger srLogger = new SRLogger(javaLogger, true);
    private final MojangAPI mojangAPI = new MojangAPI(srLogger, metricsCounter);
    private final MineSkinAPI mineSkinAPI = new MineSkinAPI(srLogger, metricsCounter);
    private final SkinStorage skinStorage = new SkinStorage(srLogger, mojangAPI, mineSkinAPI);
    private final UpdateChecker updateChecker = new UpdateCheckerGitHub(2124, getVersion(), srLogger, "SkinsRestorerUpdater/Bukkit");
    private final SkinsRestorerAPI skinsRestorerAPI = new SkinsRestorerBukkitAPI();
    private final UpdateDownloaderGithub updateDownloader = new UpdateDownloaderGithub(this);
    private final SkinCommand skinCommand = new SkinCommand(this);
    private LocaleManager<ISRForeign> localeManager;
    private Path dataFolderPath;
    private SkinApplierBukkit skinApplierBukkit;
    private boolean proxyMode;
    private boolean updateDownloaded = false;
    private PaperCommandManager manager;
    private boolean isUpdaterInitialized = false;

    @SuppressWarnings("unchecked")
    private static Map<String, GenericProperty> convertToObject(byte[] byteArr) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
            ObjectInputStream ois = new ObjectInputStream(bis);

            return (Map<String, GenericProperty>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> convertToObjectV2(byte[] byteArr) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(byteArr)));

            return (Map<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public String getVersion() {
        return getDescription() == null ? null : getDescription().getVersion();
    }

    @Override
    public void runAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public void runRepeat(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        getServer().getScheduler().runTaskTimerAsynchronously(this, runnable, timeUnit.toSeconds(delay) * 20L, timeUnit.toSeconds(interval) * 20L);
    }

    @Override
    public Collection<ISRPlayer> getOnlinePlayers() {
        return getServer().getOnlinePlayers().stream().map(WrapperBukkit::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public void onEnable() {
        bukkitConsole.setConsoleCommandSender(getServer().getConsoleSender());
        javaLogger.setLogger(getServer().getLogger());
        dataFolderPath = getDataFolder().toPath();
        updateChecker.setCurrentVersion(getVersion());
        srLogger.load(dataFolderPath);

        Exception startupError = null;
        try {
            pluginStartup();
        } catch (Exception e) {
            startupError = e;
        } finally {
            if (!isUpdaterInitialized) {
                updateCheck();
            }
        }

        if (startupError != null) {
            srLogger.debug("An error occurred while starting the plugin.", startupError);
        }
    }

    public void pluginStartup() throws InitializeException {
        Metrics metrics = new Metrics(this, 1669);
        metrics.addCustomChart(new SingleLineChart("mineskin_calls", metricsCounter::collectMineskinCalls));
        metrics.addCustomChart(new SingleLineChart("minetools_calls", metricsCounter::collectMinetoolsCalls));
        metrics.addCustomChart(new SingleLineChart("mojang_calls", metricsCounter::collectMojangCalls));
        metrics.addCustomChart(new SingleLineChart("ashcon_calls", metricsCounter::collectAshconCalls));

        try {
            skinApplierBukkit = new SkinApplierBukkit(this, srLogger);
        } catch (NoMappingException e) {
            srLogger.severe("Your Minecraft version is not supported by this version of SkinsRestorer! Is there a newer version available? If not, join our discord server!", e);
            throw e;
        } catch (InitializeException e) {
            srLogger.severe(ChatColor.RED + ChatColor.UNDERLINE.toString() + "Could not initialize SkinApplier! Please report this on our discord server!");
            throw e;
        }

        srLogger.info(ChatColor.GREEN + "Detected Minecraft " + ChatColor.YELLOW + ReflectionUtil.SERVER_VERSION_STRING + ChatColor.GREEN + ", using " + ChatColor.YELLOW + skinApplierBukkit.getRefresh().getClass().getSimpleName() + ChatColor.GREEN + ".");

        if (ReflectionUtil.SERVER_VERSION != null && !ReflectionUtil.SERVER_VERSION.isNewer(new ServerVersion(1, 7, 10))) {
            srLogger.warning(ChatColor.YELLOW + "Although SkinsRestorer allows using this ancient version, we will not provide full support for it. This version of Minecraft does not allow using all of SkinsRestorers features due to client side restrictions. Please be aware things WILL BREAK and not work!");
        }

        if (getServer().getPluginManager().getPlugin("ViaVersion") != null) {
            if (!ReflectionUtil.classExists("com.viaversion.viaversion.api.Via")) {
                getServer().getScheduler().runTaskTimerAsynchronously(this, () -> srLogger.severe("Outdated ViaVersion found! Please update to at least ViaVersion 4.0.0 for SkinsRestorer to work again!"), 50, 20L * 60);
            }
        }

        // Detect MundoSK
        if (getServer().getPluginManager().getPlugin("MundoSK") != null) {
            try {
                YamlConfig mundoConfig = new YamlConfig(dataFolderPath.getParent().resolve("MundoSK").resolve("config.yml"));
                mundoConfig.load();
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

        // Check if we are running in proxy mode
        checkProxyMode();

        updateCheck();

        // Init locale
        localeManager = LocaleManager.create(ISRForeign::getLocale, SkinsRestorerAPIShared.getApi().getDefaultForeign().getLocale());
        Message.load(localeManager, dataFolderPath, this);

        // Init SkinsGUI click listener even when in ProxyMode
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);

        if (proxyMode) {
            if (Files.exists(dataFolderPath.resolve("enableSkinStorageAPI.txt"))) {
                initConfigAndStorage();
            }

            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "sr:skinchange");
            Bukkit.getMessenger().registerIncomingPluginChannel(this, "sr:skinchange", (channel, player, message) -> {
                if (!channel.equals("sr:skinchange"))
                    return;

                runAsync(() -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("SkinUpdate")) {
                            try {
                                skinsRestorerAPI.applySkin(new PlayerWrapper(player), SkinsRestorerAPI.getApi().createPlatformProperty(in.readUTF(), in.readUTF(), in.readUTF()));
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

                            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> player.openInventory(inventory));
                        } else if (subChannel.equalsIgnoreCase("GiveSkull")) {
                            Player targetPlayer = Bukkit.getPlayer(in.readUTF());
                            if (targetPlayer == null)
                                return;

                            String lore = in.readUTF();
                            OfflinePlayer skullOwner = Bukkit.getOfflinePlayer(in.readUTF()); // todo fix depricated
                            String b64stringTexture = in.readUTF();

                            SkinSkull.giveSkull(this, targetPlayer, lore, skullOwner, b64stringTexture);
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
                srLogger.info("Using paper join listener!");
                Bukkit.getPluginManager().registerEvents(new PaperPlayerJoinEvent(this), this);
            } else {
                Bukkit.getPluginManager().registerEvents(new PlayerJoin(this), this);

                if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerResourcePackStatusEvent")) {
                    Bukkit.getPluginManager().registerEvents(new PlayerResourcePackStatus(this), this);
                }
            }

            // Run connection check
            runAsync(() -> SharedMethods.runServiceCheck(mojangAPI, srLogger));
        }
    }

    private void initConfigAndStorage() throws InitializeException {
        // Init config files
        Config.load(dataFolderPath, getResource("config.yml"), srLogger);
        // Set new default locale because we initialized localeManager early
        localeManager.setDefaultLocale(LocaleParser.getDefaultLocale());

        // Init storage
        initStorage();
    }

    private void updateCheck() {
        // Check for updates
        isUpdaterInitialized = true;
        checkUpdateInit(() -> {
            checkUpdate(true);

            // Delay update between 5 & 30 minutes
            int delayInt = 300 + ThreadLocalRandom.current().nextInt(1800 + 1 - 300);
            // Repeat update between 1 & 4 hours
            int periodInt = 60 * (60 + ThreadLocalRandom.current().nextInt(240 + 1 - 60));
            runRepeat(this::checkUpdate, delayInt, periodInt, TimeUnit.SECONDS);
        });
    }

    public void requestSkinsFromProxy(Player player, int page) {
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

    public void requestSkinClearFromProxy(Player player) {
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

    public void requestSkinSetFromProxy(Player player, String skin) {
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

        runRepeat(cooldownStorage::cleanup, 60, 60, TimeUnit.SECONDS);

        manager.registerCommand(skinCommand); // fixme
        manager.registerCommand(new SrCommand(this));
        manager.registerCommand(new GUICommand(this));
        manager.registerCommand(new SkullCommand(this, new SkinSkull(this, srLogger)));
    }

    private void checkProxyMode() {
        proxyMode = false;
        try {
            if (PaperLib.isSpigot()) {
                proxyMode = SpigotUtil.getSpigotConfig(getServer()).getBoolean("settings.bungeecord");
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

                YamlConfiguration config = PaperUtil.getPaperConfig(getServer());

                if (config != null) {
                    if (!proxyMode && (config.getBoolean("settings.velocity-support.enabled")
                            || config.getBoolean("proxies.velocity.enabled"))) {
                        proxyMode = true;
                    }
                }
            }

            Path bungeeModeEnabled = dataFolderPath.resolve("enableBungeeMode"); // Legacy
            Path bungeeModeDisabled = dataFolderPath.resolve("disableBungeeMode"); // Legacy

            Path proxyModeEnabled = dataFolderPath.resolve("enableProxyMode.txt");
            Path proxyModeDisabled = dataFolderPath.resolve("disableProxyMode.txt");

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
            Path warning = dataFolderPath.resolve("(README) Use proxy config for settings! (README).txt");
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
            srLogger.info("-------------------------/Warning\\-------------------------");
            srLogger.info("This plugin is running in PROXY mode!");
            srLogger.info("You have to do all configuration at config file");
            srLogger.info("inside your BungeeCord/Velocity server.");
            srLogger.info("(<proxy>/plugins/SkinsRestorer/)");
            srLogger.info("-------------------------\\Warning/-------------------------");
        }
    }

    public void checkUpdate(boolean showUpToDate) {
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
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

                updateChecker.getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, getVersion(), proxyMode, true, failReason).forEach(srLogger::info);
            }

            @Override
            public void upToDate() {
                if (!showUpToDate)
                    return;

                updateChecker.getUpToDateMessages(getVersion(), proxyMode).forEach(srLogger::info);
            }
        }));
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
        public SkinsRestorerBukkitAPI() {
            super(mojangAPI, mineSkinAPI, skinStorage, new WrapperFactoryBukkit(), new PropertyFactoryBukkit());
        }

        @Override
        public void applySkin(PlayerWrapper playerWrapper, IProperty property) {
            skinApplierBukkit.applySkin(playerWrapper.get(Player.class), property);
        }

        @Override
        protected LocaleManager<ISRForeign> getLocaleManager() {
            return localeManager;
        }
    }
}
