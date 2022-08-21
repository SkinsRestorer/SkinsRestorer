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
package net.skinsrestorer.shared.utils;

import co.aikar.commands.CommandManager;
import co.aikar.commands.Locales;
import co.aikar.locales.MessageKey;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommandPropertiesManager {
    private static final String FILE = "command.properties";

    public static void load(CommandManager<?, ?, ?, ?, ?, ?> manager, Path dataFolder, InputStream defaultConfigStream, SRLogger logger) {
        Path outFile = dataFolder.resolve(FILE);

        if (!Files.exists(outFile)) {
            try {
                Files.copy(defaultConfigStream, outFile);
            } catch (IOException ex) {
                logger.warning("Could not save " + outFile.getFileName() + " to " + outFile);
                ex.printStackTrace();
            }
        }

        try {
            defaultConfigStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try (InputStream in = Files.newInputStream(outFile)) {
            PropertyReader.readProperties(in).forEach((k, v) -> manager.getLocales().addMessage(Locales.ENGLISH, MessageKey.of(k.toString()), C.c(v.toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
