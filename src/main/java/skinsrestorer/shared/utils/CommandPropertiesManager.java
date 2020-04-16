package skinsrestorer.shared.utils;

import co.aikar.commands.*;
import co.aikar.locales.MessageKey;

import java.io.*;
import java.util.Properties;

/**
 * Created by McLive on 12.02.2019.
 */
public class CommandPropertiesManager {
    // private String path = "plugins" + File.separator + "SkinsRestorer" + File.separator;
    private String configPath;
    private InputStream inputStream;
    private String file = "command-messages.properties";

    private void copyFile() {
        File outFile = new File(this.configPath, this.file);
        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = this.inputStream.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                this.inputStream.close();
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + outFile.getName() + " to " + outFile);
            ex.printStackTrace();
        }
    }

    public CommandPropertiesManager(PaperCommandManager manager, String configPath, InputStream inputStream) {
        this.configPath = configPath;
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.configPath, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public CommandPropertiesManager(BungeeCommandManager manager, String configPath, InputStream inputStream) {
        this.configPath = configPath;
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.configPath, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public CommandPropertiesManager(VelocityCommandManager manager, String configPath, InputStream inputStream) {
        this.configPath = configPath;
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.configPath, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString().replace("&", "§")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public CommandPropertiesManager(SpongeCommandManager manager, String configPath, InputStream inputStream) {
        this.configPath = configPath + File.separator;
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.configPath, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString().replace("&", "§")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
