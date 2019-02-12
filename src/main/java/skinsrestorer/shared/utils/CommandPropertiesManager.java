package skinsrestorer.shared.utils;

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.PaperCommandManager;
import co.aikar.locales.MessageKey;

import java.io.*;
import java.util.Properties;

/**
 * Created by McLive on 12.02.2019.
 */
public class CommandPropertiesManager {
    private String path = "plugins" + File.separator + "SkinsRestorer" + File.separator;
    private String file = "command-messages.properties";
    private InputStream inputStream;
    private File outFile = new File(path, file);

    private void copyFile() {
        try {
            if (!this.outFile.exists()) {
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

    public CommandPropertiesManager(PaperCommandManager manager, InputStream inputStream) {
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.path, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public CommandPropertiesManager(BungeeCommandManager manager, InputStream inputStream) {
        this.inputStream = inputStream;
        this.copyFile();

        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new File(this.path, this.file)));
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
