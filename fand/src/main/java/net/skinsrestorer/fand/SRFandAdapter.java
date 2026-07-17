/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.fand;

import ch.jalu.injector.Injector;
import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.item.component.ItemProfile;
import io.fand.api.network.ProxyForwardingMode;
import io.fand.api.plugin.PluginContext;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.key.Key;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.fand.command.FandCommandManager;
import net.skinsrestorer.fand.gui.FandGUI;
import net.skinsrestorer.fand.wrapper.FandComponentHelper;
import net.skinsrestorer.fand.wrapper.WrapperFand;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRFandAdapter implements SRServerAdapter {
    public static final Key MESSAGE_CHANNEL = Key.key(SRHelpers.MESSAGE_CHANNEL);
    private static final List<Object> REFERENCES_TO_PREVENT_GC = new ArrayList<>();

    private final PluginContext context;
    private final Injector injector;
    private final ScheduledExecutorService asyncScheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("SkinsRestorer-Fand-Async").factory());

    @Override
    public CommandManager<SRCommandSender> createCommandManager() {
        WrapperFand wrapper = injector.getSingleton(WrapperFand.class);
        return new FandCommandManager<>(
                context.commands(),
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(wrapper::commandSender, wrapper::unwrap),
                context.logger());
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers(SRCommandSender sender) {
        WrapperFand wrapper = injector.getSingleton(WrapperFand.class);
        return Fand.server().players().stream().<SRPlayer>map(wrapper::player).toList();
    }

    @Override
    public Optional<SRPlayer> getPlayer(SRCommandSender sender, UUID uniqueId) {
        WrapperFand wrapper = injector.getSingleton(WrapperFand.class);
        return Fand.server().player(uniqueId).map(wrapper::player);
    }

    @Override
    public InputStream getResource(String resource) {
        return SRPlatformAdapter.class.getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public void runAsync(Runnable runnable) {
        asyncScheduler.execute(runnable);
    }

    @Override
    public void runAsyncDelayed(Runnable runnable, long delay, TimeUnit timeUnit) {
        asyncScheduler.schedule(runnable, delay, timeUnit);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, long delay, long interval, TimeUnit timeUnit) {
        asyncScheduler.scheduleWithFixedDelay(runnable, delay, interval, timeUnit);
    }

    @Override
    public void runSync(SRCommandSender sender, Runnable runnable) {
        context.scheduler().runMain(runnable);
    }

    @Override
    public void runSyncToPlayer(SRPlayer player, Runnable runnable) {
        context.scheduler().runMain(runnable);
    }

    @Override
    public boolean determineProxy() {
        return Fand.server().proxyForwardingMode() != ProxyForwardingMode.NONE;
    }

    @Override
    public String getPlatformVersion() {
        return Fand.server().version();
    }

    @Override
    public String getPlatformName() {
        return "Fand";
    }

    @Override
    public String getPlatformVendor() {
        return "FandMC";
    }

    @Override
    public Platform getPlatform() {
        return Platform.FAND;
    }

    @Override
    public List<PluginInfo> getPlugins() {
        var plugins = Fand.server().plugins();
        return plugins.loadedDescriptors().stream()
                .map(descriptor -> new PluginInfo(
                        plugins.isEnabled(descriptor.id()),
                        descriptor.id(),
                        descriptor.id(),
                        descriptor.version(),
                        descriptor.mainClass(),
                        Map.of("website", descriptor.website().isBlank() ? "N/A" : descriptor.website()),
                        descriptor.authors()))
                .toList();
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return player.getAs(Player.class).skin()
                .flatMap(skin -> skin.signature().map(signature -> SkinProperty.of(skin.value(), signature)));
    }

    @Override
    public Object createMetricsInstance() {
        return null;
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
        asyncScheduler.shutdownNow();
        REFERENCES_TO_PREVENT_GC.clear();
    }

    @Override
    public void openGUI(SRPlayer player, SRInventory srInventory) {
        runSyncToPlayer(player, () -> injector.getSingleton(FandGUI.class)
                .open(player.getAs(Player.class), srInventory));
    }

    @Override
    public void giveSkullItem(
            SRPlayer player,
            SRServerPluginMessage.GiveSkullChannelPayload giveSkullPayload
    ) {
        runSyncToPlayer(player, () -> {
            var type = Fand.server().itemType(Key.key("minecraft:player_head"))
                    .orElseThrow(() -> new IllegalStateException("Fand does not expose minecraft:player_head"));
            var property = ItemProfile.Property.unsigned(
                    SkinProperty.TEXTURES_NAME,
                    SRHelpers.encodeHashToTexturesValue(giveSkullPayload.textureHash()));
            ItemStack stack = new ItemStack(type, 1)
                    .withProfile(new ItemProfile(null, null, List.of(property), null, null, null, null))
                    .withCustomName(FandComponentHelper.deserialize(giveSkullPayload.displayName()));
            player.getAs(Player.class).inventory().add(stack);
        });
    }

    @Override
    public Class<? extends SoundProvider> getSoundProviderClass() {
        return FandSoundProvider.class;
    }

    public void sendPluginMessage(Player player, byte[] data) {
        context.pluginMessaging().send(player, MESSAGE_CHANNEL, data);
    }

    SRPlayer getPlayerForScheduling(Player player) {
        return injector.getSingleton(WrapperFand.class).player(player);
    }
}
