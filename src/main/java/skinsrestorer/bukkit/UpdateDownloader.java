package skinsrestorer.bukkit;

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
import java.util.logging.Level;

/**
 * All credits go to https://github.com/InventivetalentDev/SpigetUpdater
 */
public class UpdateDownloader {
    private SkinsRestorer plugin;

    public DownloadFailReason failReason;
    private ResourceInfo latestResourceInfo;

    public UpdateDownloader(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    public boolean downloadUpdate() {
        this.latestResourceInfo = plugin.getUpdateChecker().getLatestResourceInfo();

        if (latestResourceInfo == null) {
            failReason = DownloadFailReason.NOT_CHECKED;
            return false;// Update not yet checked
        }
        if (!plugin.getUpdateChecker().isVersionNewer(plugin.getUpdateChecker().currentVersion, latestResourceInfo.latestVersion.name)) {
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
        if (!updateFolder.exists()) {
            if (!updateFolder.mkdirs()) {
                failReason = DownloadFailReason.NO_UPDATE_FOLDER;
                return false;
            }
        }
        final File updateFile = new File(updateFolder, pluginFile.getName());

        Properties properties = getUpdaterProperties();
        boolean allowExternalDownload = properties != null && properties.containsKey("externalDownloads") && Boolean.valueOf(properties.getProperty("externalDownloads"));

        if (!allowExternalDownload && latestResourceInfo.external) {
            failReason = DownloadFailReason.EXTERNAL_DISALLOWED;
            return false;
        }

        plugin.getSrLogger().logAlways("[SpigetUpdate] Downloading update...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, org.inventivetalent.update.spiget.download.UpdateDownloader.downloadAsync(latestResourceInfo, updateFile, plugin.getUpdateChecker().getUserAgent(), new DownloadCallback() {
            @Override
            public void finished() {
                plugin.getSrLogger().logAlways("[SpigetUpdate] Update saved as " + updateFile.getPath());
            }

            @Override
            public void error(Exception exception) {
                plugin.getSrLogger().logAlways(Level.WARNING, "[SpigetUpdate] Could not download update", exception);
            }
        }));

        return true;
    }

    public DownloadFailReason getFailReason() {
        return failReason;
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
                properties.store(new FileWriter(file), "Configuration for the Spiget auto-updater. https://spiget.org | https://github.com/InventivetalentDev/SpigetUpdater\n"
                        + "Use 'externalDownloads' if you want to auto-download resources hosted on external sites\n"
                        + "");
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            properties.load(new FileReader(file));
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
        if (!(plugin instanceof JavaPlugin)) {
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
        EXTERNAL_DISALLOWED,
        UNKNOWN;
    }

}
