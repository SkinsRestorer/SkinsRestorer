package net.skinsrestorer.shared.utils;

import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.YamlConfig;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by McLive on 21.07.2019.
 */
public class SRLogger {
    private final File folder;
    private java.util.logging.Logger logger;

    public SRLogger(File pluginFolder) {
        folder = pluginFolder;
        load();
    }

    private void load() {
        try {
            //Manual check config value
            File pluginConfigFile = new File(folder.getAbsolutePath() + File.separator + "config.yml");
            YamlConfig pluginConfig = new YamlConfig(folder.getAbsolutePath() + File.separator, "config", false);

            if (pluginConfigFile.exists()) {
                pluginConfig.reload();
                if (pluginConfig.getBoolean("Debug")) {
                    Config.DEBUG = true;
                }
            }
        } catch (Exception ignored) {
        }

        if (Config.DEBUG) {
            logger = Logger.getLogger(SRLogger.class.getName());
        } else {
            logger = Logger.getLogger("");
        }
    }

    public void log(String message) {
        this.log(Level.INFO, message);
    }

    public void logAlways(String message) {
        this.logAlways(Level.INFO, message);
    }

    public void log(Level level, String message, Throwable thrown) {
        if (!Config.DEBUG)
            return;

        this.logAlways(level, message, thrown);
    }

    public void log(Level level, String message) {
        if (!Config.DEBUG)
            return;

        this.logAlways(level, message);
    }

    public void logAlways(Level level, String message) {
        logger.log(level, "§e[§2SkinsRestorer§e] §r" + message);
    }

    public void logAlways(Level level, String message, Throwable thrown) {
        logger.log(level, "§e[§2SkinsRestorer§e] §r" + message, thrown);
    }
}
