/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.subjects.messages;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.LocaleParser;
import net.skinsrestorer.shared.utils.SRFileUtils;
import net.skinsrestorer.shared.utils.TranslationReader;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MessageLoader {
    private final SRPlugin plugin;
    private final LocaleManager manager;
    private final SRPlatformAdapter<?> adapter;
    private final SRLogger logger;

    public void loadMessages() throws IOException {
        for (String localeFile : BuildData.LOCALES) {
            String filePath = "locales/" + localeFile;
            Locale locale = getTranslationLocale(localeFile);

            try (InputStream is = adapter.getResource(filePath)) {
                for (Map.Entry<String, String> entry : TranslationReader.readJsonTranslation(is).entrySet()) {
                    logger.debug(String.format("Loaded message '%s' for locale %s", entry.getKey(), locale));
                    manager.addMessage(Message.fromKey(entry.getKey()), locale, entry.getValue());
                }
            }
        }

        Path localesFolder = plugin.getDataFolder().resolve("locales");
        if (Files.exists(localesFolder)) {
            logger.info("Found locales folder, loading custom translations...");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(localesFolder)) {
                for (Path path : stream) {
                    Path localeFile = path.getFileName();
                    if (localeFile == null) {
                        continue;
                    }

                    String fileName = localeFile.toString();
                    if (!isJson(fileName)) {
                        continue;
                    }

                    Locale locale = getTranslationLocale(fileName);

                    try (InputStream is = Files.newInputStream(path)) {
                        for (Map.Entry<String, String> entry : TranslationReader.readJsonTranslation(is).entrySet()) {
                            logger.debug(String.format("Loaded custom message '%s' for locale %s", entry.getKey(), locale));
                            manager.addMessage(Message.fromKey(entry.getKey()), locale, entry.getValue());
                        }
                    }
                }
            }
        }

        manager.verifyValid();
    }

    public void moveOldFiles() {
        Path dataFolder = plugin.getDataFolder();

        try {
            SRFileUtils.renameFile(dataFolder, "Archive", "archive"); // Now lowercase
        } catch (IOException e) {
            e.printStackTrace();
        }
        moveToArchive(dataFolder.resolve("messages.yml"));
        moveToArchive(dataFolder.resolve("command-messages.properties"));
        moveToArchive(dataFolder.resolve("languages"));
    }

    private void moveToArchive(Path path) {
        if (!Files.exists(path)) {
            return;
        }

        Path archive = plugin.getDataFolder().resolve("archive");
        try {
            Files.createDirectories(archive);
            Files.move(path, archive.resolve(path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isJson(String fileName) {
        return fileName.endsWith(".json");
    }

    private Locale getTranslationLocale(String fileName) {
        return fileName.startsWith("locale_") ? LocaleParser.parseLocaleStrict(stripFileFormat(fileName)) : Locale.ENGLISH;
    }

    private String stripFileFormat(String fileName) {
        return fileName.replace("locale_", "").replace(".json", "");
    }
}
