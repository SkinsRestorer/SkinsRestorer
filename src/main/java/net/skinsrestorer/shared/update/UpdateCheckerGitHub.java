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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.SRLogger;
import org.inventivetalent.update.spiget.UpdateCallback;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateCheckerGitHub extends UpdateChecker {
    private static final String RESOURCE_ID = "SkinsRestorerX";
    private static final String RELEASES_URL_LATEST = "https://api.github.com/repos/SkinsRestorer/%s/releases/latest";
    private final SRLogger log;
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
            HttpURLConnection connection = (HttpURLConnection) new URL(String.format(RELEASES_URL_LATEST, RESOURCE_ID)).openConnection();
            connection.setRequestProperty("User-Agent", this.userAgent);

            JsonObject apiResponse = new JsonParser().parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
            releaseInfo = new Gson().fromJson(apiResponse, GitHubReleaseInfo.class);

            releaseInfo.assets.forEach(gitHubAssetInfo -> {
                releaseInfo.latestDownloadURL = gitHubAssetInfo.browser_download_url;

                if (isVersionNewer(currentVersion, releaseInfo.tag_name)) {
                    callback.updateAvailable(releaseInfo.tag_name, gitHubAssetInfo.browser_download_url, true);
                } else {
                    callback.upToDate();
                }
            });

        } catch (Exception e) {
            log.logAlways(Level.WARNING, "Failed to get release info from api.github.com. \n If this message is repeated a lot, please see http://skinsrestorer.net/firewall");
            if (Config.DEBUG)
                e.printStackTrace();
        }
    }

    @Override
    public GitHubReleaseInfo getLatestResourceInfo() {
        return releaseInfo;
    }
}
