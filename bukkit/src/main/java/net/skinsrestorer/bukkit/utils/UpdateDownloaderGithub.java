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
package net.skinsrestorer.bukkit.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.shared.exception.UpdateException;
import net.skinsrestorer.shared.update.DownloadCallback;
import net.skinsrestorer.shared.update.GitHubReleaseInfo;
import net.skinsrestorer.shared.update.UpdateCheckerGitHub;
import net.skinsrestorer.shared.utils.log.SRLogger;
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
    private final SkinsRestorerBukkit plugin;
    private final UpdateCheckerGitHub updateChecker;
    private final SRLogger logger;
    private final Server server;

    @Getter
    private DownloadFailReason failReason;

    private static Runnable downloadAsync(GitHubReleaseInfo releaseInfo, Path file, String userAgent, DownloadCallback callback) {
        return () -> {
            try {
                download(releaseInfo, file, userAgent);
                callback.finished();
            } catch (Exception e) {
                callback.error(e);
            }
        };
    }

    private static void download(GitHubReleaseInfo releaseInfo, Path file, String userAgent) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(releaseInfo.latestDownloadURL).openConnection();
            connection.setRequestProperty("User-Agent", userAgent);
            if (connection.getResponseCode() != 200) {
                throw new UpdateException("Download returned status #" + connection.getResponseCode());
            }

            ReadableByteChannel channel = Channels.newChannel(connection.getInputStream());

            try (FileOutputStream output = new FileOutputStream(file.toFile())) {
                output.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                output.flush();
            } catch (IOException e) {
                throw new UpdateException("Could not save file", e);
            }
        } catch (IOException e) {
            throw new UpdateException("Download failed", e);
        }
    }

    public boolean downloadUpdate() {
        GitHubReleaseInfo releaseInfo = updateChecker.getReleaseInfo();

        if (releaseInfo == null) {
            failReason = DownloadFailReason.NOT_CHECKED;
            return false; // Update is not yet checked
        }

        if (!updateChecker.isVersionNewer(plugin.getVersion(), releaseInfo.tag_name)) {
            failReason = DownloadFailReason.NO_UPDATE;
            return false; // Version is no update
        }

        Path pluginFile = getPluginFile();// /plugins/XXX.jar
        if (pluginFile == null) {
            failReason = DownloadFailReason.NO_PLUGIN_FILE;
            return false;
        }
        Path updateFolder = server.getUpdateFolderFile().toPath();
        try {
            Files.createDirectories(updateFolder);

        } catch (IOException e) {
            failReason = DownloadFailReason.NO_UPDATE_FOLDER;
            return false;
        }

        Path updateFile = updateFolder.resolve(pluginFile.getFileName());// /plugins/update/XXX.jar

        logger.info("[GitHubUpdate] Downloading update...");
        plugin.runAsync(downloadAsync(releaseInfo, updateFile, updateChecker.getUserAgent(), new DownloadCallback() {
            @Override
            public void finished() {
                logger.info("[GitHubUpdate] Update saved as " + updateFile.getFileName());
            }

            @Override
            public void error(Exception exception) {
                logger.warning("[GitHubUpdate] Could not download update", exception);
            }
        }));

        return true;
    }

    /**
     * Get the plugin's file name
     *
     * @return the plugin file name
     */
    public Path getPluginFile() {
        if (plugin == null) {
            return null;
        }
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            return ((File) method.invoke(plugin)).toPath();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not get plugin file", e);
        }
    }

    public enum DownloadFailReason {
        NOT_CHECKED,
        NO_UPDATE,
        NO_DOWNLOAD,
        NO_PLUGIN_FILE,
        NO_UPDATE_FOLDER,
        EXTERNAL_DISALLOWED
    }
}
