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
package net.skinsrestorer.bungee;

import ch.jalu.injector.Injector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.bungee.listeners.ForceAliveListener;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.plugin.SRProxyAdapter;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import org.bstats.bungeecord.Metrics;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bungee.BungeeCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBungeeAdapter implements SRProxyAdapter {
    private final Injector injector;
    private final ProxyServer proxy;
    @Getter
    private final Plugin pluginInstance; // Only for platform API use
    @Getter
    private final LazyBungeeAudiences adventure;

    @Override
    public Object createMetricsInstance() {
        return new Metrics(pluginInstance, 1686);
    }

    @Override
    public InputStream getResource(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    @Override
    public CommandManager<SRCommandSender> createCommandManager() {
        WrapperBungee wrapper = injector.getSingleton(WrapperBungee.class);
        return new BungeeCommandManager<>(
                pluginInstance,
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(
                        wrapper::commandSender,
                        wrapper::unwrap
                )
        );
    }

    @Override
    public void runAsync(Runnable runnable) {
        proxy.getScheduler().runAsync(pluginInstance, runnable);
    }

    @Override
    public void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit) {
        proxy.getScheduler().schedule(pluginInstance, runnable, delay, interval, timeUnit);
    }

    @Override
    public void extendLifeTime(Object plugin, Object object) {
        proxy.getPluginManager().registerListener((Plugin) plugin, new ForceAliveListener(object));
    }

    @Override
    public boolean supportsDefaultPermissions() {
        return false;
    }

    @Override
    public String getPlatformVersion() {
        return proxy.getVersion();
    }

    @Override
    public String getPlatformName() {
        return proxy.getName();
    }

    @Override
    public String getPlatformVendor() {
        return "N/A";
    }

    @Override
    public Platform getPlatform() {
        return Platform.BUNGEE_CORD;
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return proxy.getPluginManager().getPlugins().stream()
                .map(p -> new PluginInfo(
                        true,
                        p.getDescription().getName(),
                        p.getDescription().getVersion(),
                        p.getDescription().getMain(),
                        new String[]{p.getDescription().getAuthor()}
                )).collect(Collectors.toList());
    }

    @Override
    public Optional<SkinProperty> getSkinProperty(SRPlayer player) {
        return SkinApplierBungee.getApplyAdapter().getSkinProperty(player.getAs(ProxiedPlayer.class));
    }

    @Override
    public Collection<SRPlayer> getOnlinePlayers(SRCommandSender sender) {
        return proxy.getPlayers().stream().map(injector.getSingleton(WrapperBungee.class)::player).collect(Collectors.toList());
    }

    @Override
    public Optional<SRPlayer> getPlayer(SRCommandSender sender, UUID uniqueId) {
        return Optional.ofNullable(proxy.getPlayer(uniqueId)).map(injector.getSingleton(WrapperBungee.class)::player);
    }
}
