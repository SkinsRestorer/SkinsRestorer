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
package net.skinsrestorer.shared.update;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.interfaces.SRPlugin;
import net.skinsrestorer.shared.interfaces.SRServerPlugin;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;

import javax.inject.Inject;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Credit goes to <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckerGitHub {
    private static final String RESOURCE_ID = "SkinsRestorerX";
    private static final String RELEASES_URL_LATEST = "https://api.github.com/repos/SkinsRestorer/%s/releases/latest";
    private static final String LOG_ROW = "§a----------------------------------------------";
    private final SRLogger logger;
    private final SRPlugin plugin;
    @Getter
    private GitHubReleaseInfo releaseInfo;

    public void checkForUpdate(UpdateCallback callback) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(RELEASES_URL_LATEST, RESOURCE_ID)).openConnection();
            connection.setRequestProperty("User-Agent", plugin.getUpdateCheckerAgent());

            releaseInfo = new Gson().fromJson(new InputStreamReader(connection.getInputStream()), GitHubReleaseInfo.class);

            releaseInfo.assets.forEach(gitHubAssetInfo -> {
                releaseInfo.latestDownloadURL = gitHubAssetInfo.browser_download_url;

                if (isVersionNewer(plugin.getVersion(), releaseInfo.tag_name)) {
                    callback.updateAvailable(releaseInfo.tag_name, gitHubAssetInfo.browser_download_url, true);
                } else {
                    callback.upToDate();
                }
            });
        } catch (Exception e) {
            logger.warning("Failed to get release info from api.github.com. \n If this message is repeated a lot, please see https://skinsrestorer.net/firewall");
            logger.debug(e);
        }
    }

    public List<String> getUpToDateMessages() {
        List<String> upToDateMessages = new LinkedList<>();
        fillHeader(upToDateMessages, plugin);
        upToDateMessages.add("§b    Current version: §a" + plugin.getVersion());
        upToDateMessages.add("§a    This is the latest version!");
        upToDateMessages.add(LOG_ROW);

        return upToDateMessages;
    }

    public List<String> getUpdateAvailableMessages(String newVersion, String downloadUrl, boolean hasDirectDownload, String currentVersion) {
        return getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, currentVersion, false, null);
    }

    public List<String> getUpdateAvailableMessages(String newVersion, String downloadUrl, boolean hasDirectDownload, String currentVersion, boolean updateDownloader, String failReason) {
        List<String> updateAvailableMessages = new LinkedList<>();
        fillHeader(updateAvailableMessages, plugin);

        updateAvailableMessages.add("§b    Current version: §c" + currentVersion);
        updateAvailableMessages.add("§b    New version: §c" + newVersion);

        if (updateDownloader && hasDirectDownload) {
            updateAvailableMessages.add("    A new version is available! Downloading it now...");
            if (failReason == null) {
                updateAvailableMessages.add("    Update downloaded successfully, it will be applied on the next restart.");
            } else {
                // Update failed
                updateAvailableMessages.add("§cCould not download the update, reason: " + failReason);
            }
        } else {
            updateAvailableMessages.add("§e    A new version is available! Download it at:");
            updateAvailableMessages.add("§e    " + downloadUrl);
        }

        updateAvailableMessages.add(LOG_ROW);

        return updateAvailableMessages;
    }

    private void fillHeader(List<String> updateAvailableMessages, SRPlugin plugin) {
        updateAvailableMessages.add(LOG_ROW);
        updateAvailableMessages.add("§a    +==================+");
        updateAvailableMessages.add("§a    |   SkinsRestorer  |");
        if (plugin instanceof SRServerPlugin) {
            SRServerPlugin serverPlugin = (SRServerPlugin) plugin;
            if (serverPlugin.isProxyMode()) {
                updateAvailableMessages.add("§a    |------------------|");
                updateAvailableMessages.add("§a    |    §eProxy Mode§a    |");
            } else {
                updateAvailableMessages.add("§a    |------------------|");
                updateAvailableMessages.add("§a    |  §9§n§lStandalone Mode§r§a |");
            }
        }
        updateAvailableMessages.add("§a    +==================+");
        updateAvailableMessages.add(LOG_ROW);
    }

    public boolean isVersionNewer(String oldVersion, String newVersion) {
        return VersionComparator.SEM_VER_SNAPSHOT.isNewer(oldVersion, newVersion);
    }
}
