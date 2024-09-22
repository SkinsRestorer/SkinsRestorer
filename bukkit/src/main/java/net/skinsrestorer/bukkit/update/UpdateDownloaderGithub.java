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
package net.skinsrestorer.bukkit.update;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.utils.PluginJarProvider;
import net.skinsrestorer.shared.exception.UpdateException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.update.UpdateDownloader;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parts taken from <a href="https://github.com/InventivetalentDev/SpigetUpdater">SpigetUpdater</a>
 */
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UpdateDownloaderGithub implements UpdateDownloader {
    private final SRPlugin plugin;
    private final SRLogger logger;
    private final Server server;
    private final PluginJarProvider jarProvider;

    private void download(String downloadUrl, Path targetFile, @Nullable String expectedHash) throws UpdateException {
        try {
            // We don't use HttpClient because this writes to a file directly
            HttpsURLConnection connection = (HttpsURLConnection) URI.create(downloadUrl).toURL().openConnection();
            connection.setRequestProperty("User-Agent", plugin.getUserAgent());
            if (connection.getResponseCode() != 200) {
                throw new UpdateException("Download returned status code %d".formatted(connection.getResponseCode()));
            }

            byte[] fileData;
            try (InputStream is = connection.getInputStream()) {
                if (is == null) {
                    throw new IOException("Failed to open input stream");
                }

                fileData = is.readAllBytes();
                if (expectedHash != null && !expectedHash.equals(SRHelpers.hashSha256ToHex(fileData))) {
                    throw new UpdateException("Downloaded file is corrupted. SHA256 hash does not match.");
                } else if (expectedHash == null) {
                    logger.warning("[GitHubUpdate] SHA256 hash not found, cannot verify integrity");
                } else {
                    logger.debug("[GitHubUpdate] SHA256 hash successfully verified");
                }
            }

            Files.write(targetFile, fileData);
        } catch (IOException e) {
            throw new UpdateException("Download failed", e);
        }
    }

    private String readStringFromUrl(String url) throws UpdateException {
        try {
            HttpsURLConnection connection = (HttpsURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent", plugin.getUserAgent());
            if (connection.getResponseCode() != 200) {
                throw new UpdateException("Download returned status code %d".formatted(connection.getResponseCode()));
            }

            try (InputStream is = connection.getInputStream()) {
                if (is == null) {
                    throw new IOException("Failed to open input stream");
                }

                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UpdateException("Download failed", e);
        }
    }

    @Override
    public boolean downloadUpdate(String downloadUrl, @Nullable String verificationAssetUrl) {
        Path pluginFile = jarProvider.get(); // /plugins/XXX.jar
        Path updateFolder = server.getUpdateFolderFile().toPath();
        SRHelpers.createDirectoriesSafe(updateFolder);

        Path updateFile = updateFolder.resolve(pluginFile.getFileName()); // /plugins/update/XXX.jar

        logger.info("[GitHubUpdate] Downloading update...");
        try {
            long start = System.currentTimeMillis();
            String expectedHash = verificationAssetUrl == null ? null : readStringFromUrl(verificationAssetUrl).lines().findFirst().orElse(null);
            download(downloadUrl, updateFile, expectedHash);

            logger.info("[GitHubUpdate] Downloaded update in %dms".formatted(System.currentTimeMillis() - start));
            logger.info("[GitHubUpdate] Update saved as %s".formatted(updateFile.getFileName()));
            logger.info("[GitHubUpdate] The update will be loaded on the next server restart");
        } catch (UpdateException e) {
            logger.warning("[GitHubUpdate] Could not download update", e);
            return false;
        }

        return true;
    }
}
