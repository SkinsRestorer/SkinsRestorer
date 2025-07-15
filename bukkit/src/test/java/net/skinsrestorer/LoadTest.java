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
package net.skinsrestorer;

import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.bukkit.SRBukkitInit;
import net.skinsrestorer.bukkit.logger.BukkitConsoleImpl;
import net.skinsrestorer.bukkit.update.UpdateDownloaderGithub;
import net.skinsrestorer.bukkit.utils.PluginJarProvider;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.update.DownloaderClassProvider;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SRExtension.class, SRBukkitExtension.class})
public class LoadTest {
    @TempDir
    private Path tempDir;

    @Test
    public void testLoad() {
        Path pluginFile = tempDir.resolve("SkinsRestorer.jar");
        Path configDir = tempDir.resolve("config");

        Queue<Runnable> runQueue = new ConcurrentLinkedQueue<>();
        ServerMock server = mock(ServerMock.class);
        Logger logger = Logger.getLogger("TestSkinsRestorer");
        ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            System.out.println(arg0);
            return null;
        }).when(sender).sendMessage(anyString());

        when(server.getLogger()).thenReturn(logger);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        doAnswer(invocation -> {
            runQueue.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTaskAsynchronously(any(), any(Runnable.class));

        /*
        doAnswer(invocation -> {
            runQueue.add(invocation.getArgument(1));
            return null;
        }).when(scheduler).runTask(any(), any(Runnable.class));
        */

        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getBukkitVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
        when(server.getVersion()).thenReturn("1.19.2-R0.1-SNAPSHOT");
        when(server.getName()).thenReturn("TestServer");
        when(server.getCommandMap()).thenReturn(new SimpleCommandMap(server));
        SimplePluginManager pluginManager = mock(SimplePluginManager.class);
        when(pluginManager.getPlugins()).thenReturn(new JavaPlugin[0]);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getUpdateFolderFile()).thenReturn(tempDir.toFile());

        Bukkit.setServer(server);

        JavaPluginMock plugin = mock(JavaPluginMock.class);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getName()).thenReturn("SkinsRestorer");

        SRBootstrapper.startPlugin(
                runnable -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                List.of(
                        new SRBootstrapper.PlatformClass<>(JavaPlugin.class, plugin),
                        new SRBootstrapper.PlatformClass<>(Server.class, server),
                        new SRBootstrapper.PlatformClass<>(PluginJarProvider.class, () -> pluginFile),
                        new SRBootstrapper.PlatformClass<>(DownloaderClassProvider.class, () -> UpdateDownloaderGithub.class)
                ),
                new JavaLoggerImpl(new BukkitConsoleImpl(sender), logger),
                true,
                SRBukkitAdapter.class,
                SRServerPlugin.class,
                configDir,
                SRBukkitInit.class
        );

        while (!runQueue.isEmpty()) {
            try {
                runQueue.poll().run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public abstract static class ServerMock implements Server {
        abstract CommandMap getCommandMap();
    }

    public abstract static class JavaPluginMock extends JavaPlugin {
    }
}
