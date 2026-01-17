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
package net.skinsrestorer.mod;

import ch.jalu.injector.Injector;
import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.mod.gui.ModGUI;
import net.skinsrestorer.mod.utils.SoundUtil;
import net.skinsrestorer.mod.wrapper.WrapperMod;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.SRHelpers;
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
        return SRPlatformAdapter.class.getClassLoader().getResourceAsStream(resource);
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
        asyncScheduler.schedule(runnable, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runAsyncDelayed(Runnable runnable, long delay, TimeUnit timeUnit) {
        asyncScheduler.schedule(runnable, delay, timeUnit);
    }

    @Override
    public void runSync(SRCommandSender sender, Runnable runnable) {
        runSync(sender.getAs(CommandSourceStack.class).getServer(), runnable);
    }

    public void runSync(MinecraftServer server, Runnable runnable) {
        server.schedule(server.wrapRunnable(runnable));
    }

    @Override
    public void runSyncToPlayer(SRPlayer player, Runnable runnable) {
        runSync(player, runnable);
    }

    @Override
    public boolean determineProxy() {
        return Set.of(
                "fabricproxy",
                "fabricproxy-lite",
                "neoforwarding",
                "neovelocity"
        ).stream().anyMatch(mod -> getPluginInfo(mod).isPresent());
    }

    @Override
    public void openGUI(SRPlayer player, SRInventory srInventory) {
        MenuProvider inventory = injector.getSingleton(ModGUI.class).createGUI(srInventory);

        runSyncToPlayer(player, () -> openMenuCustom(player.getAs(ServerPlayer.class), inventory));
    }

    // Custom open menu method that does not close the current menu
    // This presents the cursor from resetting when opening a new menu
    private void openMenuCustom(ServerPlayer player, MenuProvider menuProvider) {
        player.nextContainerCounter();
        AbstractContainerMenu abstractContainerMenu = menuProvider.createMenu(player.containerCounter, player.getInventory(), player);
        if (abstractContainerMenu == null) {
            return;
        }

        player.connection.send(new ClientboundOpenScreenPacket(abstractContainerMenu.containerId, abstractContainerMenu.getType(), menuProvider.getDisplayName()));
        player.initMenu(abstractContainerMenu);
        player.containerMenu = abstractContainerMenu;
    }

    @Override
    public void giveSkullItem(SRPlayer player, SRServerPluginMessage.GiveSkullChannelPayload giveSkullPayload) {
        final ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
        builder.put(SkinProperty.TEXTURES_NAME, new Property(
                SkinProperty.TEXTURES_NAME,
                SRHelpers.encodeHashToTexturesValue(giveSkullPayload.textureHash()),
                null
        ));

        PropertyMap propertyMap = new PropertyMap(builder.build());
        GameProfile gameProfile = new GameProfile(Util.NIL_UUID, "", propertyMap);

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.applyComponents(DataComponentPatch.builder()
                .set(DataComponents.PROFILE, ResolvableProfile.createResolved(gameProfile))
                .build());

        player.getAs(ServerPlayer.class).getInventory().add(stack);
    }

    @Override
    public Class<? extends SoundProvider> getSoundProviderClass() {
        return SoundUtil.class;
    }

    @Override
    public void runRepeatAsync(Runnable runnable, long delay, long interval, TimeUnit timeUnit) {
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
                        plugin.getModId(),
                        plugin.getName(),
                        plugin.getVersion(),
                        "N/A",
                        Map.of(
                                "homepage", plugin.getHomepage().orElse("N/A"),
                                "sources", plugin.getSources().orElse("N/A"),
                                "issueTracker", plugin.getIssueTracker().orElse("N/A")
                        ),
                        List.copyOf(plugin.getAuthors())
                )).collect(Collectors.toList());
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return player.getAs(ServerPlayer.class).getGameProfile().properties().values().stream()
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
