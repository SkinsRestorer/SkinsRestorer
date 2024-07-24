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
package net.skinsrestorer.velocity;

import ch.jalu.injector.Injector;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.GameProfile;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.plugin.SRProxyAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.velocity.listener.ForceAliveListener;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;
import org.bstats.velocity.Metrics;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public record SRVelocityAdapter(Injector injector, SRVelocityBootstrap pluginInstance,
                                ProxyServer proxy) implements SRProxyAdapter {
    @Inject
    public SRVelocityAdapter {
    }

    @Override
    public Object createMetricsInstance() {
        Metrics.Factory metricsFactory = injector.getSingleton(com.google.inject.Injector.class)
                .getInstance(Metrics.Factory.class);
        return metricsFactory.make(pluginInstance, 10606);
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName).isPresent();
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager() {
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        return new VelocityCommandManager<>(
                proxy.getPluginManager().fromInstance(pluginInstance).orElseThrow(),
                proxy,
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(
                        wrapper::commandSender,
                        wrapper::unwrap
                )
        );
    }

    @Override
    public void runAsync(Runnable runnable) {
        proxy.getScheduler().buildTask(pluginInstance, runnable).schedule();
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        proxy.getScheduler().buildTask(pluginInstance, runnable).delay(delay, timeUnit).repeat(interval, timeUnit).schedule();
    }

    @Override
    public void extendLifeTime(Object plugin, Object object) {
        proxy.getEventManager().register(plugin, ProxyShutdownEvent.class, PostOrder.LAST, new ForceAliveListener(object));
    }

    @Override
    public boolean supportsDefaultPermissions() {
        return true;
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion().getVersion();
    }

    @Override
    public String getPlatformName() {
        return proxy.getVersion().getName();
    }

    @Override
    public String getPlatformVendor() {
        return proxy.getVersion().getVendor();
    }

    @Override
    public Platform getPlatform() {
        return Platform.VELOCITY;
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return proxy.getPluginManager().getPlugins().stream().map(p -> new PluginInfo(
                p.getInstance().isPresent(),
                p.getDescription().getName().orElseGet(() -> p.getDescription().getId()),
                p.getDescription().getVersion().orElse("Unknown"),
                p.getInstance().map(i -> i.getClass().getCanonicalName()).orElse("N/A"),
                p.getDescription().getAuthors().toArray(new String[0])
        )).collect(Collectors.toList());
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        List<GameProfile.Property> prop = player.getAs(Player.class).getGameProfileProperties();

        return prop.stream().filter(p -> p.getName().equals(SkinProperty.TEXTURES_NAME))
                .map(p -> SkinProperty.of(p.getValue(), p.getSignature())).findFirst();
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers() {
        WrapperVelocity wrapper = injector.getSingleton(WrapperVelocity.class);
        return proxy.getAllPlayers().stream().map(wrapper::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRProxyPlayer> getPlayer(String name) {
        return proxy.getPlayer(name).map(injector.getSingleton(WrapperVelocity.class)::player);
    }
}
