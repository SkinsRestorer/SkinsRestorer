/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.connections.http.HttpClient;
import net.skinsrestorer.shared.connections.http.HttpResponse;
import net.skinsrestorer.shared.exception.DataRequestExceptionShared;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.update.model.GitHubAssetInfo;
import net.skinsrestorer.shared.update.model.GitHubReleaseInfo;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;

/**
 * Credit goes to <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckerGitHub {
    private static final URI RELEASES_URL_LATEST = URI.create("https://api.github.com/repos/SkinsRestorer/SkinsRestorerX/releases/latest");
    private static final String JAR_ASSET_NAME = "SkinsRestorer.jar";
    private static final String LOG_ROW = "§a----------------------------------------------";
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final Injector injector;
    private final HttpClient httpClient;
    private boolean updateDownloaded;

    public void checkForUpdate(UpdateCause cause, UpdateDownloader downloader) {
        try {
            HttpResponse response = httpClient.execute(RELEASES_URL_LATEST,
                    null,
                    HttpClient.HttpType.JSON,
                    plugin.getUserAgent(),
                    HttpClient.HttpMethod.GET,
                    Collections.emptyMap(),
                    90_000);
            GitHubReleaseInfo releaseInfo = response.getBodyAs(GitHubReleaseInfo.class);

            if (releaseInfo.getAssets() == null || releaseInfo.getAssets().isEmpty()) {
                throw new DataRequestExceptionShared("No release info found");
            }

            Optional<String> jarAssetUrl = releaseInfo.getAssets().stream()
                    .filter(asset -> asset.getName().equals(JAR_ASSET_NAME))
                    .map(GitHubAssetInfo::getBrowserDownloadUrl)
                    .findFirst();

            if (!jarAssetUrl.isPresent()) {
                throw new DataRequestExceptionShared("No jar asset found in release");
            }

            if (isVersionNewer(BuildData.VERSION, releaseInfo.getTagName())) {
                plugin.setOutdated();

                // An update was already downloaded, we don't need to download it again
                if (updateDownloaded) {
                    return;
                }

                String downloadUrl = jarAssetUrl.get();
                printUpdateAvailable(cause, releaseInfo.getTagName(), downloadUrl, downloader != null);
                if (downloader != null && downloader.downloadUpdate(downloadUrl)) {
                    updateDownloaded = true;
                }
            } else {
                if (cause == UpdateCause.SCHEDULED) {
                    return;
                }

                printUpToDate(cause);
            }
        } catch (IOException | DataRequestException e) {
            logger.warning("Failed to get release info from api.github.com. \n If this message is repeated a lot, please see https://skinsrestorer.net/firewall");
            logger.debug(e);
        }
    }

    public void printUpToDate(UpdateCause cause) {
        printHeader(cause);
        logger.info("§b    Version: §a" + BuildData.VERSION);
        logger.info("§b    Commit: §a" + BuildData.COMMIT_SHORT);
        if (cause == UpdateCause.NO_NETWORK) {
            logger.info("§c    No network connection available!");
        } else {
            logger.info("§a    This is the latest version!");
        }
        printFooter();
    }

    public void printUpdateAvailable(UpdateCause cause, String newVersion, String downloadUrl, boolean updateDownloader) {
        printHeader(cause);
        logger.info("§b    Version: §c" + BuildData.VERSION);
        logger.info("§b    Commit: §c" + BuildData.COMMIT_SHORT);
        if (updateDownloader) {
            logger.info("§b    A new version (§a" + newVersion + "§b) is available! Downloading update...");
        } else {
            logger.info("§b    A new version (§a" + newVersion + "§b) is available!");
            logger.info("§e    " + downloadUrl);
        }
        printFooter();
    }

    private void printHeader(UpdateCause cause) {
        logger.info(LOG_ROW);
        logger.info("§a    +==================+");
        logger.info("§a    |   SkinsRestorer  |");
        if (cause.isError()) {
            logger.info("§a    |------------------|");
            logger.info("§a    |    §cError Mode§a    |");
        } else {
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
        }
        logger.info("§a    +==================+");
        logger.info(LOG_ROW);
    }

    private void printFooter() {
        logger.info(LOG_ROW);
    }

    public boolean isVersionNewer(String currentVersion, String newVersion) {
        return SemanticVersion.fromString(newVersion).isNewerThan(SemanticVersion.fromString(currentVersion));
    }
}
