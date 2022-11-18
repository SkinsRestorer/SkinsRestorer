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
package net.skinsrestorer.shared.storage.adapter.file;

import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;

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
    public static final Pattern SAFE_PATH_PATTERN = Pattern.compile("[A-za-z0-9._-]");
    private final Path skinsFolder;
    private final Path playersFolder;

    public FileAdapter(Path dataFolder) throws IOException {
        skinsFolder = dataFolder.resolve("Skins");
        playersFolder = dataFolder.resolve("Players");
    }

    @Override
    public Optional<String> getStoredSkinNameOfPlayer(String playerName) {
        Optional<PlayerStorageType> data = getPlayerStorageData(playerName);

        return data.map(PlayerStorageType::getSkinName);
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
                skinName = LegacyFileHelper.removeWhitespaces(skinName); // TODO: Remove after legacy migration is done
                skinName = LegacyFileHelper.replaceForbiddenChars(skinName); // TODO: Remove after legacy migration is done

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

        int i = 0;
        for (String skinName : skinNames) {
            if (list.size() >= 36)
                break;

            if (i >= offset) {
                if (Config.CUSTOM_GUI_ONLY) { // Show only Config.CUSTOM_GUI_SKINS in the gui
                    for (String guiSkins : Config.CUSTOM_GUI_SKINS) {
                        if (skinName.toLowerCase().contains(guiSkins.toLowerCase()))
                            getStoredSkinData(skinName).ifPresent(property -> list.put(skinName.toLowerCase(), property.getValue()));
                    }
                } else {
                    getStoredSkinData(skinName).ifPresent(property -> list.put(skinName.toLowerCase(), property.getValue()));
                }
            }
            i++;
        }
        return list;
    }

    @Override
    public Optional<Long> getStoredTimestamp(String skinName) {
        Optional<SkinStorageType> data = getSkinStorageData(skinName);

        return data.map(SkinStorageType::getTimestamp);
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) throws StorageException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
            for (Path file : stream) {
                try {
                    Optional<SkinStorageType> data = getSkinStorageData(file);
                    if (!data.isPresent())
                        continue;

                    if (data.get().getTimestamp() < targetPurgeTimestamp) {
                        Files.deleteIfExists(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new StorageException(e);
        }
    }

    private Optional<PlayerStorageType> getPlayerStorageData(String playerName) {
        return LegacyFileHelper.readLegacyPlayerFile(LegacyFileHelper.resolveLegacyPlayerFile(playersFolder, playerName));
    }

    private Optional<SkinStorageType> getSkinStorageData(String skinName) {
        return getSkinStorageData(LegacyFileHelper.resolveLegacySkinFile(skinsFolder, skinName));
    }

    private Optional<SkinStorageType> getSkinStorageData(Path file) {
        return LegacyFileHelper.readLegacySkinFile(file);
    }

    private Path resolvePlayerFile(String playerName) {
        return LegacyFileHelper.resolveLegacyPlayerFile(playersFolder, playerName);
    }

    private Path resolveSkinFile(String skinName) {
        return LegacyFileHelper.resolveLegacySkinFile(skinsFolder, skinName);
    }
}
