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
package net.skinsrestorer.shared.storage.adapter.file;

import ch.jalu.configme.SettingsManager;
import com.google.gson.*;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.config.GUIConfig;
import net.skinsrestorer.shared.gui.GUIUtils;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.adapter.file.model.cache.MojangCacheFile;
import net.skinsrestorer.shared.storage.adapter.file.model.cooldown.CooldownFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.LegacyPlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.player.PlayerFile;
import net.skinsrestorer.shared.storage.adapter.file.model.skin.*;
import net.skinsrestorer.shared.storage.model.cache.MojangCacheData;
import net.skinsrestorer.shared.storage.model.player.LegacyPlayerData;
import net.skinsrestorer.shared.storage.model.player.PlayerData;
import net.skinsrestorer.shared.storage.model.skin.*;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.UUIDUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileAdapter implements StorageAdapter {
    private final Path skinsFolder;
    private final Path playersFolder;
    private final Path cooldownsFolder;
    private final Path cacheFolder;
    private final Path legacyFolder;
    private final SettingsManager settings;
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(ComponentString.class, new ComponentStringSerializer())
            .create();
    private final SRLogger logger;

    @Inject
    public FileAdapter(SRPlugin plugin, SettingsManager settings, SRLogger logger) {
        Path dataFolder = plugin.getDataFolder();
        this.skinsFolder = dataFolder.resolve("skins");
        this.playersFolder = dataFolder.resolve("players");
        this.cooldownsFolder = dataFolder.resolve("cooldowns");
        this.cacheFolder = dataFolder.resolve("cache");
        this.legacyFolder = dataFolder.resolve("legacy");
        this.settings = settings;
        this.logger = logger;
        try {
            migrate(dataFolder);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        init();
    }

    @Override
    public void init() {
        SRHelpers.createDirectoriesSafe(skinsFolder);
        SRHelpers.createDirectoriesSafe(playersFolder);
        SRHelpers.createDirectoriesSafe(cooldownsFolder);
        SRHelpers.createDirectoriesSafe(cacheFolder);
    }

    private void migrate(Path dataFolder) throws IOException {
        SRHelpers.renameFile(dataFolder, "Skins", "skins");
        SRHelpers.renameFile(dataFolder, "Players", "players");

        migrateSkins();
        migratePlayers();
    }

    private void migratePlayers() {
        if (!Files.exists(playersFolder)) {
            return;
        }

        Path legacyPlayersFolder = legacyFolder.resolve("players");
        boolean generatedFolder = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersFolder, "*.player")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String playerName = fileName.substring(0, fileName.length() - ".player".length());

                if (UUIDUtils.tryParseUniqueId(playerName).isPresent()) {
                    return; // If we find a UUID, we assume the migration has already been done.
                }

                if (!generatedFolder) {
                    generatedFolder = true;
                    SRHelpers.createDirectoriesSafe(legacyPlayersFolder);
                    logger.info("Migrating legacy player files to new format...");
                }

                try {
                    Path legacyPlayerFile = resolveLegacyPlayerFile(playerName);
                    if (Files.exists(legacyPlayerFile)) {
                        continue;
                    }

                    String skinName = Files.readString(path);

                    LegacyPlayerData legacyPlayerData = LegacyPlayerData.of(playerName, skinName);

                    SRHelpers.writeIfNeeded(legacyPlayerFile, gson.toJson(LegacyPlayerFile.fromLegacyPlayerData(legacyPlayerData)));

                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    logger.warning("Failed to migrate legacy player file: " + path.getFileName(), e);
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to migrate legacy player files", e);
        }

        if (generatedFolder) {
            logger.info("Player files migration complete!");
        }
    }

    private void migrateSkins() {
        if (!Files.exists(skinsFolder)) {
            return;
        }

        Path legacySkinsFolder = legacyFolder.resolve("skins");
        boolean generatedFolder = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
            for (Path path : stream) {
                if (!generatedFolder) {
                    generatedFolder = true;
                    SRHelpers.createDirectoriesSafe(legacySkinsFolder);
                    logger.info("Migrating legacy skin files to new format...");
                }

                try {
                    String fileName = path.getFileName().toString();
                    String skinName = fileName.substring(0, fileName.length() - ".skin".length());

                    Path legacySkinFile = resolveLegacySkinFile(skinName);
                    if (Files.exists(legacySkinFile)) {
                        continue;
                    }

                    String[] lines = Files.readString(path).split("\n");
                    String skinValue = lines[0].trim();
                    String skinSignature = lines[1].trim();
                    SkinProperty skinProperty = SkinProperty.of(skinValue, skinSignature);

                    // Remove this logic in like 50 years ;)
                    if (lines.length == 2 || isLegacyCustomSkinTimestamp(Long.parseLong(lines[2].trim()))) {
                        setCustomSkinData(skinName, CustomSkinData.of(skinName, null, skinProperty));
                    } else {
                        LegacySkinData legacySkinData = LegacySkinData.of(skinName, skinProperty);

                        SRHelpers.writeIfNeeded(legacySkinFile, gson.toJson(LegacySkinFile.fromLegacySkinData(legacySkinData)));
                    }

                    Files.deleteIfExists(path);
                } catch (Exception e) {
                    logger.warning("Failed to migrate legacy skin file: " + path.getFileName(), e);
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to migrate legacy skin files", e);
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
            String json = Files.readString(playerFile);
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

            SRHelpers.writeIfNeeded(playerFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save player data for " + uuid, e);
        }
    }

    @Override
    public Optional<PlayerSkinData> getPlayerSkinData(UUID uuid) throws StorageException {
        Path skinFile = resolvePlayerSkinFile(uuid);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(skinFile);

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
            logger.warning("Failed to remove player skin data for " + uuid, e);
        }
    }

    @Override
    public void setPlayerSkinData(UUID uuid, PlayerSkinData skinData) {
        Path skinFile = resolvePlayerSkinFile(uuid);

        try {
            PlayerSkinFile file = PlayerSkinFile.fromPlayerSkinData(skinData);

            SRHelpers.writeIfNeeded(skinFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save player skin data for " + uuid, e);
        }
    }

    @Override
    public Optional<URLSkinData> getURLSkinData(String url, SkinVariant skinVariant) throws StorageException {
        Path skinFile = resolveURLSkinFile(url, skinVariant);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(skinFile);

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
            logger.warning("Failed to remove URL skin data for " + url, e);
        }
    }

    @Override
    public void setURLSkinData(String url, URLSkinData skinData) {
        Path skinFile = resolveURLSkinFile(url, skinData.getSkinVariant());

        try {
            URLSkinFile file = URLSkinFile.fromURLSkinData(skinData);

            SRHelpers.writeIfNeeded(skinFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save URL skin data for " + url, e);
        }
    }

    @Override
    public Optional<URLIndexData> getURLSkinIndex(String url) throws StorageException {
        Path skinFile = resolveURLSkinIndexFile(url);

        if (!Files.exists(skinFile)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(skinFile);

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
            logger.warning("Failed to remove URL skin index for " + url, e);
        }
    }

    @Override
    public void setURLSkinIndex(String url, URLIndexData skinData) {
        Path skinFile = resolveURLSkinIndexFile(url);

        try {
            URLIndexFile file = URLIndexFile.fromURLIndexData(skinData);

            SRHelpers.writeIfNeeded(skinFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save URL skin index for " + url, e);
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
            String json = Files.readString(skinFile);

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
            logger.warning("Failed to remove custom skin data for " + skinName, e);
        }
    }

    @Override
    public void setCustomSkinData(String skinName, CustomSkinData skinData) {
        skinName = CustomSkinData.sanitizeCustomSkinName(skinName);
        Path skinFile = resolveCustomSkinFile(skinName);

        try {
            CustomSkinFile file = CustomSkinFile.fromCustomSkinData(skinData);

            SRHelpers.writeIfNeeded(skinFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save custom skin data for " + skinName, e);
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
            String json = Files.readString(skinFile);

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
            logger.warning("Failed to remove legacy skin data for " + skinName, e);
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
            String json = Files.readString(legacyFile);

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
            logger.warning("Failed to remove legacy player data for " + playerName, e);
        }
    }

    @Override
    public int getTotalCustomSkins() {
        return getCustomGUISkinFiles(0, Integer.MAX_VALUE).size();
    }

    @Override
    public List<GUIUtils.GUIRawSkinEntry> getCustomGUISkins(int offset, int limit) {
        List<GUIUtils.GUIRawSkinEntry> list = new ArrayList<>();
        List<GUIFileData> files = getCustomGUISkinFiles(offset, limit);

        for (GUIFileData data : files) {
            String fileName = data.fileName();
            try {
                CustomSkinData customSkinData = getCustomSkinData(fileName).orElseThrow();
                list.add(new GUIUtils.GUIRawSkinEntry(
                        SkinIdentifier.ofCustom(fileName),
                        customSkinData.getDisplayName() == null ? ComponentHelper.convertPlainToJson(fileName) : customSkinData.getDisplayName(),
                        PropertyUtils.getSkinTextureHash(customSkinData.getProperty()),
                        List.of()
                ));
            } catch (StorageException e) {
                logger.warning("Failed to load skin data for " + fileName, e);
            }
        }

        return list;
    }

    private List<GUIFileData> getCustomGUISkinFiles(int offset, int limit) {
        boolean onlyList = settings.getProperty(GUIConfig.CUSTOM_GUI_ONLY_LIST);
        Set<String> onlyListSkins = settings.getProperty(GUIConfig.CUSTOM_GUI_LIST)
                .stream()
                .map(CustomSkinData::sanitizeCustomSkinName)
                .collect(Collectors.toSet());

        List<GUIFileData> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(skinsFolder, 1)) {
            int skinIndex = 0;
            for (Path path : (Iterable<Path>) stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    ::iterator) {
                String fileName = path.getFileName().toString();
                int lastDotIndex = fileName.lastIndexOf(".");
                if (lastDotIndex == -1) {
                    continue;
                }

                String extension = fileName.substring(lastDotIndex + 1);
                String name = fileName.substring(0, lastDotIndex);

                if (name.startsWith(SkinStorageImpl.RECOMMENDATION_PREFIX) || !extension.equals("customskin")) {
                    continue;
                }

                // Only allow specific skins if enabled
                if (onlyList && !onlyListSkins.contains(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                // We offset only valid skin files
                if (skinIndex < offset) {
                    skinIndex++;
                    continue;
                }

                files.add(new GUIFileData(name, path));

                // We got max skins now, stop
                if (skinIndex++ >= offset + limit) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load GUI files", e);
        }

        return files;
    }

    @Override
    public int getTotalPlayerSkins() {
        return getPlayerGUISkinFiles(0, Integer.MAX_VALUE).size();
    }

    @Override
    public List<GUIUtils.GUIRawSkinEntry> getPlayerGUISkins(int offset, int limit) {
        List<GUIUtils.GUIRawSkinEntry> list = new ArrayList<>();
        List<GUIFileData> files = getPlayerGUISkinFiles(offset, limit);

        for (GUIFileData data : files) {
            String fileName = data.fileName();
            try {
                PlayerSkinData playerSkinData = getPlayerSkinData(UUID.fromString(fileName)).orElseThrow();
                list.add(new GUIUtils.GUIRawSkinEntry(
                        SkinIdentifier.ofPlayer(UUID.fromString(fileName)),
                        ComponentHelper.convertPlainToJson(playerSkinData.getLastKnownName()),
                        PropertyUtils.getSkinTextureHash(playerSkinData.getProperty()),
                        List.of()
                ));
            } catch (StorageException e) {
                logger.warning("Failed to load skin data for " + fileName, e);
            }
        }

        return list;
    }

    private List<GUIFileData> getPlayerGUISkinFiles(int offset, int limit) {
        boolean onlyList = settings.getProperty(GUIConfig.PLAYERS_GUI_ONLY_LIST);
        Set<String> onlyListSkins = settings.getProperty(GUIConfig.PLAYERS_GUI_LIST)
                .stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<GUIFileData> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(skinsFolder, 1)) {
            int skinIndex = 0;
            for (Path path : (Iterable<Path>) stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    ::iterator) {
                String fileName = path.getFileName().toString();
                int lastDotIndex = fileName.lastIndexOf(".");
                if (lastDotIndex == -1) {
                    continue;
                }

                String extension = fileName.substring(lastDotIndex + 1);
                String name = fileName.substring(0, lastDotIndex);

                if (!extension.equals("playerskin")) {
                    continue;
                }

                // Only allow specific skins if enabled
                if (onlyList && !onlyListSkins.contains(name.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                // We offset only valid skin files
                if (skinIndex < offset) {
                    skinIndex++;
                    continue;
                }

                files.add(new GUIFileData(name, path));

                // We got max skins now, stop
                if (skinIndex++ >= offset + limit) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load GUI files", e);
        }

        return files;
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
                String json = Files.readString(file);

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
            String json = Files.readString(cacheFile);

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

            SRHelpers.writeIfNeeded(cacheFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save cached UUID for " + playerName, e);
        }
    }

    @Override
    public List<UUID> getAllCooldownProfiles() throws StorageException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cooldownsFolder)) {
            List<UUID> list = new ArrayList<>();
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String[] parts = fileName.split("_");

                if (parts.length != 2) {
                    continue;
                }

                UUID uuid = UUID.fromString(parts[0]);

                list.add(uuid);
            }

            return list;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public List<StorageCooldown> getCooldowns(UUID owner) throws StorageException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cooldownsFolder)) {
            List<StorageCooldown> list = new ArrayList<>();
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String[] parts = fileName.split("_");

                if (parts.length != 2) {
                    continue;
                }

                UUID uuid = UUIDUtils.parseUniqueIdNullable(parts[0]);
                if (uuid == null || !uuid.equals(owner)) {
                    continue;
                }

                try {
                    String json = Files.readString(path);

                    CooldownFile file = gson.fromJson(json, CooldownFile.class);

                    list.add(file.toCooldownData());
                } catch (Exception e) {
                    logger.debug("Failed to load cooldown data for " + owner, e);
                }
            }

            return list;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void setCooldown(UUID owner, String groupName, Instant creationTime, Duration duration) {
        Path cooldownFile = resolveCooldownFile(owner, groupName);

        try {
            CooldownFile file = CooldownFile.fromCooldownData(new StorageCooldown(owner, groupName, creationTime, duration));

            SRHelpers.writeIfNeeded(cooldownFile, gson.toJson(file));
        } catch (IOException e) {
            logger.warning("Failed to save cooldown data for " + owner, e);
        }
    }

    @Override
    public void removeCooldown(UUID owner, String groupName) {
        Path cooldownFile = resolveCooldownFile(owner, groupName);

        try {
            Files.deleteIfExists(cooldownFile);
        } catch (IOException e) {
            logger.warning("Failed to remove cooldown data for " + owner, e);
        }
    }

    private Path resolveCustomSkinFile(String skinName) {
        return skinsFolder.resolve(skinName + ".customskin");
    }

    private Path resolveLegacySkinFile(String skinName) {
        return legacyFolder.resolve("skins").resolve(skinName + ".legacyskin");
    }

    private Path resolveURLSkinFile(String url, SkinVariant skinVariant) {
        return skinsFolder.resolve(SRHelpers.hashSha256ToHex(url) + "_" + skinVariant.name() + ".urlskin");
    }

    private Path resolveURLSkinIndexFile(String url) {
        return skinsFolder.resolve(SRHelpers.hashSha256ToHex(url) + ".urlindex");
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

    private Path resolveCooldownFile(UUID uuid, String groupName) {
        return cooldownsFolder.resolve(uuid + "_" + groupName + ".cooldown");
    }

    private Path resolveCacheFile(String name) {
        return cacheFolder.resolve(name + ".mojangcache");
    }

    private String sanitizeLegacyPlayerName(String playerName) {
        // The use of #toLowerCase() instead of #toLowerCase(Locale.ROOT) is intentional
        // This is because the legacy player names used this incorrect way of lowercasing
        return playerName.toLowerCase();
    }

    private String sanitizeLegacySkinName(String skinName) {
        // The use of #toLowerCase() instead of #toLowerCase(Locale.ROOT) is intentional
        // This is because the legacy skin names used this incorrect way of lowercasing
        return skinName.toLowerCase();
    }

    private record GUIFileData(String fileName, Path path) {
    }

    private record ComponentStringSerializer() implements JsonSerializer<ComponentString>, JsonDeserializer<ComponentString> {
        @Override
        public ComponentString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new ComponentString(json.getAsString());
        }

        @Override
        public JsonElement serialize(ComponentString src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.jsonString());
        }
    }
}
