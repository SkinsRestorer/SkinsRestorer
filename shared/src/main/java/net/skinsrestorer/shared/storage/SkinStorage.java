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
package net.skinsrestorer.shared.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISkinStorage;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.exception.NotPremiumException;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.api.util.Pair;
import net.skinsrestorer.shared.utils.connections.MineSkinAPI;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.sql.RowSet;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SkinStorage implements ISkinStorage {
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[\\\\/:*?\"<>|\\.]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private static final String LTRIM = "^\\\\s+";
    private static final String RTRIM = "\\\\s+$";
    private static final Pattern TRIM_PATTERN = Pattern.compile("(" + LTRIM + "|" + RTRIM + ")");
    private final SRLogger logger;
    private final MojangAPI mojangAPI;
    private final MineSkinAPI mineSkinAPI;
    @Setter
    private MySQL mysql;
    private Path skinsFolder;
    private Path playersFolder;
    @Setter
    @Getter
    private boolean initialized = false;

    public void loadFolders(Path dataFolder) {
        skinsFolder = dataFolder.resolve("Skins");
        try {
            Files.createDirectories(skinsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        playersFolder = dataFolder.resolve("Players");
        try {
            Files.createDirectories(playersFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void preloadDefaultSkins() {
        if (!Config.DEFAULT_SKINS_ENABLED)
            return;

        List<String> toRemove = new ArrayList<>();
        Config.DEFAULT_SKINS.forEach(skin -> {
            // TODO: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    fetchSkinData(skin);
                }
            } catch (SkinRequestException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.warning("[WARNING] DefaultSkin '" + skin + "'(.skin) could not be found or requested! Removing from list..");

                logger.debug("[DEBUG] DefaultSkin '" + skin + "' error: ", e);
            }
        });
        Config.DEFAULT_SKINS.removeAll(toRemove);

        if (Config.DEFAULT_SKINS.isEmpty()) {
            logger.warning("[WARNING] No more working DefaultSkin left... disabling feature");
            Config.DEFAULT_SKINS_ENABLED = false;
        }
    }

    @Override
    public Pair<IProperty, Boolean> getDefaultSkinForPlayer(String playerName) throws SkinRequestException {
        final Pair<String, Boolean> result = getDefaultSkinName(playerName, false);
        String skin = result.getLeft();

        if (C.validUrl(skin)) {
            return Pair.of(mineSkinAPI.genSkin(skin, null), result.getRight());
        } else {
            return Pair.of(fetchSkinData(skin), result.getRight());
        }
    }

    @Override
    public IProperty fetchSkinData(String skinName) throws SkinRequestException {
        Optional<IProperty> textures = getSkinData(skinName, true);
        if (!textures.isPresent()) {
            // No cached skin found, get from MojangAPI, save and return
            try {
                if (!C.validMojangUsername(skinName))
                    throw new SkinRequestException(Locale.INVALID_PLAYER.replace("%player", skinName));

                textures = mojangAPI.getSkin(skinName);

                if (!textures.isPresent())
                    throw new SkinRequestException(Locale.ERROR_NO_SKIN);

                setSkinData(skinName, textures.get());

                return textures.get();
            } catch (SkinRequestException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();

                throw new SkinRequestException(Locale.WAIT_A_MINUTE);
            }
        } else {
            return textures.get();
        }
    }

    @Override
    public Optional<String> getSkinNameOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYER_TABLE + " WHERE Nick=?", playerName);

            if (crs == null)
                return Optional.empty();

            try {
                final String skin = crs.getString("Skin");

                // maybe useless
                if (skin.isEmpty()) {
                    removeSkinOfPlayer(playerName);
                    return Optional.empty();
                }

                return Optional.of(skin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            playerName = replaceForbiddenChars(playerName);
            Path playerFile = playersFolder.resolve(playerName + ".player");

            try {
                if (!Files.exists(playerFile))
                    return Optional.empty();

                List<String> lines = Files.readAllLines(playerFile);

                // Maybe useless
                if (lines.isEmpty()) {
                    removeSkinOfPlayer(playerName);
                    return Optional.empty();
                }

                return Optional.of(lines.get(0));
            } catch (MalformedInputException e) {
                removeSkinOfPlayer(playerName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }

    /**
     * Create a platform specific property and also optionally update cached skin if outdated.
     *
     * @param playerName     the players name
     * @param updateOutdated whether the skin data shall be looked up again if the timestamp is too far away
     * @param value          skin data value
     * @param signature      signature to verify skin data
     * @param timestamp      time cached property data was created
     * @return Platform specific property
     * @throws SkinRequestException throws when no API calls were successful
     */
    private IProperty createProperty(String playerName, boolean updateOutdated, String value, String signature, long timestamp) throws SkinRequestException {
        if (updateOutdated && C.validMojangUsername(playerName) && isExpired(timestamp)) {
            Optional<IProperty> skin = mojangAPI.getSkin(playerName);

            if (skin.isPresent()) {
                setSkinData(playerName, skin.get());
                return skin.get();
            }
        }

        return SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME, value, signature);
    }

    @Override
    public void removeSkinOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_PLAYER_TABLE + " WHERE Nick=?", playerName);
        } else {
            playerName = replaceForbiddenChars(playerName);
            Path playerFile = playersFolder.resolve(playerName + ".player");

            try {
                Files.deleteIfExists(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setSkinOfPlayer(String playerName, String skinName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_PLAYER_TABLE + " (Nick, Skin) VALUES (?,?) ON DUPLICATE KEY UPDATE Skin=?",
                    playerName, skinName, skinName);
        } else {
            playerName = replaceForbiddenChars(playerName);
            Path playerFile = playersFolder.resolve(playerName + ".player");

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
    }

    // #getSkinData() also create while we have #getSkinForPlayer()
    @Override
    public Optional<IProperty> getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKIN_TABLE + " WHERE Nick=?", skinName);

            if (crs == null)
                return Optional.empty();

            try {
                final String value = crs.getString("Value");
                final String signature = crs.getString("Signature");
                final String timestamp = crs.getString("timestamp");

                return Optional.of(createProperty(skinName, updateOutdated, value, signature, Long.parseLong(timestamp)));
            } catch (Exception e) {
                logger.info(String.format("Unsupported skin format.. removing (%s).", skinName));
                removeSkinData(skinName);
            }
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = replaceForbiddenChars(skinName);
            Path skinFile = skinsFolder.resolve(skinName + ".skin");

            try {
                if (!Files.exists(skinFile))
                    return Optional.empty();

                List<String> lines = Files.readAllLines(skinFile);

                String value = lines.get(0);
                String signature = lines.get(1);
                String timestamp = lines.get(2);

                return Optional.of(createProperty(skinName, updateOutdated, value, signature, Long.parseLong(timestamp)));
            } catch (Exception e) {
                logger.info(String.format("Unsupported skin format.. removing (%s).", skinName));
                removeSkinData(skinName);
            }
        }

        return Optional.empty();
    }

    /**
     * Removes skin data from database
     *
     * @param skinName Skin name
     */
    public void removeSkinData(String skinName) {
        skinName = skinName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_SKIN_TABLE + " WHERE Nick=?", skinName);
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = replaceForbiddenChars(skinName);
            Path skinFile = skinsFolder.resolve(skinName + ".skin");

            try {
                Files.deleteIfExists(skinFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setSkinData(String skinName, IProperty textures) {
        setSkinData(skinName, textures, System.currentTimeMillis());
    }

    @Override
    public void setSkinData(String skinName, IProperty textures, long timestamp) {
        skinName = skinName.toLowerCase();
        String value = textures.getValue();
        String signature = textures.getSignature();

        String timestampString = Long.toString(timestamp);

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_SKIN_TABLE + " (Nick, Value, Signature, timestamp) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE Value=?, Signature=?, timestamp=?",
                    skinName, value, signature, timestampString, value, signature, timestampString);
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = replaceForbiddenChars(skinName);
            Path skinFile = skinsFolder.resolve(skinName + ".skin");

            try {
                if (value.isEmpty() || signature.isEmpty() || timestampString.isEmpty())
                    return;

                try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(skinFile), StandardCharsets.UTF_8)) {
                    writer.write(value + "\n" + signature + "\n" + timestamp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, String> getSkins(int offset) {
        Map<String, String> list = new TreeMap<>();

        if (Config.MYSQL_ENABLED) {
            String filterBy = "";
            String orderBy = "Nick";

            // Custom GUI
            if (Config.CUSTOM_GUI_ENABLED) {
                if (Config.CUSTOM_GUI_ONLY) {
                    filterBy = "WHERE Nick RLIKE '" + String.join("|", Config.CUSTOM_GUI_SKINS) + "'";
                } else {
                    orderBy = "FIELD(Nick, " + Config.CUSTOM_GUI_SKINS.stream().map(skin -> "'" + skin + "'").collect(Collectors.joining(", ")) + ") DESC, Nick";
                }
            }

            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKIN_TABLE + " " + filterBy + " ORDER BY " + orderBy + " LIMIT " + offset + ", 36");
            try {
                do {
                    list.put(crs.getString("Nick").toLowerCase(), crs.getString("Value"));
                } while (crs.next());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
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
                                getSkinData(skinName, false).ifPresent(property -> list.put(skinName.toLowerCase(), property.getValue()));
                        }
                    } else {
                        getSkinData(skinName, false).ifPresent(property -> list.put(skinName.toLowerCase(), property.getValue()));
                    }
                }
                i++;
            }
        }

        return list;
    }

    /**
     * @param skinName Skin name
     * @return true on updated
     * @throws SkinRequestException On updating disabled OR invalid username + api error
     */
    // skin update [include custom skin flag]
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean updateSkinData(String skinName) throws SkinRequestException {
        if (!C.validMojangUsername(skinName))
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);

        // Check if updating is disabled for skin (by timestamp = 0)
        boolean updateDisabled = false;
        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT timestamp FROM " + Config.MYSQL_SKIN_TABLE + " WHERE Nick=?", skinName);
            if (crs != null)
                try {
                    updateDisabled = crs.getString("timestamp").equals("0");
                } catch (Exception ignored) {
                }
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = replaceForbiddenChars(skinName);

            Path skinFile = skinsFolder.resolve(skinName + ".skin");

            try {
                if (Files.exists(skinFile)) {
                    updateDisabled = Files.readAllLines(skinFile).get(2).equals("0");
                }
            } catch (Exception ignored) {
            }
        }

        if (updateDisabled)
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);

        // Update Skin
        try {
            Optional<String> mojangUUID = mojangAPI.getUUIDMojang(skinName);

            if (mojangUUID.isPresent()) {
                Optional<IProperty> textures = mojangAPI.getProfileMojang(mojangUUID.get());

                if (textures.isPresent()) {
                    setSkinData(skinName, textures.get());
                    return true;
                }
            }
        } catch (NotPremiumException e) {
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);
        }

        return false;
    }

    /**
     * Filters player name to exclude non [a-z_]
     * Checks and process default skin.
     * IF no default skin:
     * 1: Return player if clear
     * 2: Return skin if found
     * Else: return player
     *
     * @param playerName Player name
     * @param clear      ignore custom set skin of player
     * @return Custom skin or default skin or player name, right side indicates if it is a custom skin
     */
    public Pair<String, Boolean> getDefaultSkinName(String playerName, boolean clear) {
        // Trim player name
        playerName = TRIM_PATTERN.matcher(playerName).replaceAll("");

        if (!clear) {
            Optional<String> playerSkinName = getSkinNameOfPlayer(playerName);

            if (playerSkinName.isPresent()) {
                return Pair.of(playerSkinName.get(), true);
            }
        }

        if (Config.DEFAULT_SKINS_ENABLED) {
            // don't return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (C.validMojangUsername(playerName) && mojangAPI.getUUID(playerName) != null) {
                        // player is premium, return his skin name instead of default skin
                        return Pair.of(playerName, false);
                    }
                } catch (SkinRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set, or we want to clear to default
            List<String> skins = Config.DEFAULT_SKINS;

            // return player name if there are no default skins set
            if (skins.isEmpty())
                return Pair.of(playerName, false);

            return Pair.of(skins.size() > 1 ? skins.get(ThreadLocalRandom.current().nextInt(skins.size())) : skins.get(0), false);
        }

        // empty if player has no custom skin, we'll return his name then
        return Pair.of(playerName, false);
    }

    /**
     * Checks if updating skins is disabled and if skin expired
     *
     * @param timestamp in milliseconds
     * @return true if skin is outdated
     */
    private boolean isExpired(long timestamp) {
        // Don't update if timestamp is not 0 or update is disabled.
        if (timestamp == 0 || Config.DISALLOW_AUTO_UPDATE_SKIN)
            return false;

        return timestamp + TimeUnit.MINUTES.toMillis(Config.SKIN_EXPIRES_AFTER) <= System.currentTimeMillis();
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

    public boolean purgeOldSkins(int days) {
        long targetPurgeTimestamp = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli();

        if (Config.MYSQL_ENABLED) {
            // delete if name not start with " " and timestamp below targetPurgeTimestamp
            mysql.execute("DELETE FROM " + Config.MYSQL_SKIN_TABLE + " WHERE Nick NOT LIKE ' %' AND " + Config.MYSQL_SKIN_TABLE + ".timestamp NOT LIKE 0 AND " + Config.MYSQL_SKIN_TABLE + ".timestamp<=?", targetPurgeTimestamp);
            return true;
        } else {
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
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
