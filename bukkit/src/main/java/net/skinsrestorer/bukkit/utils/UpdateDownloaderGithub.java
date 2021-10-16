/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.exception.UpdateException;
import net.skinsrestorer.shared.update.DownloadCallback;
import net.skinsrestorer.shared.update.GitHubReleaseInfo;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UpdateDownloaderGithub extends UpdateDownloader {
    private final SkinsRestorer plugin;

    public UpdateDownloaderGithub(SkinsRestorer plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    private static Runnable downloadAsync(final GitHubReleaseInfo releaseInfo, final File file, final String userAgent, final DownloadCallback callback) {
        return () -> {
            try {
                download(releaseInfo, file, userAgent);
                callback.finished();
            } catch (Exception e) {
                callback.error(e);
            }
        };
    }

    private static void download(GitHubReleaseInfo releaseInfo, File file, String userAgent) {
        ReadableByteChannel channel;

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(releaseInfo.latestDownloadURL).openConnection();
            connection.setRequestProperty("User-Agent", userAgent);
            if (connection.getResponseCode() != 200) {
                throw new UpdateException("Download returned status #" + connection.getResponseCode());
            }

            channel = Channels.newChannel(connection.getInputStream());
        } catch (IOException e) {
            throw new UpdateException("Download failed", e);
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            FileOutputStream output = new FileOutputStream(file);
            output.getChannel().transferFrom(channel, 0L, 9223372036854775807L);
            output.flush();
        } catch (IOException e) {
            throw new UpdateException("Could not save file", e);
        }
    }

    @Override
    public boolean downloadUpdate() {
        GitHubReleaseInfo releaseInfo = (GitHubReleaseInfo) plugin.getUpdateChecker().getLatestResourceInfo();

        if (releaseInfo == null) {
            failReason = DownloadFailReason.NOT_CHECKED;
            return false;// Update not yet checked
        }
        if (!plugin.getUpdateChecker().isVersionNewer(plugin.getUpdateChecker().getCurrentVersion(), releaseInfo.tag_name)) {
            failReason = DownloadFailReason.NO_UPDATE;
            return false;// Version is no update
        }

        File pluginFile = getPluginFile();// /plugins/XXX.jar
        if (pluginFile == null) {
            failReason = DownloadFailReason.NO_PLUGIN_FILE;
            return false;
        }
        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            failReason = DownloadFailReason.NO_UPDATE_FOLDER;
            return false;
        }
        final File updateFile = new File(updateFolder, pluginFile.getName());

        plugin.getSrLogger().info("[GitHubUpdate] Downloading update...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, downloadAsync(releaseInfo, updateFile, plugin.getUpdateChecker().getUserAgent(), new DownloadCallback() {
            @Override
            public void finished() {
                plugin.getSrLogger().info("[GitHubUpdate] Update saved as " + updateFile.getPath());
            }

            @Override
            public void error(Exception exception) {
                plugin.getSrLogger().warning("[GitHubUpdate] Could not download update", exception);
            }
        }));

        return true;
    }
}
