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
package net.skinsrestorer.shared.subjects.messages;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.subjects.SRForeign;
import net.skinsrestorer.shared.utils.LocaleParser;
import net.skinsrestorer.shared.utils.TranslationReader;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MessageLoader {
    private final SRPlugin plugin;
    private final LocaleManager<SRForeign> manager;
    private final SRPlatformAdapter<?> adapter;

    public void loadMessages() throws IOException {
        for (String localeFile : BuildData.LOCALES) {
            String filePath = "locales/" + localeFile;
            Locale locale = getTranslationLocale(localeFile);

            try (InputStream is = adapter.getResource(filePath)) {
                TranslationReader.readJsonTranslation(is)
                        .forEach((k, v) -> manager.addMessage(Message.fromKey(k), locale, v));
            }
        }

        Path localesFolder = plugin.getDataFolder().resolve("locales");
        if (Files.exists(localesFolder)) {
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
                        TranslationReader.readJsonTranslation(is)
                                .forEach((k, v) -> manager.addMessage(Message.fromKey(k), locale, v));
                    }
                }
            }
        }

        manager.verifyValid();
    }

    public void migrateOldFiles() {
        Path archive = plugin.getDataFolder().resolve("Archive");

        Path oldMessagesFile = plugin.getDataFolder().resolve("messages.yml");
        if (Files.exists(oldMessagesFile)) {
            try {
                Files.createDirectories(archive);
                String newName = "old-messages-" + System.currentTimeMillis() / 1000 + ".yml";
                Files.move(oldMessagesFile, archive.resolve(newName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Path oldAcf = plugin.getDataFolder().resolve("command-messages.properties");
        if (Files.exists(oldAcf)) {
            try {
                Files.createDirectories(archive);
                Files.move(oldAcf, archive.resolve("command-messages.properties"));
            } catch (Exception e) {
                e.printStackTrace();
            }
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
