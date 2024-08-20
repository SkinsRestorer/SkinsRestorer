/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.bukkit;

import ch.jalu.injector.Injector;
import lombok.Getter;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.folia.FoliaSchedulerProvider;
import net.skinsrestorer.bukkit.gui.BukkitGUI;
import net.skinsrestorer.bukkit.listener.ForceAliveListener;
import net.skinsrestorer.bukkit.paper.PaperUtil;
import net.skinsrestorer.bukkit.spigot.SpigotConfigUtil;
import net.skinsrestorer.bukkit.utils.BukkitSchedulerProvider;
import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.info.ClassInfo;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.ProviderSelector;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import javax.inject.Inject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRBukkitAdapter implements SRServerAdapter {
    private final Injector injector;
    private final Server server;
    @Getter
    private final JavaPlugin pluginInstance; // Only for platform API use
    @Getter
    private final LazyBukkitAudiences adventure;
    @Getter
    private final SchedulerProvider schedulerProvider;

    @Inject
    public SRBukkitAdapter(Injector injector, Server server, JavaPlugin pluginInstance, LazyBukkitAudiences adventure) {
        this.injector = injector;
        this.server = server;
        this.pluginInstance = pluginInstance;
        this.adventure = adventure;
        this.schedulerProvider = ProviderSelector.<SchedulerProvider>selector()
                .add(FoliaSchedulerProvider::isAvailable,
                        () -> injector.getSingleton(FoliaSchedulerProvider.class))
                .addDefault(injector.getSingleton(BukkitSchedulerProvider.class))
                .get();
        injector.register(SchedulerProvider.class, schedulerProvider);
    }

    @Override
    public Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1669);
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager() {
        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);
        LegacyPaperCommandManager<SRCommandSender> commandManager = new LegacyPaperCommandManager<>(
                pluginInstance,
                ExecutionCoordinator.<SRCommandSender>builder()
                        .commonPoolExecutor()
                        .suggestionsExecutor(ExecutionCoordinator.nonSchedulingExecutor())
                        .build(),
                SenderMapper.create(
                        wrapper::commandSender,
                        wrapper::unwrap
                )
        );

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }

        return commandManager;
    }

    @Override
    public void runAsync(Runnable runnable) {
        schedulerProvider.runAsync(runnable);
    }

    @Override
    public void runSync(SRCommandSender sender, Runnable runnable) {
        schedulerProvider.runSync(runnable);
    }

    @Override
    public void runSyncToPlayer(SRPlayer player, Runnable runnable) {
        runSyncToPlayer(player.getAs(Player.class), runnable);
    }

    public void runSyncToPlayer(Player player, Runnable runnable) {
        schedulerProvider.runSyncToEntity(player, runnable);
    }

    @Override
    public boolean determineProxy() {
        Path spigotFile = Paths.get("spigot.yml");
        Path paperFile = Paths.get("paper.yml");

        if (SpigotConfigUtil.getSpigotConfig(server).map(config ->
                config.getBoolean("settings.bungeecord")).orElse(false)) {
            return true;
        } else if (ClassInfo.get().isSpigot() // Only consider files if classes for that platform are present
                && Files.exists(spigotFile)
                && YamlConfiguration.loadConfiguration(spigotFile.toFile())
                .getBoolean("settings.bungeecord")) {
            return true;
        } else if (PaperUtil.getPaperConfig(server).map(config ->
                config.getBoolean("settings.velocity-support.enabled")
                        || config.getBoolean("proxies.velocity.enabled")).orElse(false)) {
            return true;
        } else return ClassInfo.get().isPaper() // Only consider files if classes for that platform are present
                && Files.exists(paperFile)
                && YamlConfiguration.loadConfiguration(paperFile.toFile())
                .getBoolean("settings.velocity-support.enabled");
    }

    @Override
    public void openGUI(SRPlayer player, SRInventory srInventory) {
        Inventory inventory = injector.getSingleton(BukkitGUI.class).createGUI(srInventory);

        runSyncToPlayer(player, () -> player.getAs(Player.class).openInventory(inventory));
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        schedulerProvider.runRepeatAsync(runnable, delay, interval, timeUnit);
    }

    @Override
    public void extendLifeTime(Object plugin, Object object) {
        server.getPluginManager().registerEvents(new ForceAliveListener(object), (JavaPlugin) plugin);
    }

    @Override
    public boolean supportsDefaultPermissions() {
        return true;
    }

    @Override
    public String getPlatformVersion() {
        return server.getVersion();
    }

    @Override
    public String getPlatformName() {
        return server.getName();
    }

    @Override
    public String getPlatformVendor() {
        return server.getBukkitVersion();
    }

    @Override
    public Platform getPlatform() {
        return Platform.BUKKIT;
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return Arrays.stream(server.getPluginManager().getPlugins())
                .map(plugin -> new PluginInfo(
                        plugin.isEnabled(),
                        plugin.getName(),
                        plugin.getDescription().getVersion(),
                        plugin.getDescription().getMain(),
                        plugin.getDescription().getAuthors().toArray(new String[0])
                )).collect(Collectors.toList());
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return injector.getSingleton(SkinApplyBukkitAdapter.class).getSkinProperty(player.getAs(Player.class));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers(SRCommandSender sender) {
        return server.getOnlinePlayers().stream().map(injector.getSingleton(WrapperBukkit.class)::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRPlayer> getPlayer(SRCommandSender sender, UUID uniqueId) {
        return Optional.ofNullable(server.getPlayer(uniqueId)).map(injector.getSingleton(WrapperBukkit.class)::player);
    }

    @Override
    public void shutdownCleanup() {
        schedulerProvider.unregisterTasks();
    }
}
