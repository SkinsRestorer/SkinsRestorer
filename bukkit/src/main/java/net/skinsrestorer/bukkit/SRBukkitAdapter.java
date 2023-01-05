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
import lombok.Getter;
import lombok.val;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.utils.IOExceptionConsumer;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.acf.OnlineISRPlayer;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.interfaces.SRServerAdapter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRBukkitAdapter implements SRServerAdapter {
    private final Injector injector;
    private final Server server;
    @Getter
    private final JavaPlugin pluginInstance; // Only for platform API use

    public SRBukkitAdapter(Injector injector, Server server, JavaPlugin pluginInstance) {
        this.injector = injector;
        this.server = server;
        this.pluginInstance = pluginInstance;

        injector.register(SRBukkitAdapter.class, this);
        injector.register(SRServerAdapter.class, this);
        injector.register(Server.class, server);
    }

    @Override
    public Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1669);
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
    public CommandManager<?, ?, ?, ?, ?, ?> createCommandManager() {
        PaperCommandManager manager = new PaperCommandManager(pluginInstance);

        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);

        val playerResolver = manager.getCommandContexts().getResolver(Player.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRPlayer.class, c -> {
            Object playerObject = playerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return wrapper.player((Player) playerObject);
        });

        val commandSenderResolver = manager.getCommandContexts().getResolver(CommandSender.class);
        manager.getCommandContexts().registerIssuerAwareContext(SRCommandSender.class, c -> {
            Object commandSenderObject = commandSenderResolver.getContext(c);
            if (commandSenderObject == null) {
                return null;
            }
            return wrapper.commandSender((CommandSender) commandSenderObject);
        });


        val onlinePlayerResolver = manager.getCommandContexts().getResolver(OnlinePlayer.class);
        manager.getCommandContexts().registerContext(OnlineISRPlayer.class, c -> {
            Object playerObject = onlinePlayerResolver.getContext(c);
            if (playerObject == null) {
                return null;
            }
            return new OnlineISRPlayer(wrapper.player(((OnlinePlayer) playerObject).getPlayer()));
        });

        return manager;
    }

    @Override
    public SRCommandSender convertCommandSender(Object sender) {
        return injector.getSingleton(WrapperBukkit.class).commandSender((CommandSender) sender);
    }

    @Override
    public void reloadPlatformHook() {
        injector.getSingleton(SkinApplierBukkit.class).setOptFileChecked(false);
    }

    @Override
    public String getPlatformVersion() {
        return server.getVersion();
    }

    @Override
    public List<SkinProperty> getPropertiesOfPlayer(SRPlayer player) {
        Map<String, Collection<SkinProperty>> propertyMap = injector.getSingleton(SkinApplierBukkit.class).getPlayerProperties(player.getAs(Player.class));
        return new ArrayList<>(propertyMap.get(SkinProperty.TEXTURES_NAME));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);
        return server.getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }
}
