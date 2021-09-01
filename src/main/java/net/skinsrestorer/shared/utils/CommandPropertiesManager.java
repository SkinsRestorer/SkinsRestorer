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
package net.skinsrestorer.shared.utils;

import co.aikar.commands.CommandManager;
import co.aikar.commands.Locales;
import co.aikar.locales.MessageKey;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.apache.any23.encoding.TikaEncodingDetector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class CommandPropertiesManager {
    private static final String FILE = "command-messages.properties";

    public static void load(CommandManager<?, ?, ?, ?, ?, ?> manager, File configPath, InputStream defaultConfigStream, SRLogger logger) {
        File outFile = new File(configPath, FILE);

        Charset usedCharset = StandardCharsets.UTF_8;
        if (outFile.exists()) {
            try (InputStream in = new FileInputStream(outFile)) {
                usedCharset = Charset.forName(new TikaEncodingDetector().guessEncoding(in));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Files.copy(defaultConfigStream, outFile.toPath());
                defaultConfigStream.close();
            } catch (IOException ex) {
                logger.warning("Could not save " + outFile.getName() + " to " + outFile);
                ex.printStackTrace();
            }
        }

        try (InputStream in = new FileInputStream(outFile)) {
            Properties props = new Properties();

            props.load(new InputStreamReader(in, usedCharset));
            props.forEach((k, v) -> manager.getLocales().addMessage(Locales.ENGLISH, MessageKey.of(k.toString()), v.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
