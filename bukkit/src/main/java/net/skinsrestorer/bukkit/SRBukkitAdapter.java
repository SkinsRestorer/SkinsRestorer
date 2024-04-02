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

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bukkit.command.SRBukkitCommand;
import net.skinsrestorer.bukkit.folia.FoliaSchedulerProvider;
import net.skinsrestorer.bukkit.gui.SkinsGUI;
import net.skinsrestorer.bukkit.listener.ForceAliveListener;
import net.skinsrestorer.bukkit.paper.PaperTabCompleteEvent;
import net.skinsrestorer.bukkit.paper.PaperUtil;
import net.skinsrestorer.bukkit.spigot.SpigotConfigUtil;
import net.skinsrestorer.bukkit.utils.BukkitSchedulerProvider;
import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.commands.library.SRRegisterPayload;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.info.ClassInfo;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.listeners.event.ClickEventInfo;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.ProviderSelector;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SRBukkitAdapter implements SRServerAdapter<JavaPlugin, CommandSender> {
    private final Injector injector;
    private final Server server;
    @Getter
    private final JavaPlugin pluginInstance; // Only for platform API use
    @Getter
    private final BukkitAudiences adventure;
    @Getter
    private final SchedulerProvider schedulerProvider;
    private final SRLogger logger;

    @Inject
    public SRBukkitAdapter(Injector injector, Server server, JavaPlugin pluginInstance, BukkitAudiences adventure) {
        this.injector = injector;
        this.server = server;
        this.pluginInstance = pluginInstance;
        this.adventure = adventure;
        this.logger = injector.getSingleton(SRLogger.class);
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
    public void sendMessageToChannel(SRPlayer player, byte[] data) {
        player.getAs(Player.class).sendPluginMessage(pluginInstance, "sr:messagechannel", data);
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
        schedulerProvider.runAsync(runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
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
    public void openServerGUI(SRPlayer player, int page) {
        openGUI(player, SharedGUI.ServerGUIActions.class, page, null);
    }

    @Override
    public void openProxyGUI(SRPlayer player, int page, Map<String, String> skinList) {
        openGUI(player, SharedGUI.ProxyGUIActions.class, page, skinList);
    }

    private void openGUI(SRPlayer player, Class<? extends Consumer<ClickEventInfo>> consumer, int page, @Nullable Map<String, String> skinList) {
        Inventory inventory = injector.getSingleton(SharedGUI.class)
                .createGUI(injector.getSingleton(SkinsGUI.class), injector.getSingleton(consumer), player, page, skinList);

        runSyncToPlayer(player, () -> player.getAs(Player.class).openInventory(inventory));
    }

    @Override
    public Optional<SRPlayer> getPlayer(String name) {
        return Optional.ofNullable(server.getPlayer(name)).map(p -> injector.getSingleton(WrapperBukkit.class).player(p));
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        schedulerProvider.runRepeatAsync(runnable, delay, interval, timeUnit);
    }

    @Override
    public void extendLifeTime(JavaPlugin plugin, Object object) {
        server.getPluginManager().registerEvents(new ForceAliveListener(object), plugin);
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
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);
        return server.getOnlinePlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public SRCommandSender convertPlatformSender(CommandSender sender) {
        return injector.getSingleton(WrapperBukkit.class).commandSender(sender);
    }

    @Override
    public Class<CommandSender> getPlatformSenderClass() {
        return CommandSender.class;
    }

    @Override
    public void registerCommand(SRRegisterPayload<CommandSender> payload) {
        SettingsManager settingsManager = injector.getSingleton(SettingsManager.class);
        WrapperBukkit wrapper = injector.getSingleton(WrapperBukkit.class);

        try {
            CommandMap commandMap = (CommandMap) ReflectionUtil.invokeObjectMethod(server, "getCommandMap");
            SRBukkitCommand command = new SRBukkitCommand(payload, pluginInstance, injector.getSingleton(WrapperBukkit.class));

            if (settingsManager.getProperty(AdvancedConfig.ENABLE_PAPER_ASYNC_TAB_LISTENER)
                    && ReflectionUtil.classExists("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent")) {
                server.getPluginManager().registerEvents(
                        new PaperTabCompleteEvent(payload, wrapper::commandSender), pluginInstance);
            }

            commandMap.register(payload.meta().rootName(), "skinsrestorer", command);
        } catch (ReflectiveOperationException e) {
            logger.severe("Encountered a error while registering a command", e);
        }
    }

    @Override
    public void shutdownCleanup() {
        schedulerProvider.unregisterTasks();
    }
}
