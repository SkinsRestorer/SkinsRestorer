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
package net.skinsrestorer.bukkit.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.shared.update.UpdateChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.update.spiget.ResourceInfo;
import org.inventivetalent.update.spiget.download.DownloadCallback;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * All credits go to https://github.com/InventivetalentDev/SpigetUpdater
 */
@RequiredArgsConstructor
public class UpdateDownloader {
    protected final SkinsRestorerBukkit plugin;
    protected final UpdateChecker updateChecker;
    protected final SRLogger logger;

    @Getter
    protected DownloadFailReason failReason;

    public boolean downloadUpdate() {
        ResourceInfo latestResourceInfo = updateChecker.getLatestResourceInfo();

        if (latestResourceInfo == null) {
            failReason = DownloadFailReason.NOT_CHECKED;
            return false;// Update not yet checked
        }

        if (!updateChecker.isVersionNewer(updateChecker.getCurrentVersion(), latestResourceInfo.latestVersion.name)) {
            failReason = DownloadFailReason.NO_UPDATE;
            return false;// Version is no update
        }

        if (latestResourceInfo.external) {
            failReason = DownloadFailReason.NO_DOWNLOAD;
            return false;// No download available
        }

        File pluginFile = getPluginFile();// /plugins/XXX.jar
        if (pluginFile == null) {
            failReason = DownloadFailReason.NO_PLUGIN_FILE;
            return false;
        }

        File updateFolder = Bukkit.getUpdateFolderFile();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            failReason = DownloadFailReason.NO_UPDATE_FOLDER;
            return false;
        }
        final File updateFile = new File(updateFolder, pluginFile.getName());

        Properties properties = getUpdaterProperties();
        boolean allowExternalDownload = properties != null && properties.containsKey("externalDownloads") && Boolean.parseBoolean(properties.getProperty("externalDownloads"));

        if (!allowExternalDownload && latestResourceInfo.external) {
            failReason = DownloadFailReason.EXTERNAL_DISALLOWED;
            return false;
        }

        logger.info("[SpigetUpdate] Downloading update...");
        plugin.runAsync(org.inventivetalent.update.spiget.download.UpdateDownloader.downloadAsync(latestResourceInfo, updateFile, updateChecker.getUserAgent(), new DownloadCallback() {
            @Override
            public void finished() {
                logger.info("[SpigetUpdate] Update saved as " + updateFile.getPath());
            }

            @Override
            public void error(Exception exception) {
                logger.warning("[SpigetUpdate] Could not download update", exception);
            }
        }));

        return true;
    }

    public Properties getUpdaterProperties() {
        File file = new File(Bukkit.getUpdateFolderFile(), "spiget.properties");
        Properties properties = new Properties();
        if (!file.exists()) {

            try {
                if (!file.createNewFile()) {
                    return null;
                }
                properties.setProperty("externalDownloads", "false");

                try (FileWriter write = new FileWriter(file)) {
                    properties.store(write, "Configuration for the Spiget auto-updater. https://spiget.org | https://github.com/InventivetalentDev/SpigetUpdater\n"
                            + "Use 'externalDownloads' if you want to auto-download resources hosted on external sites\n"
                            + "");
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        try (FileReader reader = new FileReader(file)) {
            properties.load(reader);
        } catch (IOException e) {
            return null;
        }
        return properties;
    }

    /**
     * Get the plugin's file name
     *
     * @return the plugin file name
     */
    public File getPluginFile() {
        if (plugin == null) {
            return null;
        }
        try {
            Method method = JavaPlugin.class.getDeclaredMethod("getFile");
            method.setAccessible(true);
            return (File) method.invoke(plugin);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not get plugin file", e);
        }
    }

    public enum DownloadFailReason {
        NOT_CHECKED,
        NO_UPDATE,
        NO_DOWNLOAD,
        NO_PLUGIN_FILE,
        NO_UPDATE_FOLDER,
        EXTERNAL_DISALLOWED
    }
}
