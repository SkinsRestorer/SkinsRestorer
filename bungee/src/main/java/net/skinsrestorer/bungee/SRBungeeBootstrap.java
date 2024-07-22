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

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRProxyPlugin;

import java.util.List;

@Getter
@SuppressWarnings("unused")
public class SRBungeeBootstrap extends Plugin {
    private Runnable shutdownHook;

    @Override
    public void onEnable() {
        ProxyServer proxy = getProxy();
        SRBootstrapper.startPlugin(
                runnable -> this.shutdownHook = runnable,
                List.of(
                        new SRBootstrapper.PlatformClass<>(Plugin.class, this),
                        new SRBootstrapper.PlatformClass<>(ProxyServer.class, proxy)
                ),
                new JavaLoggerImpl(proxy.getLogger()::info, proxy.getLogger()),
                true,
                SRBungeeAdapter.class,
                SRProxyPlugin.class,
                getDataFolder().toPath(),
                SRBungeeInit.class
        );
    }

    @Override
    public void onDisable() {
        shutdownHook.run();
    }
}
