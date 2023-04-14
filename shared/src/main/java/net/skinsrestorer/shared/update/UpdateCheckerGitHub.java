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

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.serverinfo.SemanticVersion;
import net.skinsrestorer.shared.update.model.GitHubReleaseInfo;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Credit goes to <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckerGitHub {
    private static final String RELEASES_URL_LATEST = "https://api.github.com/repos/SkinsRestorer/SkinsRestorerX/releases/latest";
    private static final String LOG_ROW = "§a----------------------------------------------";
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final Injector injector;

    public void checkForUpdate(UpdateCallback callback) {
        HttpClient client = new HttpClient(
                RELEASES_URL_LATEST,
                null,
                HttpClient.HttpType.JSON,
                plugin.getUserAgent(),
                HttpClient.HttpMethod.GET,
                Collections.emptyMap(),
                90_000
        );

        try {
            HttpResponse response = client.execute();
            GitHubReleaseInfo releaseInfo = response.getBodyAs(GitHubReleaseInfo.class);

            logger.debug("Response body: " + response.getBody());
            logger.debug("Response code: " + response.getStatusCode());

            releaseInfo.getAssets().forEach(gitHubAssetInfo -> { // TODO: Check if this is the correct asset
                if (isVersionNewer(plugin.getVersion(), releaseInfo.getTagName())) {
                    callback.updateAvailable(releaseInfo.getTagName(), gitHubAssetInfo.getBrowserDownloadUrl());
                } else {
                    callback.upToDate();
                }
            });
        } catch (IOException | DataRequestException e) {
            logger.warning("Failed to get release info from api.github.com. \n If this message is repeated a lot, please see https://skinsrestorer.net/firewall");
            logger.debug(e);
        }
    }

    public List<String> getUpToDateMessages() {
        List<String> upToDateMessages = new LinkedList<>();
        fillHeader(upToDateMessages);
        upToDateMessages.add("§b    Current version: §a" + plugin.getVersion());
        upToDateMessages.add("§a    This is the latest version!");
        upToDateMessages.add(LOG_ROW);

        return upToDateMessages;
    }

    public List<String> getUpdateAvailableMessages(String newVersion, String downloadUrl, boolean updateDownloader) {
        List<String> updateAvailableMessages = new LinkedList<>();
        fillHeader(updateAvailableMessages);

        updateAvailableMessages.add("§b    Current version: §c" + plugin.getVersion());
        updateAvailableMessages.add("§b    New version: §c" + newVersion);

        if (updateDownloader) {
            updateAvailableMessages.add("    A new version is available! Downloading it now...");
        } else {
            updateAvailableMessages.add("§e    A new version is available! Download it at:");
            updateAvailableMessages.add("§e    " + downloadUrl);
        }

        updateAvailableMessages.add(LOG_ROW);

        return updateAvailableMessages;
    }

    private void fillHeader(List<String> updateAvailableMessages) {
        updateAvailableMessages.add(LOG_ROW);
        updateAvailableMessages.add("§a    +==================+");
        updateAvailableMessages.add("§a    |   SkinsRestorer  |");
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
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

    public boolean isVersionNewer(String currentVersion, String newVersion) {
        return SemanticVersion.fromString(newVersion).isNewerThan(SemanticVersion.fromString(currentVersion));
    }
}
