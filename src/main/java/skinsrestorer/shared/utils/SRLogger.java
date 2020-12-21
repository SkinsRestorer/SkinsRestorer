package skinsrestorer.shared.utils;

import skinsrestorer.shared.storage.Config;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by McLive on 21.07.2019.
 */
public class SRLogger {
    private File folder;
    private java.util.logging.Logger logger;

    public SRLogger(File pluginFolder) {
        folder = pluginFolder;
        this.load();
    }

    public boolean load() {
        try {
            //Manual check config value
            File pluginConfigFile = new File(folder.getAbsolutePath() + File.separator +"config.yml");
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
            return true;
        }
        logger = Logger.getLogger("");
        return true;
    }


    public Object getLogger() {
        return this.logger;
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
        this.logger.log(level, "§e[§2SkinsRestorer§e] §r" + message);
    }

    public void logAlways(Level level, String message, Throwable thrown) {
        this.logger.log(level, "§e[§2SkinsRestorer§e] §r" + message, thrown);
    }

}
