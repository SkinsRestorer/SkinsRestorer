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
import net.skinsrestorer.builddata.BuildData;
import org.bstats.sponge.Metrics;
import org.slf4j.Logger;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.nio.file.Path;

@Plugin(id = "skinsrestorer", name = "SkinsRestorer", version = BuildData.VERSION, description = BuildData.DESCRIPTION, url = BuildData.URL, authors = {"knat", "AlexProgrammerDE", "Blackfire62", "McLive"})
public class SkinsRestorerBootstrap {
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

    @Listener
    public void onInitialize(GameInitializationEvent event) {
        new SkinsRestorerSponge(this, metricsFactory, dataFolder, logger, container).pluginStartup();
    }
}
