/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.platform.SRBootstrapper;
import net.skinsrestorer.shared.platform.SRProxyPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.SharedUpdateCheck;
import net.skinsrestorer.shared.utils.log.Slf4jLoggerImpl;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"knat", "AlexProgrammerDE", "Blackfire62", "McLive"})
public class SRVelocityBootstrap {
    @Inject
    private ProxyServer proxy;
    @Inject
    private Metrics.Factory metricsFactory;
    @Inject
    @DataDirectory
    private Path dataFolder;
    @Inject
    private Logger logger;
    @Inject
    private PluginContainer container;

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        SRBootstrapper.startPlugin(
                new Slf4jLoggerImpl(logger),
                false,
                i -> new SRVelocityAdapter(i, this, proxy, metricsFactory),
                SharedUpdateCheck.class,
                SRProxyPlugin.class,
                container.getDescription().getVersion().orElse("Unknown"),
                dataFolder,
                Platform.VELOCITY,
                SRVelocityStarter.class
        );
    }
}
