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
package net.skinsrestorer.shared.utils;

import co.aikar.commands.BungeeCommandManager;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.SpongeCommandManager;
import co.aikar.commands.VelocityCommandManager;
import co.aikar.locales.MessageKey;

import java.io.*;
import java.util.Properties;

public class CommandPropertiesManager {
    private final String configPath;
    private final InputStream inputStream;
    private static final String FILE = "command-messages.properties";

    private void copyFile() {
        File outFile = new File(this.configPath, FILE);

        try {
            if (!outFile.exists()) {
                try (OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = this.inputStream.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }

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
        try (InputStream in = new FileInputStream(new File(this.configPath, FILE))) {
            props.load(in);
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
        try (InputStream in = new FileInputStream(new File(this.configPath, FILE))) {
            props.load(in);
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
        try (InputStream in = new FileInputStream(new File(this.configPath, FILE))) {
            props.load(in);
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
        try (InputStream in = new FileInputStream(new File(this.configPath, FILE))) {
            props.load(in);
            props.forEach((k, v) -> manager.getLocales().addMessage(co.aikar.commands.Locales.ENGLISH, MessageKey.of(k.toString()), v.toString().replace("&", "§")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
