/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.subjects.messages;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.LocaleParser;
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
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MessageLoader {
    private final SRPlugin plugin;
    private final LocaleManager manager;
    private final SRPlatformAdapter adapter;
    private final SRLogger logger;

    public void loadMessages() throws IOException {
        loadDefaultMessages();

        loadCustomMessages();

        manager.verifyValid();
    }

    private void loadDefaultMessages() throws IOException {
        for (String localeFile : BuildData.LOCALES) {
            String resourcePath = "locales/" + localeFile;
            Locale locale = getTranslationLocale(localeFile);

            int count = 0;
            try (InputStream is = adapter.getResource(resourcePath)) {
                for (Map.Entry<String, String> entry : TranslationReader.readJsonTranslation(is).entrySet()) {
                    var message = Message.fromKey(entry.getKey());
                    if (message.isEmpty() && locale != LocaleManager.BASE_LOCALE) {
                        continue;
                    }

                    manager.addMessage(
                            message
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            String.format("No message enum found for key %s", entry.getKey())
                                    )),
                            locale,
                            entry.getValue()
                    );
                    count++;
                }
            }

            logger.debug(String.format("Loaded %d default message strings for locale %s", count, locale));
        }
    }

    private void loadCustomMessages() throws IOException {
        Path localesFolder = plugin.getDataFolder().resolve("locales");
        Path repositoryFolder = localesFolder.resolve("repository");
        Path customFolder = localesFolder.resolve("custom");

        Files.createDirectories(repositoryFolder);
        Files.createDirectories(customFolder);

        for (String localeFile : BuildData.LOCALES) {
            String resourcePath = "locales/" + localeFile;
            Path filePath = repositoryFolder.resolve(localeFile);

            try (InputStream is = adapter.getResource(resourcePath)) {
                Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(customFolder)) {
            boolean found = false;
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                }

                Path localeFile = path.getFileName();
                if (localeFile == null) {
                    continue;
                }

                String fileName = localeFile.toString();
                if (!isJson(fileName)) {
                    continue;
                }

                if (!found) {
                    logger.info("Found custom translations, loading...");
                    found = true;
                }

                Locale locale = getTranslationLocale(fileName);

                int count = 0;
                try (InputStream is = Files.newInputStream(path)) {
                    for (Map.Entry<String, String> entry : TranslationReader.readJsonTranslation(is).entrySet()) {
                        Optional<Message> message = Message.fromKey(entry.getKey());
                        if (message.isEmpty()) {
                            logger.warning("Skipping unknown message key " + entry.getKey());
                            continue;
                        }

                        manager.addMessage(message.get(), locale, entry.getValue());
                        count++;
                    }
                }

                logger.debug(String.format("Loaded %d custom message strings for locale %s", count, locale));
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
