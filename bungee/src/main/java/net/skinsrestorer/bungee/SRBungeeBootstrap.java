/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.bungee;

import lombok.Getter;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.skinsrestorer.bungee.logger.BungeeConsoleImpl;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRProxyPlugin;
import net.skinsrestorer.shared.update.SharedUpdateCheckInit;

@SuppressWarnings("unused")
public class SRBungeeBootstrap extends Plugin {
    @Getter
    private BungeeAudiences adventure;

    @Override
    public void onEnable() {
        this.adventure = BungeeAudiences.create(this);

        ProxyServer proxy = getProxy();
        SRBootstrapper.startPlugin(
                injector -> {
                    injector.register(Plugin.class, this);
                    injector.register(ProxyServer.class, proxy);
                    injector.register(BungeeAudiences.class, this.adventure);
                },
                new JavaLoggerImpl(new BungeeConsoleImpl(proxy.getConsole()), proxy.getLogger()),
                true,
                SRBungeeAdapter.class,
                SharedUpdateCheckInit.class,
                SRProxyPlugin.class,
                getDescription().getVersion(),
                getDataFolder().toPath(),
                SRBungeeInit.class
        );
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
}
