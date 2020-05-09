package skinsrestorer.shared.utils;

import skinsrestorer.shared.storage.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by McLive on 21.07.2019.
 */
public class SRLogger {
    private java.util.logging.Logger logger;

    public SRLogger() {
        this.load();
    }

    public boolean load() {
        logger = Logger.getLogger(SRLogger.class.getName());
        return true;

        /*
        try {
            logger = ReflectionUtil.invokeMethod(Class.forName("org.bukkit.Bukkit"), null, "getLogger");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }*/
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
        this.logger.log(level, "[SkinsRestorer] " + message);
    }

    public void logAlways(Level level, String message, Throwable thrown) {
        this.logger.log(level, "[SkinsRestorer] " + message, thrown);
    }

}
