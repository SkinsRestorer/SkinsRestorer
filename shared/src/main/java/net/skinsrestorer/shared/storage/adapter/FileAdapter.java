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
package net.skinsrestorer.shared.storage.adapter;

import ch.jalu.configme.SettingsManager;
import net.skinsrestorer.shared.config.StorageConfig;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileAdapter implements StorageAdapter {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\\\/:*\"<>|.\\?]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private final Path skinsFolder;
    private final Path playersFolder;
    private final SettingsManager settings;

    public FileAdapter(Path dataFolder, SettingsManager settings) throws IOException {
        skinsFolder = dataFolder.resolve("Skins");
        Files.createDirectories(skinsFolder);

        playersFolder = dataFolder.resolve("Players");
        Files.createDirectories(playersFolder);
        this.settings = settings;
    }

    @Override
    public Optional<String> getStoredSkinNameOfPlayer(String playerName) {
        Path playerFile = resolvePlayerFile(playerName);

        try {
            if (!Files.exists(playerFile))
                return Optional.empty();

            List<String> lines = Files.readAllLines(playerFile);

            if (lines.isEmpty()) {
                removeStoredSkinNameOfPlayer(playerName);
                return Optional.empty();
            }

            return Optional.of(lines.get(0));
        } catch (MalformedInputException e) {
            removeStoredSkinNameOfPlayer(playerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void removeStoredSkinNameOfPlayer(String playerName) {
        Path playerFile = resolvePlayerFile(playerName);

        try {
            Files.deleteIfExists(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setStoredSkinNameOfPlayer(String playerName, String skinName) {
        Path playerFile = resolvePlayerFile(playerName);

        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(playerFile), StandardCharsets.UTF_8)) {
                skinName = removeWhitespaces(skinName);
                skinName = replaceForbiddenChars(skinName);

                writer.write(skinName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<StoredProperty> getStoredSkinData(String skinName) {
        Path skinFile = resolveSkinFile(skinName);

        try {
            if (!Files.exists(skinFile))
                return Optional.empty();

            List<String> lines = Files.readAllLines(skinFile);

            String value = lines.get(0);
            String signature = lines.get(1);
            String timestamp = lines.get(2);

            return Optional.of(new StoredProperty(value, signature, Long.parseLong(timestamp)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void removeStoredSkinData(String skinName) {
        Path skinFile = resolveSkinFile(skinName);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setStoredSkinData(String skinName, StoredProperty storedProperty) {
        Path skinFile = resolveSkinFile(skinName);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(skinFile), StandardCharsets.UTF_8)) {
            writer.write(storedProperty.getValue() + "\n" + storedProperty.getSignature() + "\n" + storedProperty.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getStoredSkins(int offset) {
        Map<String, String> list = new TreeMap<>();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
            stream.forEach(files::add);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> skinNames = files.stream().map(Path::getFileName).map(Path::toString).map(s ->
                s.substring(0, s.length() - 5) // remove .skin (5 characters)
        ).sorted().collect(Collectors.toList());

        if (settings.getProperty(StorageConfig.CUSTOM_GUI_ENABLED)) {
            List<String> customSkinNames = settings.getProperty(StorageConfig.CUSTOM_GUI_SKINS);
            if (settings.getProperty(StorageConfig.CUSTOM_GUI_ONLY)) {
                skinNames = skinNames.stream().filter(customSkinNames::contains).collect(Collectors.toList());
            } else {
                skinNames = skinNames.stream().sorted((s1, s2) -> {
                    boolean s1Custom = customSkinNames.contains(s1);
                    boolean s2Custom = customSkinNames.contains(s2);
                    if (s1Custom && s2Custom) {
                        return s1.compareTo(s2);
                    } else if (s1Custom) {
                        return -1;
                    } else if (s2Custom) {
                        return 1;
                    } else {
                        return s1.compareTo(s2);
                    }
                }).collect(Collectors.toList());
            }
        }

        int i = 0;
        for (String skinName : skinNames) {
            if (list.size() >= 36)
                break;

            if (i < offset) {
                continue;
            }

            getStoredSkinData(skinName).ifPresent(property -> list.put(skinName.toLowerCase(), property.getValue()));
            i++;
        }
        return list;
    }

    @Override
    public Optional<Long> getStoredTimestamp(String skinName) {
        Path skinFile = resolveSkinFile(skinName);

        try {
            if (!Files.exists(skinFile)) {
                return Optional.empty();
            }

            List<String> lines = Files.readAllLines(skinFile);

            if (lines.size() < 3)
                return Optional.empty();

            return Optional.of(Long.parseLong(lines.get(2)));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) throws StorageException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
            for (Path file : stream) {
                try {
                    if (!Files.exists(file))
                        continue;

                    List<String> lines = Files.readAllLines(file);
                    long timestamp = Long.parseLong(lines.get(2));

                    if (timestamp != 0L && timestamp < targetPurgeTimestamp) {
                        Files.deleteIfExists(file);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new StorageException(e);
        }
    }

    private Path resolveSkinFile(String skinName) {
        skinName = removeWhitespaces(skinName);
        skinName = replaceForbiddenChars(skinName);
        return skinsFolder.resolve(skinName + ".skin");
    }

    private Path resolvePlayerFile(String playerName) {
        playerName = replaceForbiddenChars(playerName);
        return playersFolder.resolve(playerName + ".player");
    }

    private String replaceForbiddenChars(String str) {
        // Escape all Windows / Linux forbidden printable ASCII characters
        return FORBIDDEN_CHARS_PATTERN.matcher(str).replaceAll("·");
    }

    // TODO remove all whitespace after last starting space.
    private String removeWhitespaces(String str) {
        // Remove all whitespace expect when startsWith " ".
        if (str.startsWith(" ")) {
            return str;
        }
        return WHITESPACE_PATTERN.matcher(str).replaceAll("");
    }
}
