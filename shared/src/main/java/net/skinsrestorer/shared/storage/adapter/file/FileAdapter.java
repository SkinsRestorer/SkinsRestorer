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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.adapter.file.model.cache.MojangCacheFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.LegacyPlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.PlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.skin.*;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.LegacyPlayerData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.*;
import net.skinsrestorer.shared.utils.SRFileUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileAdapter implements StorageAdapter {
    private static final String LAST_KNOW_NAME_ATTRIBUTE = "sr_last_known_name";
    private static final Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
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
        try {
            migrate(dataFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        init();
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
        SRFileUtils.renameFile(dataFolder, "Skins", "skins");
        SRFileUtils.renameFile(dataFolder, "Players", "players");

        migrateSkins();
        migratePlayers();
    }

    private void migratePlayers() {
        Path legacyPlayersFolder = legacyFolder.resolve("players");
        boolean generatedFolder = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersFolder, "*.player")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String playerName = fileName.substring(0, fileName.length() - ".player".length());

                if (UUID_REGEX.matcher(playerName).matches()) {
                    return; // If we find a UUID, we assume the migration has already been done.
                }

                if (!generatedFolder) {
                    generatedFolder = true;
                    Files.createDirectories(legacyPlayersFolder);
                    logger.info("Migrating legacy player files to new format...");
                }

                try {
                    Path legacyPlayerFile = resolveLegacyPlayerFile(playerName);
                    if (Files.exists(legacyPlayerFile)) {
                        continue;
                    }

                    String skinName = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

                    LegacyPlayerData legacyPlayerData = LegacyPlayerData.of(playerName, skinName);

                    Files.write(legacyPlayerFile, gson.toJson(LegacyPlayerFile.fromLegacyPlayerData(legacyPlayerData)).getBytes(StandardCharsets.UTF_8));

                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (generatedFolder) {
            logger.info("Player files migration complete!");
        }
    }

    private void migrateSkins() {
        Path legacySkinsFolder = legacyFolder.resolve("skins");
        boolean generatedFolder = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
            for (Path path : stream) {
                if (!generatedFolder) {
                    generatedFolder = true;
                    Files.createDirectories(legacySkinsFolder);
                    logger.info("Migrating legacy skin files to new format...");
                }

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

                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (generatedFolder) {
            logger.info("Skin files migration complete!");
        }
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

            UserDefinedFileAttributeView view = Files.getFileAttributeView(skinFile, UserDefinedFileAttributeView.class);
            view.write(LAST_KNOW_NAME_ATTRIBUTE, StandardCharsets.UTF_8.encode(skinData.getLastKnownName()));
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
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
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
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
        Path skinFile = resolveCustomSkinFile(skinName);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
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
        skinName = sanitizeLegacySkinName(skinName);
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
        skinName = sanitizeLegacySkinName(skinName);
        Path skinFile = resolveLegacySkinFile(skinName);

        try {
            Files.deleteIfExists(skinFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<LegacyPlayerData> getLegacyPlayerData(String playerName) throws StorageException {
        playerName = sanitizeLegacyPlayerName(playerName);
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
        playerName = sanitizeLegacyPlayerName(playerName);
        Path legacyFile = resolveLegacyPlayerFile(playerName);

        try {
            Files.deleteIfExists(legacyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getStoredGUISkins(int offset) {
        Map<String, String> list = new LinkedHashMap<>();

        Map<String, GUIFileData> files = getGUIFilesSorted(offset);

        int i = 0;
        for (Map.Entry<String, GUIFileData> entry : files.entrySet()) {
            if (i > SkinStorageImpl.SKINS_PER_GUI_PAGE) {
                break;
            }

            if (entry.getValue().getSkinType() == SkinType.PLAYER) {
                try {
                    getPlayerSkinData(UUID.fromString(entry.getValue().getFileName()))
                            .ifPresent(skinData -> list.put(entry.getKey(), skinData.getProperty().getValue()));
                } catch (StorageException e) {
                    e.printStackTrace();
                }
            } else if (entry.getValue().getSkinType() == SkinType.CUSTOM) {
                try {
                    getCustomSkinData(entry.getValue().getFileName())
                            .ifPresent(skinData -> list.put(entry.getKey(), skinData.getProperty().getValue()));
                } catch (StorageException e) {
                    e.printStackTrace();
                }
            }

            i++;
        }

        return list;
    }

    private Map<String, GUIFileData> getGUIFilesSorted(int offset) {
        boolean customEnabled = settings.getProperty(GUIConfig.CUSTOM_GUI_ENABLED);
        boolean customOnly = settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY);
        List<String> customSkins = settings.getProperty(GUIConfig.CUSTOM_GUI_SKINS)
                .stream()
                .map(String::toLowerCase)
                .distinct() // No duplicates
                .collect(Collectors.toList());

        int i = 0;
        Map<String, GUIFileData> files = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        try (Stream<Path> stream = Files.walk(skinsFolder, 1)) {
            for (Iterator<Path> it = stream.iterator(); it.hasNext(); ) {
                Path path = it.next();
                if (Files.isDirectory(path)) {
                    continue;
                }

                String fileName = path.getFileName().toString();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                String name = fileName.substring(0, fileName.length() - extension.length() - 1);

                boolean isPlayerSkin = extension.equals("playerskin");
                boolean isCustomSkin = extension.equals("customskin");

                // Only allow player skins and custom skins
                if (!isPlayerSkin && !isCustomSkin) {
                    continue;
                }

                // No player skins if custom skins only
                if (isPlayerSkin && customEnabled && customOnly) {
                    continue;
                }

                // Do not allow custom skins if not enabled
                if (isCustomSkin && !customEnabled) {
                    continue;
                }

                // Only allow specific custom skins if enabled
                if (customEnabled && customOnly && !customSkins.contains(name.toLowerCase())) {
                    continue;
                }

                if (i < offset) {
                    i++;
                    continue;
                }

                GUIFileData data = new GUIFileData(name, path, isPlayerSkin ? SkinType.PLAYER : SkinType.CUSTOM);
                if (isPlayerSkin) {
                    getLastKnownName(path)
                            .ifPresent(lastKnownName -> files.put(lastKnownName, data));
                } else {
                    files.put(name, data);
                }

                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return files;
    }

    private Optional<String> getLastKnownName(Path path) {
        try {
            UserDefinedFileAttributeView view = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
            if (!view.list().contains(LAST_KNOW_NAME_ATTRIBUTE)) {
                return Optional.empty();
            }

            ByteBuffer buffer = ByteBuffer.allocate(view.size(LAST_KNOW_NAME_ATTRIBUTE));
            view.read(LAST_KNOW_NAME_ATTRIBUTE, buffer);
            buffer.flip();
            return Optional.of(StandardCharsets.UTF_8.decode(buffer).toString());
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
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

    private Path resolveCustomSkinFile(String skinName) {
        return skinsFolder.resolve(skinName + ".customskin");
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

    private String sanitizeLegacyPlayerName(String playerName) {
        // The use of #toLowerCase() instead of #toLowerCase(Locale.ENGLISH) is intentional
        // This is because the legacy player names used this incorrect way of lowercasing
        return playerName.toLowerCase();
    }

    private String sanitizeLegacySkinName(String skinName) {
        // The use of #toLowerCase() instead of #toLowerCase(Locale.ENGLISH) is intentional
        // This is because the legacy skin names used this incorrect way of lowercasing
        return skinName.toLowerCase();
    }

    @Getter
    @RequiredArgsConstructor
    private static class GUIFileData {
        private final String fileName;
        private final Path path;
        private final SkinType skinType;
    }
}
