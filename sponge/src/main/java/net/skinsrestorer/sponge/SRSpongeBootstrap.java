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
package net.skinsrestorer.sponge;

import com.google.inject.Inject;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.update.SharedUpdateCheck;
import net.skinsrestorer.sponge.utils.Log4jLoggerImpl;
import org.apache.logging.log4j.Logger;
import org.bstats.sponge.Metrics;
import org.spongepowered.api.Game;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;

@SuppressWarnings("unused")
@Plugin("skinsrestorer")
public class SRSpongeBootstrap {
    @Inject
    private PluginContainer container;
    @ConfigDir(sharedRoot = false)
    @Inject
    private Path dataFolder;
    @Inject
    @SuppressWarnings("SpongeInjection")
    private Metrics.Factory metricsFactory;
    @Inject
    private Logger logger;
    @Inject
    private Game game;

    @Listener
    public void onInitialize(ConstructPluginEvent event) {
        // Need to init metrics before plugin because metrics wants to hook in the lifecycle before the plugin
        Metrics metrics = metricsFactory.make(2337);
        metrics.startup(event);

        SRBootstrapper.startPlugin(
                new Log4jLoggerImpl(logger),
                false,
                i -> new SRSpongeAdapter(i, metrics, container, game),
                SharedUpdateCheck.class,
                SRServerPlugin.class,
                container.metadata().version().toString(),
                dataFolder,
                Platform.SPONGE,
                SRSpongeInit.class
        );
    }
}
