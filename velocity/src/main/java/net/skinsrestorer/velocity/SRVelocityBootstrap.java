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
package net.skinsrestorer.velocity;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRProxyPlugin;
import net.skinsrestorer.velocity.logger.Slf4jLoggerImpl;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "skinsrestorer", name = "SkinsRestorer",
        version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL,
        authors = {"knat", "AlexProgrammerDE", "Blackfire62", "McLive"},
        dependencies = {@Dependency(id = "floodgate", optional = true)})
public class SRVelocityBootstrap {
    @Inject
    private ProxyServer proxy;
    @Inject
    private Injector guiceInjector;
    @Inject
    @DataDirectory
    private Path dataFolder;
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        SRBootstrapper.startPlugin(
                runnable -> {
                },
                injector -> {
                    injector.register(ProxyServer.class, proxy);
                    injector.register(Injector.class, guiceInjector);
                    injector.register(SRVelocityBootstrap.class, this);
                },
                new Slf4jLoggerImpl(logger),
                false,
                SRVelocityAdapter.class,
                SRProxyPlugin.class,
                dataFolder,
                SRVelocityInit.class
        );
    }
}
