package skinsrestorer.shared.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.inventivetalent.update.spiget.ResourceInfo;
import org.inventivetalent.update.spiget.ResourceVersion;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All credits go to https://github.com/InventivetalentDev/SpigetUpdater
 */
public class UpdateChecker {
    public static final String RESOURCE_INFO = "http://api.spiget.org/v2/resources/%s?ut=%s";
    public static final String RESOURCE_VERSION = "http://api.spiget.org/v2/resources/%s/versions/latest?ut=%s";
    private final int resourceId;
    public String currentVersion;
    private Logger log;
    private String userAgent;
    private VersionComparator versionComparator = VersionComparator.SEM_VER_SNAPSHOT;
    private ResourceInfo latestResourceInfo;

    public UpdateChecker(int resourceId, String currentVersion, Logger log, String userAgent) {
        this.resourceId = resourceId;
        this.currentVersion = currentVersion;
        this.log = log;
        this.userAgent = userAgent;
    }

    public void checkForUpdate(final UpdateCallback callback) {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(String.format("http://api.spiget.org/v2/resources/%s?ut=%s", this.resourceId, System.currentTimeMillis()))).openConnection();
            connection.setRequestProperty("User-Agent", this.getUserAgent());
            JsonObject jsonObject = (new JsonParser()).parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
            this.latestResourceInfo = (ResourceInfo) (new Gson()).fromJson(jsonObject, ResourceInfo.class);
            connection = (HttpURLConnection) (new URL(String.format("http://api.spiget.org/v2/resources/%s/versions/latest?ut=%s", this.resourceId, System.currentTimeMillis()))).openConnection();
            connection.setRequestProperty("User-Agent", this.getUserAgent());
            jsonObject = (new JsonParser()).parse(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
            this.latestResourceInfo.latestVersion = (ResourceVersion) (new Gson()).fromJson(jsonObject, ResourceVersion.class);
            if (this.isVersionNewer(this.currentVersion, this.latestResourceInfo.latestVersion.name)) {
                callback.updateAvailable(this.latestResourceInfo.latestVersion.name, "https://spigotmc.org/" + this.latestResourceInfo.file.url, !this.latestResourceInfo.external);
            } else {
                callback.upToDate();
            }
        } catch (Exception var3) {
            this.log.log(Level.WARNING, "Failed to get resource info from spiget.org", var3);
        }
    }

    public boolean isVersionNewer(String oldVersion, String newVersion) {
        return this.versionComparator.isNewer(oldVersion, newVersion);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public ResourceInfo getLatestResourceInfo() {
        return this.latestResourceInfo;
    }
}
