/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.inventivetalent.update.spiget.ResourceInfo;
import org.inventivetalent.update.spiget.ResourceVersion;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * All credits go to https://github.com/InventivetalentDev/SpigetUpdater
 */
public class UpdateChecker {
    public static final String RESOURCE_INFO = "https://api.spiget.org/v2/resources/%s?ut=%s";
    public static final String RESOURCE_VERSION = "https://api.spiget.org/v2/resources/%s/versions/latest?ut=%s";
    private final int resourceId;
    private final SRLogger log;
    @Getter
    private final String currentVersion;
    @Getter
    private final String userAgent;
    @Getter
    private ResourceInfo latestResourceInfo;

    public UpdateChecker(int resourceId, String currentVersion, SRLogger log, String userAgent) {
        this.resourceId = resourceId;
        this.currentVersion = currentVersion;
        this.log = log;
        this.userAgent = userAgent;
    }

    public void checkForUpdate(final UpdateCallback callback) {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(String.format(RESOURCE_INFO, resourceId, System.currentTimeMillis()))).openConnection();
            connection.setRequestProperty("User-Agent", getUserAgent());

            latestResourceInfo = new Gson().fromJson(new InputStreamReader(connection.getInputStream()), ResourceInfo.class);
            connection = (HttpURLConnection) new URL(String.format(RESOURCE_VERSION, resourceId, System.currentTimeMillis())).openConnection();
            connection.setRequestProperty("User-Agent", getUserAgent());
            latestResourceInfo.latestVersion = new Gson().fromJson(new InputStreamReader(connection.getInputStream()), ResourceVersion.class);

            if (isVersionNewer(currentVersion, latestResourceInfo.latestVersion.name)) {
                callback.updateAvailable(latestResourceInfo.latestVersion.name, "https://spigotmc.org/" + latestResourceInfo.file.url, !latestResourceInfo.external);
            } else {
                callback.upToDate();
            }
        } catch (Exception e) {
            log.debug(SRLogLevel.WARNING, "Failed to get resource info from spiget.org", e);
        }
    }

    public List<String> getUpToDateMessages(String currentVersion, boolean bungeeMode) {
        List<String> upToDateMessages = new LinkedList<>();
        upToDateMessages.add("§a----------------------------------------------");
        upToDateMessages.add("§a    +==================+");
        upToDateMessages.add("§a    |   SkinsRestorer  |");
        upToDateMessages.add("§a    |------------------|");
        if (bungeeMode) {

            upToDateMessages.add("§a    |   §eBungee Mode§a    |");
        } else {
            try {
                Bukkit.getName(); //try if it is running bukkit
                upToDateMessages.add("§a    |  §9§n§lStandalone Mode§a |");
            } catch (NoClassDefFoundError ignored) {
            }
        }
        upToDateMessages.add("§a    +==================+");
        upToDateMessages.add("§a----------------------------------------------");
        upToDateMessages.add("§b    Current version: §a" + currentVersion);
        upToDateMessages.add("§a    This is the latest version!");
        upToDateMessages.add("§a----------------------------------------------");

        return upToDateMessages;
    }

    public List<String> getUpdateAvailableMessages(String newVersion, String downloadUrl, boolean hasDirectDownload, String currentVersion, boolean bungeeMode) {
        return getUpdateAvailableMessages(newVersion, downloadUrl, hasDirectDownload, currentVersion, bungeeMode, false, null);

    }

    public List<String> getUpdateAvailableMessages(String newVersion, String downloadUrl, boolean hasDirectDownload, String currentVersion, boolean bungeeMode, boolean updateDownloader, String failReason) {
        List<String> updateAvailableMessages = new LinkedList<>();

        updateAvailableMessages.add("§a----------------------------------------------");
        updateAvailableMessages.add("§a    +==================+");
        updateAvailableMessages.add("§a    |   SkinsRestorer  |");
        updateAvailableMessages.add("§a    |------------------|");
        if (bungeeMode) {

            updateAvailableMessages.add("§a    |   §eBungee Mode§a    |");
        } else {
            try {
                Bukkit.getName(); //try if it is running bukkit
                updateAvailableMessages.add("§a    |  §9§n§lStandalone Mode§a |");
            } catch (NoClassDefFoundError ignored) {
            }
        }
        updateAvailableMessages.add("§a    +==================+");
        updateAvailableMessages.add("§a----------------------------------------------");
        updateAvailableMessages.add("§b    Current version: §c" + currentVersion);
        updateAvailableMessages.add("§b    New version: §c" + newVersion);

        if (updateDownloader && hasDirectDownload) {
            updateAvailableMessages.add("    A new version is available! Downloading it now...");
            if (failReason == null) {
                updateAvailableMessages.add("    Update downloaded successfully, it will be applied on the next restart.");
            } else {
                // Update failed
                updateAvailableMessages.add("§cCould not download the update, reason: " + failReason);
            }
        } else {
            updateAvailableMessages.add("§e    A new version is available! Download it at:");
            updateAvailableMessages.add("§e    " + downloadUrl);
        }

        updateAvailableMessages.add("§a----------------------------------------------");

        return updateAvailableMessages;
    }

    public boolean isVersionNewer(String oldVersion, String newVersion) {
        return VersionComparator.SEM_VER_SNAPSHOT.isNewer(oldVersion, newVersion);
    }
}
