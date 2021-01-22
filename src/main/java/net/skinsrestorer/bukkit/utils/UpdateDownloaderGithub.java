package net.skinsrestorer.bukkit.utils;

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.update.DownloadCallback;
import net.skinsrestorer.shared.update.GitHubReleaseInfo;
import net.skinsrestorer.shared.update.GitHubUpdateDownloader;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.logging.Level;

/**
 * Created by McLive on 13.04.2019.
 */
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
