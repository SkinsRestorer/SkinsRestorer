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

import ch.jalu.injector.Injector;
import co.aikar.commands.CommandManager;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.gui.SkinsGUI;
import net.skinsrestorer.bukkit.listener.ForceAliveListener;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.bukkit.paper.PaperUtil;
import net.skinsrestorer.shared.acf.OnlineSRPlayer;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.IOExceptionConsumer;
import net.skinsrestorer.bukkit.spigot.SpigotUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRBukkitAdapter implements SRServerAdapter<JavaPlugin> {
    private final Injector injector;
    private final Server server;
    @Getter
    private final Path pluginFile; // Only for platform API use
    @Getter
    private final JavaPlugin pluginInstance; // Only for platform API use

    public SRBukkitAdapter(Injector injector, Path pluginFile, JavaPlugin pluginInstance) {
        this.injector = injector;
        this.server = injector.getSingleton(Server.class);
        this.pluginFile = pluginFile;
        this.pluginInstance = pluginInstance;

        injector.register(SRBukkitAdapter.class, this);
        injector.register(SRServerAdapter.class, this);
    }

    @Override
    public Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1669);
    }

    @Override
    public void sendToMessageChannel(SRPlayer player, IOExceptionConsumer<DataOutputStream> consumer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);

            consumer.accept(out);

            player.getAs(Player.class).sendPluginMessage(pluginInstance, "sr:messagechannel", bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    public boolean determineProxy() {
        if (PaperLib.isSpigot() && SpigotUtil.isRealSpigot(server)) {
            if (SpigotUtil.getSpigotConfig(server).getBoolean("settings.bungeecord")) {
                return true;
            }
        }

        // sometimes it does not get the right "bungeecord: true" setting
        // we will try it again with the old function from SR 13.3
        Path spigotFile = Paths.get("spigot.yml");
        if (Files.exists(spigotFile)) {
            if (YamlConfiguration.loadConfiguration(spigotFile.toFile()).getBoolean("settings.bungeecord")) {
                return true;
            }
        }

        if (PaperLib.isPaper()) {
            // Load paper velocity-support.enabled to allow velocity compatability.
            Path oldPaperFile = Paths.get("paper.yml");
            if (Files.exists(oldPaperFile)) {
                if (YamlConfiguration.loadConfiguration(oldPaperFile.toFile()).getBoolean("settings.velocity-support.enabled")) {
                    return true;
                }
            }

            YamlConfiguration config = PaperUtil.getPaperConfig(server);

            return config != null
                    && (config.getBoolean("settings.velocity-support.enabled") || config.getBoolean("proxies.velocity.enabled"));
        }

        return false;
    }

    @Override
    public void openServerGUI(SRPlayer player, int page) {
        Inventory inventory = injector.getSingleton(SharedGUI.class)
                .createGUI(injector.getSingleton(SkinsGUI.class), injector.getSingleton(SharedGUI.ServerGUIActions.class), player, page);

        runSync(() -> player.getAs(Player.class).openInventory(inventory));
    }

    @Override
    public void openProxyGUI(SRPlayer player, int page, Map<String, String> skinList) {
        Inventory inventory = injector.getSingleton(SkinsGUI.class)
                .createGUI(injector.getSingleton(SharedGUI.ProxyGUIActions.class), player, page, skinList);

        runSync(() -> player.getAs(Player.class).openInventory(inventory));
    }

    @Override
    public Optional<SRPlayer> getPlayer(String name) {
        return Optional.ofNullable(server.getPlayer(name)).map(p -> injector.getSingleton(WrapperBukkit.class).player(p));
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        server.getScheduler().runTaskTimerAsynchronously(pluginInstance, runnable, timeUnit.toSeconds(delay) * 20L, timeUnit.toSeconds(interval) * 20L);
    }

    @Override
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        PaperCommandManager manager = new PaperCommandManager(pluginInstance);

        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            Player player = (Player) playerResolver.getContext(c);
            if (player == null) {
                return null;
            }
            return wrapper.player(player);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSender.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            CommandSender commandSender = (CommandSender) commandSenderResolver.getContext(c);
            if (commandSender == null) {
                return null;
            }
            return wrapper.commandSender(commandSender);
        });

        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineSRPlayer.class, c -> {
            OnlinePlayer onlinePlayer = (OnlinePlayer) onlinePlayerResolver.getContext(c);
            if (onlinePlayer == null) {
                return null;
            }
            return new OnlineSRPlayer(wrapper.player(onlinePlayer.getPlayer()));
        });

        return manager;
    }

    @Override
    public SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperBukkit.class).commandSender((CommandSender) sender);
    }

    @Override
    public void extendLifeTime(JavaPlugin plugin, Object object) {
        server.getPluginManager().registerEvents(new ForceAliveListener(object), plugin);
    }

    @Override
    public String getPlatformVersion() {
        return server.getVersion();
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return SkinApplierBukkit.getSkinProperty(player.getAs(Player.class));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);
        return server.getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }
}
