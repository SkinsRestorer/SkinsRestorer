/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bukkit.utils;

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.update.DownloadCallback;
import net.skinsrestorer.shared.update.GitHubReleaseInfo;
import net.skinsrestorer.shared.update.GitHubUpdateDownloader;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.logging.Level;

public class UpdateDownloaderGithub extends UpdateDownloader {
    private final SkinsRestorer plugin;

    public UpdateDownloaderGithub(SkinsRestorer plugin) {
        super(plugin);
        this.plugin = plugin;
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

        plugin.getLogger().info("[GitHubUpdate] Downloading update...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, GitHubUpdateDownloader.downloadAsync(releaseInfo, updateFile, plugin.getUpdateChecker().getUserAgent(), new DownloadCallback() {
            @Override
            public void finished() {
                plugin.getLogger().info("[GitHubUpdate] Update saved as " + updateFile.getPath());
            }

            @Override
            public void error(Exception exception) {
                plugin.getLogger().log(Level.WARNING, "[GitHubUpdate] Could not download update", exception);
            }
        }));

        return true;
    }
}
