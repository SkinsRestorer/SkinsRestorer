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
package net.skinsrestorer.bukkit;

import lombok.Getter;
import net.skinsrestorer.bukkit.logger.BukkitConsoleImpl;
import net.skinsrestorer.bukkit.update.UpdateDownloaderGithub;
import net.skinsrestorer.bukkit.utils.BukkitSoundProvider;
import net.skinsrestorer.bukkit.utils.PluginJarProvider;
import net.skinsrestorer.shared.commands.SoundProvider;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.update.DownloaderClassProvider;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.List;

@Getter
@SuppressWarnings("unused")
public class SRBukkitBootstrap extends JavaPlugin {
    private Runnable shutdownHook;

    @Override
    public void onEnable() {
        Server server = getServer();
        Path pluginFile = getFile().toPath();
        SRBootstrapper.startPlugin(
                runnable -> this.shutdownHook = runnable,
                List.of(
                    new SRBootstrapper.PlatformClass<>(JavaPlugin.class, this),
                    new SRBootstrapper.PlatformClass<>(Server.class, server),
                    new SRBootstrapper.PlatformClass<>(PluginJarProvider.class, () -> pluginFile),
                    new SRBootstrapper.PlatformClass<>(DownloaderClassProvider.class, () -> UpdateDownloaderGithub.class),
                    new SRBootstrapper.PlatformClass<>(SoundProvider.class, new BukkitSoundProvider())
                ),
                new JavaLoggerImpl(new BukkitConsoleImpl(server.getConsoleSender()), server.getLogger()),
                true,
                SRBukkitAdapter.class,
                SRServerPlugin.class,
                getDataFolder().toPath(),
                SRBukkitInit.class
        );
    }

    @Override
    public void onDisable() {
        shutdownHook.run();
    }
}
