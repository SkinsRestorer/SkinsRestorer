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

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.CommandManager;
import co.aikar.commands.PaperCommandManager;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.interfaces.IPropertyFactory;
import net.skinsrestorer.api.interfaces.IWrapperFactory;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.serverinfo.ServerVersion;
import net.skinsrestorer.axiom.AxiomConfiguration;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.commands.SkinCommand;
import net.skinsrestorer.bukkit.commands.SrCommand;
import net.skinsrestorer.bukkit.listener.InventoryListener;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.listener.PlayerResourcePackStatus;
import net.skinsrestorer.bukkit.skinrefresher.NOOPRefresher;
import net.skinsrestorer.bukkit.utils.*;
import net.skinsrestorer.paper.PaperPlayerJoinEvent;
import net.skinsrestorer.paper.PaperUtil;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerServerShared;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.JavaLoggerImpl;
import net.skinsrestorer.spigot.SpigotUtil;
import net.skinsrestorer.v1_7.BukkitLegacyProperty;
import org.bstats.bukkit.Metrics;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public class SkinsRestorerBukkit extends SkinsRestorerServerShared {
    private final Server server;
    private final JavaPlugin pluginInstance; // Only for platform API use
    private boolean isUpdaterInitialized = false;
    private boolean updateDownloaded = false;

    public SkinsRestorerBukkit(Server server, String version, Path dataFolder, JavaPlugin pluginInstance) {
        super(
                new JavaLoggerImpl(new BukkitConsoleImpl(server.getConsoleSender()), server.getLogger()),
                true,
                version,
                "SkinsRestorerUpdater/Bukkit",
                dataFolder,
                new WrapperFactoryBukkit(),
                new PropertyFactoryBukkit()
        );
        injector.register(SkinsRestorerBukkit.class, this);
        injector.register(Server.class, server);
        this.server = server;
        this.pluginInstance = pluginInstance;
    }

    public void pluginStartup() throws InitializeException {
        startupStart();

        // Init config files // TODO: Split config files
        loadConfig();

        SkinApplierBukkit skinApplierBukkit;
        try {
            skinApplierBukkit = injector.getSingleton(SkinApplierBukkit.class);
            if (unitTest) {
                skinApplierBukkit.setRefresh(new NOOPRefresher());
            } else {
                skinApplierBukkit.setRefresh(skinApplierBukkit.detectRefresh(server));
            }
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
                try (BufferedReader reader = Files.newBufferedReader(dataFolder.getParent().resolve("MundoSK").resolve("config.yml"))) {
                    AxiomConfiguration config = new AxiomConfiguration();
                    config.load(reader);

                    if (config.getBoolean("enable_custom_skin_and_tablist")) {
                        logger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                        logger.warning(ChatColor.DARK_RED + "             [CRITICAL WARNING]");
                        logger.warning(ChatColor.RED + "We have detected MundoSK on your server with " + ChatColor.YELLOW + "'enable_custom_skin_and_tablist: " + ChatColor.DARK_RED + ChatColor.UNDERLINE + "true" + ChatColor.YELLOW + "' " + ChatColor.RED + ".");
                        logger.warning(ChatColor.RED + "That setting is located in §e/plugins/MundoSK/config.yml");
                        logger.warning(ChatColor.RED + "You have to disable ('false') it to get SkinsRestorer to work!");
                        logger.warning(ChatColor.DARK_RED + "----------------------------------------------");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Check if we are running in proxy mode
        checkProxyMode();

        updateCheck();

        // Init locale
        loadLocales();

        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);

        injector.provide(OnlinePlayersMethod.class, (CallableValue<Collection<ISRPlayer>>)
                () -> server.getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList()));

        // Init SkinsGUI click listener even when in ProxyMode
        server.getPluginManager().registerEvents(new InventoryListener(), pluginInstance);

        if (proxyMode) {
            if (Files.exists(dataFolder.resolve("enableSkinStorageAPI.txt"))) {
                initMineSkinAPI();
                initStorage();
                registerAPI(skinApplierBukkit);
            }

            server.getMessenger().registerOutgoingPluginChannel(pluginInstance, "sr:skinchange");
            server.getMessenger().registerIncomingPluginChannel(pluginInstance, "sr:skinchange", (channel, player, message) -> {
                if (!channel.equals("sr:skinchange"))
                    return;

                runAsync(() -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("SkinUpdate")) {
                            try {
                                skinApplierBukkit.applySkin(new PlayerWrapper(player), SkinsRestorerAPI.getApi().createPlatformProperty(in.readUTF(), in.readUTF(), in.readUTF()));
                            } catch (IOException ignored) {
                            }

                            skinApplierBukkit.updateSkin(player);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            server.getMessenger().registerOutgoingPluginChannel(pluginInstance, "sr:messagechannel");
            server.getMessenger().registerIncomingPluginChannel(pluginInstance, "sr:messagechannel", (channel, channelPlayer, message) -> {
                if (!channel.equals("sr:messagechannel"))
                    return;

                runAsync(() -> {
                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                    try {
                        String subChannel = in.readUTF();

                        if (subChannel.equalsIgnoreCase("returnSkinsV2")) {
                            Player player = server.getPlayer(in.readUTF());
                            if (player == null)
                                return;

                            int page = in.readInt();

                            short len = in.readShort();
                            byte[] msgBytes = new byte[len];
                            in.readFully(msgBytes);

                            Map<String, String> skinList = convertToObjectV2(msgBytes);

                            Inventory inventory = SkinsGUI.createGUI(new SkinsGUI.ProxyGUIActions(this),
                                    injector.getSingleton(SkinsRestorerLocale.class),
                                    logger, server, wrapper.player(player), page, skinList);

                            runSync(() -> player.openInventory(inventory));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } else {
            initMineSkinAPI();
            initStorage();

            // Init API
            registerAPI(skinApplierBukkit);

            // Init commands
            CommandManager<?, ?, ?, ?, ?, ?> manager = sharedInitCommands();

            manager.registerCommand(injector.getSingleton(SkinCommand.class));
            manager.registerCommand(injector.newInstance(SrCommand.class));
            manager.registerCommand(injector.newInstance(GUICommand.class));

            // Init listener
            if (injector.getSingleton(SettingsManager.class).getProperty(Config.ENABLE_PAPER_JOIN_LISTENER)
                    && ReflectionUtil.classExists("com.destroystokyo.paper.event.profile.PreFillProfileEvent")) {
                logger.info("Using paper join listener!");
                server.getPluginManager().registerEvents(injector.newInstance(PaperPlayerJoinEvent.class), pluginInstance);
            } else {
                server.getPluginManager().registerEvents(injector.newInstance(PlayerJoin.class), pluginInstance);

                if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerResourcePackStatusEvent")) {
                    server.getPluginManager().registerEvents(injector.newInstance(PlayerResourcePackStatus.class), pluginInstance);
                }
            }

            // Run connection check
            runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPI.class), logger));
        }
    }

    @Override
    protected Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1669);
    }

    @Override
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
        sendToMessageChannel(player, out -> {
            out.writeUTF("getSkins");
            out.writeUTF(player.getName());
            out.writeInt(page);
        });
    }

    public void sendToMessageChannel(Player player, IOExceptionConsumer<DataOutputStream> consumer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            consumer.accept(out);

            player.sendPluginMessage(pluginInstance, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        if (unitTest) {
            if (showUpToDate) {
                updateChecker.getUpToDateMessages(version, proxyMode).forEach(logger::info);
            }
            return;
        }

        UpdateDownloaderGithub updateDownloader = new UpdateDownloaderGithub(this, updateChecker, logger, server);
        runAsync(() -> updateChecker.checkForUpdate(new UpdateCallback() {
            @Override
            public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                setOutdated();
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
        return getClass().getClassLoader().getResourceAsStream(resource);
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
    protected CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        return new PaperCommandManager(pluginInstance);
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
}
