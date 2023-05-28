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
package net.skinsrestorer.shared.storage.adapter.file;

import ch.jalu.configme.SettingsManager;
import com.google.gson.Gson;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.adapter.file.model.cache.MojangCacheFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.LegacyPlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.PlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.skin.*;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.LegacyPlayerData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.*;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class FileAdapter implements StorageAdapter {
    private final Path skinsFolder;
    private final Path playersFolder;
    private final Path cacheFolder;
    private final Path legacyFolder;
    private final SettingsManager settings;
    private final Gson gson = new Gson();
    private final SRLogger logger;

    @Inject
    public FileAdapter(SRPlugin plugin, SettingsManager settings, SRLogger logger) {
        Path dataFolder = plugin.getDataFolder();
        this.skinsFolder = dataFolder.resolve("skins");
        this.playersFolder = dataFolder.resolve("players");
        this.cacheFolder = dataFolder.resolve("cache");
        this.legacyFolder = dataFolder.resolve("legacy");
        this.settings = settings;
        this.logger = logger;
        init();
        try {
            migrate(dataFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(skinsFolder);
            Files.createDirectories(playersFolder);
            Files.createDirectories(cacheFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void migrate(Path dataFolder) throws IOException {
        // Legacy support
        Path legacySkinsFolder = legacyFolder.resolve("skins");
        Path legacyPlayersFolder = legacyFolder.resolve("players");
        Path oldSkinsFolder = dataFolder.resolve("Skins");
        Path oldPlayersFolder = dataFolder.resolve("Players");

        if (Files.exists(oldSkinsFolder)) {
            Files.createDirectories(legacySkinsFolder);
            migrateSkins(oldSkinsFolder);
        }

        if (Files.exists(oldPlayersFolder)) {
            Files.createDirectories(legacyPlayersFolder);
            migratePlayers(oldPlayersFolder);
        }
    }

    private void migratePlayers(Path oldPlayersFolder) {
        logger.info("Migrating legacy player files to new format...");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(oldPlayersFolder, "*.player")) {
            for (Path path : stream) {
                try {
                    String fileName = path.getFileName().toString();
                    String playerName = fileName.substring(0, fileName.length() - ".player".length());

                    Path legacyPlayerFile = resolveLegacyPlayerFile(playerName);
                    if (Files.exists(legacyPlayerFile)) {
                        continue;
                    }

                    String skinName = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

                    LegacyPlayerData legacyPlayerData = LegacyPlayerData.of(playerName, skinName);

                    Files.write(legacyPlayerFile, gson.toJson(LegacyPlayerFile.fromLegacyPlayerData(legacyPlayerData)).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Files.deleteIfExists(oldPlayersFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Migrating legacy player files to new format... Done!");
    }

    private void migrateSkins(Path oldSkinsFolder) {
        logger.info("Migrating legacy skin table to new format...");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(oldSkinsFolder, "*.skin")) {
            for (Path path : stream) {
                try {
                    String fileName = path.getFileName().toString();
                    String skinName = fileName.substring(0, fileName.length() - ".skin".length());

                    Path legacySkinFile = resolveLegacySkinFile(skinName);
                    if (Files.exists(legacySkinFile)) {
                        continue;
                    }

                    String[] lines = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).split("\n");

                    LegacySkinData legacySkinData = LegacySkinData.of(skinName, SkinProperty.of(lines[0], lines[1]));

                    Files.write(legacySkinFile, gson.toJson(LegacySkinFile.fromLegacySkinData(legacySkinData)).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Files.deleteIfExists(oldSkinsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Skin migration complete!");
    }

    private static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public Optional<PlayerData> getPlayerData(UUID uuid) throws StorageException {
        Path playerFile = resolvePlayerFile(uuid);

        if (!Files.exists(playerFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(playerFile), StandardCharsets.UTF_8);
            PlayerFile file = gson.fromJson(json, PlayerFile.class);

            return Optional.of(file.toPlayerData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setPlayerData(UUID uuid, PlayerData data) {
        Path playerFile = resolvePlayerFile(uuid);

        try {
            PlayerFile file = PlayerFile.fromPlayerData(data);

            Files.write(playerFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException {
        Path skinFile = resolvePlayerSkinFile(uuid);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(skinFile), StandardCharsets.UTF_8);

            PlayerSkinFile file = gson.fromJson(json, PlayerSkinFile.class);

            return Optional.of(file.toPlayerSkinData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removePlayerSkinData(UUID uuid) {
        Path skinFile = resolvePlayerSkinFile(uuid);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, PlayerSkinData skinData) {
        Path skinFile = resolvePlayerSkinFile(uuid);

        try {
            PlayerSkinFile file = PlayerSkinFile.fromPlayerSkinData(skinData);

            Files.write(skinFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<URLSkinData> getURLSkinData(String url, SkinVariant skinVariant) throws StorageException {
        Path skinFile = resolveURLSkinFile(url, skinVariant);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(skinFile), StandardCharsets.UTF_8);

            URLSkinFile file = gson.fromJson(json, URLSkinFile.class);

            return Optional.of(file.toURLSkinData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeURLSkinData(String url, SkinVariant skinVariant) {
        Path skinFile = resolveURLSkinFile(url, skinVariant);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setURLSkinData(String url, URLSkinData skinData) {
        Path skinFile = resolveURLSkinFile(url, skinData.getSkinVariant());

        try {
            URLSkinFile file = URLSkinFile.fromURLSkinData(skinData);

            Files.write(skinFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<URLIndexData> getURLSkinIndex(String url) throws StorageException {
        Path skinFile = resolveURLSkinIndexFile(url);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(skinFile), StandardCharsets.UTF_8);

            URLIndexFile file = gson.fromJson(json, URLIndexFile.class);

            return Optional.of(file.toURLIndexData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeURLSkinIndex(String url) {
        Path skinFile = resolveURLSkinIndexFile(url);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setURLSkinIndex(String url, URLIndexData skinData) {
        Path skinFile = resolveURLSkinIndexFile(url);

        try {
            URLIndexFile file = URLIndexFile.fromURLIndexData(skinData);

            Files.write(skinFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<CustomSkinData> getCustomSkinData(String skinName) throws StorageException {
        Path skinFile = resolveCustomSkinFile(skinName);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(skinFile), StandardCharsets.UTF_8);

            CustomSkinFile file = gson.fromJson(json, CustomSkinFile.class);

            return Optional.of(file.toCustomSkinData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeCustomSkinData(String skinName) {
        Path skinFile = resolveCustomSkinFile(skinName);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {
        Path skinFile = resolveCustomSkinFile(skinName);

        try {
            CustomSkinFile file = CustomSkinFile.fromCustomSkinData(skinData);

            Files.write(skinFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<LegacySkinData> getLegacySkinData(String skinName) throws StorageException {
        Path skinFile = resolveLegacySkinFile(skinName);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(skinFile), StandardCharsets.UTF_8);

            LegacySkinFile file = gson.fromJson(json, LegacySkinFile.class);

            return Optional.of(file.toLegacySkinData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeLegacySkinData(String skinName) {
        Path skinFile = resolveLegacySkinFile(skinName);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* // OLD METHOD
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

        if (settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED)) {
            List<String> customSkinNames = settings.getProperty(GUIConfig.CUSTOM_GUI_SKINS);
            if (settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY)) {
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

            if (settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY)) { // Show only Config.CUSTOM_GUI_SKINS in the gui
                for (String guiSkins : settings.getProperty(GUIConfig.CUSTOM_GUI_SKINS)) {
                    if (skinName.toLowerCase().contains(guiSkins.toLowerCase())) {
                        try {
                            getCustomSkinData(skinName).ifPresent(property -> list.put(skinName.toLowerCase(), property.getProperty().getValue()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                try {
                    getCustomSkinData(skinName).ifPresent(property -> list.put(skinName.toLowerCase(), property.getProperty().getValue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            i++;
        }

        return list;
    }*/

    @Override
    public Map<String, String> getStoredGUISkins(int offset) {
        throw new UnsupportedOperationException("getStoredGUISkins is not supported by this storage method");
    }

    @Override
    public void purgeStoredOldSkins(long targetPurgeTimestamp) throws StorageException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.playerskin")) {
            stream.forEach(files::add);
        } catch (IOException e) {
            throw new StorageException(e);
        }

        for (Path file : files) {
            try {
                String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

                PlayerSkinFile skinFile = gson.fromJson(json, PlayerSkinFile.class);

                if (skinFile.getTimestamp() != 0L && skinFile.getTimestamp() < targetPurgeTimestamp) {
                    Files.deleteIfExists(file);
                }
            } catch (Exception e) {
                throw new StorageException(e);
            }
        }
    }

    @Override
    public Optional<MojangCacheData> getCachedUUID(String playerName) throws StorageException {
        Path cacheFile = resolveCacheFile(playerName);

        if (!Files.exists(cacheFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);

            MojangCacheFile file = gson.fromJson(json, MojangCacheFile.class);

            return Optional.of(file.toCacheData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setCachedUUID(String playerName, MojangCacheData mojangCacheData) {
        Path cacheFile = resolveCacheFile(playerName);

        try {
            MojangCacheFile file = MojangCacheFile.fromMojangCacheData(mojangCacheData);

            Files.write(cacheFile, gson.toJson(file).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<LegacyPlayerData> getLegacyPlayerData(String playerName) throws StorageException {
        Path legacyFile = resolveLegacyPlayerFile(playerName);

        if (!Files.exists(legacyFile)) {
            return Optional.empty();
        }

        try {
            String json = new String(Files.readAllBytes(legacyFile), StandardCharsets.UTF_8);

            LegacyPlayerFile file = gson.fromJson(json, LegacyPlayerFile.class);

            return Optional.of(file.toLegacyPlayerData());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void removeLegacyPlayerData(String playerName) {
        Path legacyFile = resolveLegacyPlayerFile(playerName);

        try {
            Files.deleteIfExists(legacyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Path resolveCustomSkinFile(String skinName) {
        return skinsFolder.resolve(hashSHA256(skinName) + ".customskin");
    }

    private Path resolveLegacySkinFile(String skinName) {
        return legacyFolder.resolve("skins").resolve(skinName + ".legacyskin");
    }

    private Path resolveURLSkinFile(String url, SkinVariant skinVariant) {
        return skinsFolder.resolve(hashSHA256(url) + "_" + skinVariant.name() + ".urlskin");
    }

    private Path resolveURLSkinIndexFile(String url) {
        return skinsFolder.resolve(hashSHA256(url) + ".urlindex");
    }

    private Path resolvePlayerSkinFile(UUID uuid) {
        return skinsFolder.resolve(uuid + ".playerskin");
    }

    private Path resolvePlayerFile(UUID uuid) {
        return playersFolder.resolve(uuid + ".player");
    }

    private Path resolveLegacyPlayerFile(String name) {
        return legacyFolder.resolve("players").resolve(name + ".legacyplayer");
    }

    private Path resolveCacheFile(String name) {
        return cacheFolder.resolve(name + ".mojangcache");
    }
}
