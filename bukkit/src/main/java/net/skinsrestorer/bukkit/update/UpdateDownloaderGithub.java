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
package net.skinsrestorer.bukkit.update;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.shared.exception.UpdateException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.update.DownloadCallback;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parts taken from <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateDownloaderGithub {
    private final SRPlugin plugin;
    private final SRBukkitAdapter adapter;
    private final SRLogger logger;
    private final Server server;

    private Runnable downloadAsync(String downloadUrl, Path file, DownloadCallback callback) {
        return () -> {
            try {
                download(downloadUrl, file);
                callback.finished();
            } catch (Exception e) {
                callback.error(e);
            }
        };
    }

    private void download(String downloadUrl, Path file) throws UpdateException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            connection.setRequestProperty("User-Agent", plugin.getUserAgent());
            if (connection.getResponseCode() != 200) {
                throw new UpdateException("Download returned status code " + connection.getResponseCode());
            }

            ReadableByteChannel channel = Channels.newChannel(connection.getInputStream());

            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                output.flush();
            }
        } catch (IOException e) {
            throw new UpdateException("Download failed", e);
        }
    }

    public boolean downloadUpdate(String downloadUrl) {
        Path pluginFile = getPluginFile(adapter.getPluginInstance()); // /plugins/XXX.jar
        Path updateFolder = server.getUpdateFolderFile().toPath();
        try {
            Files.createDirectories(updateFolder);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Path updateFile = updateFolder.resolve(pluginFile.getFileName()); // /plugins/update/XXX.jar

        logger.info("[GitHubUpdate] Downloading update...");
        adapter.runAsync(downloadAsync(downloadUrl, updateFile, new DownloadCallback() {
            @Override
            public void finished() {
                logger.info(String.format("[GitHubUpdate] Update saved as %s", updateFile.getFileName()));
                logger.info("[GitHubUpdate] The update will be loaded on the next server restart");
            }

            @Override
            public void error(Exception exception) {
                logger.warning("[GitHubUpdate] Could not download update", exception);
            }
        }));

        return true;
    }

    private Path getPluginFile(JavaPlugin plugin) {
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            return ((File) method.invoke(plugin)).toPath();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not get plugin file", e);
        }
    }
}
