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
package net.skinsrestorer.shared.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class GitHubUpdateDownloader {
    private GitHubUpdateDownloader() {}

    public static Runnable downloadAsync(final GitHubReleaseInfo releaseInfo, final File file, final String userAgent, final DownloadCallback callback) {
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
                throw new RuntimeException("Download returned status #" + connection.getResponseCode());
            }

            channel = Channels.newChannel(connection.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Download failed", e);
        }

        try {
            FileOutputStream output = new FileOutputStream(file);
            output.getChannel().transferFrom(channel, 0L, 9223372036854775807L);
            output.flush();
            output.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not save file", e);
        }
    }
}
