package net.skinsrestorer.shared.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.shared.utils.SRLogger;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by McLive on 11.04.2019.
 */
public class UpdateCheckerGitHub extends UpdateChecker {
    private SRLogger log;
    private final String userAgent;
    private final String currentVersion;
    private GitHubReleaseInfo releaseInfo;

    public UpdateCheckerGitHub(int resourceId, String currentVersion, SRLogger log, String userAgent) {
        super(resourceId, currentVersion, log, userAgent);
        this.log = log;
        this.userAgent = userAgent;
        this.currentVersion = currentVersion;
    }

    @Override
    public void checkForUpdate(final UpdateCallback callback) {
        try {
            String resourceId = "SkinsRestorerX";
            String releaseUrl = "https://api.github.com/repos/SkinsRestorer/%s/releases/latest";

            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(releaseUrl, resourceId)).openConnection();
            connection.setRequestProperty("User-Agent", this.userAgent);

            JsonObject apiResponse = new JsonParser().parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
            this.releaseInfo = new Gson().fromJson(apiResponse, GitHubReleaseInfo.class);

            releaseInfo.assets.forEach(gitHubAssetInfo -> {
                releaseInfo.latestDownloadURL = gitHubAssetInfo.browser_download_url;

                if (this.isVersionNewer(this.currentVersion, releaseInfo.tag_name)) {
                    callback.updateAvailable(releaseInfo.tag_name, gitHubAssetInfo.browser_download_url, true);
                } else {
                    callback.upToDate();
                }
            });

        } catch (Exception e) {
            // this.log.log(Level.WARNING, "Failed to get release info from api.github.com.");
        }
    }

    @Override
    public GitHubReleaseInfo getLatestResourceInfo() {
        return this.releaseInfo;
    }
}
