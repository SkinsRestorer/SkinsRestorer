package skinsrestorer.shared.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by McLive on 13.04.2019.
 */
public class GitHubUpdateDownloader {
    public GitHubUpdateDownloader() {
    }

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
