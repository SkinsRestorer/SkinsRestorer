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
import net.skinsrestorer.shared.utils.SRHelpers;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

/**
 * Credit goes to <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateCheckerGitHub {
    private static final URI RELEASES_URL_LATEST = URI.create("https://api.github.com/repos/SkinsRestorer/SkinsRestorer/releases/latest");
    private static final String JAR_ASSET_NAME = "SkinsRestorer.jar";
    private static final String LOG_ROW = "§a----------------------------------------------";
    private final SRLogger logger;
    private final SRPlugin plugin;
    private final Injector injector;
    private final HttpClient httpClient;
    private boolean updateDownloaded;
    private boolean hasPrintedUpdateAvailableBanner;

    private static String getCheckerPluginVersion() {
        // Allow overriding the version for unit tests
        if (SRPlugin.isUnitTest()) {
            return System.getProperty("sr.check.version", BuildData.VERSION);
        } else {
            return BuildData.VERSION;
        }
    }

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

            Optional<GitHubAssetInfo> jarAssetUrl = releaseInfo.getAssets().stream()
                    .filter(asset -> asset.getName().equals(JAR_ASSET_NAME))
                    .findFirst();

            if (jarAssetUrl.isEmpty()) {
                throw new DataRequestExceptionShared("No jar asset found in release");
            }

            if (isVersionNewer(getCheckerPluginVersion(), releaseInfo.getTagName())) {
                plugin.setOutdated();

                // An update was already downloaded, we don't need to download it again
                if (updateDownloaded) {
                    return;
                }

                printUpdateAvailable(cause, releaseInfo.getTagName(), downloader != null);
                if (downloader != null && downloader.downloadUpdate(jarAssetUrl.get())) {
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
        logger.info("§b    Version: §a%s".formatted(BuildData.VERSION));
        logger.info("§b    Commit: §a%s".formatted(BuildData.COMMIT_SHORT));
        if (cause == UpdateCause.NETWORK_DISABLED) {
            logger.info("§c    Network connections are disabled in the SkinsRestorer config (advanced.noConnections), we will not check for updates.");
        } else if (Files.exists(plugin.getDataFolder().resolve("noautoupdate.txt"))
                || Files.exists(plugin.getDataFolder().resolve("noupdate.txt"))) { // Legacy support
            logger.info("§e    The updater is disabled using noautoupdate.txt");
        } else {
            logger.info("§a    This is the latest version!");
        }
        printFooter();
    }

    public void printUpdateAvailable(UpdateCause cause, String newVersion, boolean updateDownloader) {
        if (hasPrintedUpdateAvailableBanner) {
            logger.info("§bA new version of SkinsRestorer (§a%s§b) is available: §a%s/version/%s".formatted(newVersion, SRHelpers.DOWNLOAD_URL, newVersion));
        } else {
            hasPrintedUpdateAvailableBanner = true;
            printHeader(cause);
            logger.info("§b    Version: §c%s".formatted(BuildData.VERSION));
            logger.info("§b    Commit: §c%s".formatted(BuildData.COMMIT_SHORT));
            if (updateDownloader) {
                logger.info("§b    A new version (§a%s§b) is available! Downloading update...".formatted(newVersion));
            } else {
                logger.info("§b    A new version (§a%s§b) is available!".formatted(newVersion));
                logger.info("§e    %s/version/%s".formatted(SRHelpers.DOWNLOAD_URL, newVersion));
            }
            printFooter();
        }
    }

    private void printHeader(UpdateCause cause) {
        logger.info(LOG_ROW);
        logger.info("§a    +==================+");
        logger.info("§a    |   SkinsRestorer  |");
        logger.info("§a    |------------------|");

        if (cause.isError()) {
            logger.info("§a    |    §cError Mode§a    |");
        } else {
            SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
            if (serverPlugin != null) {
                if (serverPlugin.isProxyMode()) {
                    logger.info("§a    |    §eProxy Mode§a    |");
                } else {
                    logger.info("§a    |  §9§n§lStandalone Mode§r§a |");
                }
            }
        }
        logger.info("§a    +==================+");
        logger.info(LOG_ROW);
    }

    private void printFooter() {
        logger.info(LOG_ROW);
        logger.info("§9Do you have issues? Read our troubleshooting guide: §ehttps://skinsrestorer.net/docs/troubleshooting");
        logger.info("§9Want to support SkinsRestorer? Consider donating: §ehttps://skinsrestorer.net/donate");
    }

    public boolean isVersionNewer(String currentVersion, String newVersion) {
        return SemanticVersion.fromString(newVersion).isNewerThan(SemanticVersion.fromString(currentVersion));
    }
}
