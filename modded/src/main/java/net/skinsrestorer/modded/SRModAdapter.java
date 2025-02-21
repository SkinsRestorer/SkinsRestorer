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
package net.skinsrestorer.modded;

import ch.jalu.injector.Injector;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.modded.gui.ModGUI;
import net.skinsrestorer.modded.wrapper.WrapperMod;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SRModAdapter implements SRServerAdapter {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final List<Object> REFERENCES_TO_PREVENT_GC = new ArrayList<>();
    private final Injector injector;
    private final ScheduledExecutorService asyncScheduler = Executors.newSingleThreadScheduledExecutor();

    @Inject
    public SRModAdapter(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Object createMetricsInstance() {
        return null;
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager() {
        WrapperMod wrapper = injector.getSingleton(WrapperMod.class);
        return SRModPlatform.INSTANCE.createCommandManager(
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(
                        wrapper::commandSender,
                        wrapper::unwrap
                ));
    }

    @Override
    public void runAsync(Runnable runnable) {
        asyncScheduler.execute(runnable);
    }

    @Override
    public void runSync(SRCommandSender sender, Runnable runnable) {
        runSync(sender.getAs(CommandSourceStack.class).getServer(), runnable);
    }

    public void runSync(MinecraftServer server, Runnable runnable) {
        server.execute(runnable);
    }

    @Override
    public void runSyncToPlayer(SRPlayer player, Runnable runnable) {
        runSync(player, runnable);
    }

    @Override
    public boolean determineProxy() {
        return false;
    }

    @Override
    public void openGUI(SRPlayer player, SRInventory srInventory) {
        MenuProvider inventory = injector.getSingleton(ModGUI.class).createGUI(srInventory);

        runSyncToPlayer(player, () -> MenuRegistry.openMenu(player.getAs(ServerPlayer.class), inventory));
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        asyncScheduler.scheduleWithFixedDelay(runnable, delay, interval, timeUnit);
    }

    @Override
    public void extendLifeTime(Object plugin, Object object) {
        REFERENCES_TO_PREVENT_GC.add(object);
    }

    @Override
    public boolean supportsDefaultPermissions() {
        return true;
    }

    @Override
    public void shutdownCleanup() {
        asyncScheduler.shutdown();
        REFERENCES_TO_PREVENT_GC.clear();
    }

    @Override
    public String getPlatformVersion() {
        return dev.architectury.platform.Platform.getMinecraftVersion();
    }

    @Override
    public String getPlatformName() {
        return SRModPlatform.INSTANCE.getPlatformName();
    }

    @Override
    public String getPlatformVendor() {
        return "N/A";
    }

    @Override
    public Platform getPlatform() {
        return Platform.BUKKIT;
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return dev.architectury.platform.Platform.getMods().stream()
                .map(plugin -> new PluginInfo(
                        true,
                        plugin.getName(),
                        plugin.getVersion(),
                        "N/A",
                        plugin.getAuthors().toArray(new String[0])
                )).collect(Collectors.toList());
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return player.getAs(ServerPlayer.class).getGameProfile().getProperties().values().stream()
                .map(property -> SkinProperty.tryParse(
                        property.name(),
                        property.value(),
                        property.signature()
                ))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers(SRCommandSender sender) {
        return sender.getAs(CommandSourceStack.class).getServer().getPlayerList().getPlayers().stream().map(injector.getSingleton(WrapperMod.class)::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRPlayer> getPlayer(SRCommandSender sender, UUID uniqueId) {
        return Optional.ofNullable(sender.getAs(CommandSourceStack.class).getServer().getPlayerList().getPlayer(uniqueId)).map(injector.getSingleton(WrapperMod.class)::player);
    }
}
