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
import net.skinsrestorer.builddata.BuildData;
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

    public void printUpToDate() {
        printHeader();
        logger.info("§b    Current version: §a" + plugin.getVersion());
        logger.info("§b    Commit: §a" + BuildData.COMMIT_SHORT);
        logger.info("§a    This is the latest version!");
        logger.info(LOG_ROW);
    }

    public void printUpdateAvailable(String newVersion, String downloadUrl, boolean updateDownloader) {
        printHeader();

        logger.info("§b    Current version: §c" + plugin.getVersion());
        logger.info("§b    Commit: §a" + BuildData.COMMIT_SHORT);
        logger.info("§b    New version: §c" + newVersion);

        if (updateDownloader) {
            logger.info("    A new version is available! Downloading it now...");
        } else {
            logger.info("§e    A new version is available! Download it at:");
            logger.info("§e    " + downloadUrl);
        }

        logger.info(LOG_ROW);
    }

    private void printHeader() {
        logger.info(LOG_ROW);
        logger.info("§a    +==================+");
        logger.info("§a    |   SkinsRestorer  |");
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
            if (serverPlugin.isProxyMode()) {
                logger.info("§a    |------------------|");
                logger.info("§a    |    §eProxy Mode§a    |");
            } else {
                logger.info("§a    |------------------|");
                logger.info("§a    |  §9§n§lStandalone Mode§r§a |");
            }
        }
        logger.info("§a    +==================+");
        logger.info(LOG_ROW);
    }

    public boolean isVersionNewer(String currentVersion, String newVersion) {
        return SemanticVersion.fromString(newVersion).isNewerThan(SemanticVersion.fromString(currentVersion));
    }
}
