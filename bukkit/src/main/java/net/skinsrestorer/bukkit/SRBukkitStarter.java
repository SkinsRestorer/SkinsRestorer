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
import ch.jalu.injector.Injector;
import co.aikar.commands.CommandManager;
import io.papermc.lib.PaperLib;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.axiom.AxiomConfiguration;
import net.skinsrestorer.bukkit.commands.GUICommand;
import net.skinsrestorer.bukkit.listener.InventoryListener;
import net.skinsrestorer.bukkit.listener.PlayerJoin;
import net.skinsrestorer.bukkit.listener.PlayerResourcePackStatus;
import net.skinsrestorer.bukkit.skinrefresher.NOOPRefresher;
import net.skinsrestorer.bukkit.utils.NoMappingException;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.paper.PaperPlayerJoinEvent;
import net.skinsrestorer.paper.PaperUtil;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRPlatformStarter;
import net.skinsrestorer.shared.platform.SRPlugin;
import net.skinsrestorer.shared.platform.SRServerPlugin;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.serverinfo.ServerVersion;
import net.skinsrestorer.shared.utils.SharedMethods;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.spigot.SpigotUtil;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBukkitStarter implements SRPlatformStarter {
    private final SRServerPlugin serverPlugin;
    private final SRPlugin plugin;
    private final SRBukkitAdapter adapter;
    private final Injector injector;
    private final SRLogger logger;
    private final Server server;

    @Override
    public void pluginStartup() throws InitializeException {
        plugin.startupStart();

        // Init config files // TODO: Split config files
        plugin.loadConfig();

        SkinApplierBukkit skinApplierBukkit;
        try {
            skinApplierBukkit = injector.getSingleton(SkinApplierBukkit.class);
            if (SRPlugin.isUnitTest()) {
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
                server.getScheduler().runTaskTimerAsynchronously(adapter.getPluginInstance(), () -> logger.severe("Outdated ViaVersion found! Please update to at least ViaVersion 4.0.0 for SkinsRestorer to work again!"), 50, 20L * 60);
            }
        }

        // Detect MundoSK
        if (server.getPluginManager().getPlugin("MundoSK") != null) {
            try {
                try (BufferedReader reader = Files.newBufferedReader(plugin.getDataFolder().getParent().resolve("MundoSK").resolve("config.yml"))) {
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

        plugin.initUpdateCheck();

        // Init locale
        plugin.loadLocales();

        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);

        // Init SkinsGUI click listener even when in ProxyMode
        server.getPluginManager().registerEvents(new InventoryListener(), adapter.getPluginInstance());

        if (serverPlugin.isProxyMode()) {
            if (Files.exists(plugin.getDataFolder().resolve("enableSkinStorageAPI.txt"))) {
                plugin.initMineSkinAPI();
                plugin.initStorage();
                plugin.registerSkinApplier(skinApplierBukkit, Player.class, Player::getName);
                plugin.registerAPI();
            }

            server.getMessenger().registerOutgoingPluginChannel(adapter.getPluginInstance(), "sr:skinchange");
            server.getMessenger().registerIncomingPluginChannel(adapter.getPluginInstance(), "sr:skinchange", (channel, player, message) -> adapter.runAsync(() -> {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

                try {
                    String subChannel = in.readUTF();

                    if (subChannel.equalsIgnoreCase("SkinUpdateV2")) {
                        try {
                            skinApplierBukkit.applySkin(player, SkinProperty.of(in.readUTF(), in.readUTF()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        skinApplierBukkit.updateSkin(player);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            server.getMessenger().registerOutgoingPluginChannel(adapter.getPluginInstance(), "sr:messagechannel");
            server.getMessenger().registerIncomingPluginChannel(adapter.getPluginInstance(), "sr:messagechannel", (channel, channelPlayer, message) -> adapter.runAsync(() -> {
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

                        Map<String, String> skinList = SRServerPlugin.convertToObjectV2(msgBytes);

                        Inventory inventory = SkinsGUI.createGUI(injector.getSingleton(SkinsGUI.ProxyGUIActions.class),
                                injector.getSingleton(SkinsRestorerLocale.class),
                                logger, server, wrapper.player(player), page, skinList);

                        adapter.runSync(() -> player.openInventory(inventory));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } else {
            plugin.initMineSkinAPI();
            plugin.initStorage();
            plugin.registerSkinApplier(skinApplierBukkit, Player.class, Player::getName);

            // Init API
            plugin.registerAPI();

            // Init commands
            CommandManager<?, ?, ?, ?, ?, ?> manager = plugin.sharedInitCommands();

            manager.registerCommand(injector.newInstance(GUICommand.class));

            // Init listener
            if (injector.getSingleton(SettingsManager.class).getProperty(AdvancedConfig.ENABLE_PAPER_JOIN_LISTENER)
                    && ReflectionUtil.classExists("com.destroystokyo.paper.event.profile.PreFillProfileEvent")) {
                logger.info("Using paper join listener!");
                server.getPluginManager().registerEvents(injector.newInstance(PaperPlayerJoinEvent.class), adapter.getPluginInstance());
            } else {
                server.getPluginManager().registerEvents(injector.newInstance(PlayerJoin.class), adapter.getPluginInstance());

                if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerResourcePackStatusEvent")) {
                    server.getPluginManager().registerEvents(injector.newInstance(PlayerResourcePackStatus.class), adapter.getPluginInstance());
                }
            }

            // Run connection check
            adapter.runAsync(() -> SharedMethods.runServiceCheck(injector.getSingleton(MojangAPIImpl.class), logger));
        }
    }

    private void checkProxyMode() {
        serverPlugin.setProxyMode(false);
        try {
            if (PaperLib.isSpigot() && SpigotUtil.isRealSpigot(server)) {
                serverPlugin.setProxyMode(SpigotUtil.getSpigotConfig(server).getBoolean("settings.bungeecord"));
            }
            // sometimes it does not get the right "bungeecord: true" setting
            // we will try it again with the old function from SR 13.3
            Path spigotFile = Paths.get("spigot.yml");
            if (!serverPlugin.isProxyMode() && Files.exists(spigotFile)) {
                serverPlugin.setProxyMode(YamlConfiguration.loadConfiguration(spigotFile.toFile()).getBoolean("settings.bungeecord"));
            }

            if (PaperLib.isPaper()) {
                //load paper velocity-support.enabled to allow velocity compatability.
                Path oldPaperFile = Paths.get("paper.yml");
                if (!serverPlugin.isProxyMode() && Files.exists(oldPaperFile)) {
                    serverPlugin.setProxyMode(YamlConfiguration.loadConfiguration(oldPaperFile.toFile()).getBoolean("settings.velocity-support.enabled"));
                }

                YamlConfiguration config = PaperUtil.getPaperConfig(server);

                if (config != null) {
                    if (!serverPlugin.isProxyMode() && (config.getBoolean("settings.velocity-support.enabled")
                            || config.getBoolean("proxies.velocity.enabled"))) {
                        serverPlugin.setProxyMode(true);
                    }
                }
            }
            Path proxyModeEnabled = plugin.getDataFolder().resolve("enableProxyMode.txt");
            Path proxyModeDisabled = plugin.getDataFolder().resolve("disableProxyMode.txt");

            if (!serverPlugin.isProxyMode() && Files.exists(proxyModeEnabled)) {
                serverPlugin.setProxyMode(true);
                return;
            }

            if (Files.exists(proxyModeDisabled)) {
                serverPlugin.setProxyMode(false);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Path warning = plugin.getDataFolder().resolve("(README) Use proxy config for settings! (README).txt");
            if (serverPlugin.isProxyMode()) {
                Files.createDirectories(plugin.getDataFolder());

                try (InputStream in = adapter.getResource("proxy_warning.txt")) {
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

        if (serverPlugin.isProxyMode()) {
            logger.info("-------------------------/Warning\\-------------------------");
            logger.info("This plugin is running in PROXY mode!");
            logger.info("You have to do all configuration at config file");
            logger.info("inside your BungeeCord/Velocity server.");
            logger.info("(<proxy>/plugins/SkinsRestorer/)");
            logger.info("-------------------------\\Warning/-------------------------");
        }
    }
}
