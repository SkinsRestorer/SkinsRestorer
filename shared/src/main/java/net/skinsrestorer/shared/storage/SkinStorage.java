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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISkinStorage;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.exception.NotPremiumException;
import net.skinsrestorer.shared.utils.C;
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
import java.util.*;
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
                    getSkinForPlayer(skin);
                }
            } catch (SkinRequestException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.warning("[WARNING] DefaultSkin '" + skin + "'(.skin) could not be found or requested! Removing from list..");

                logger.debug("[DEBUG] DefaultSkin '" + skin + "' error: ");
                if (Config.DEBUG)
                    e.printStackTrace();
            }
        });
        Config.DEFAULT_SKINS.removeAll(toRemove);

        if (Config.DEFAULT_SKINS.isEmpty()) {
            logger.warning("[WARNING] No more working DefaultSkin left... disabling feature");
            Config.DEFAULT_SKINS_ENABLED = false;
        }
    }

    public IProperty getDefaultSkinForPlayer(final String playerName) throws SkinRequestException {
        final String skin = getDefaultSkinName(playerName);

        if (C.validUrl(skin)) {
            return mineSkinAPI.genSkin(skin, null);
        } else {
            return getSkinForPlayer(skin);
        }
    }

    public IProperty getSkinForPlayer(final String playerName) throws SkinRequestException {
        Optional<String> skin = getSkinOfPlayer(playerName);

        if (!skin.isPresent()) {
            skin = Optional.of(playerName.toLowerCase());
        }

        Optional<IProperty> textures = getSkinData(skin.get());
        if (!textures.isPresent()) {
            // No cached skin found, get from MojangAPI, save and return
            try {
                if (!C.validMojangUsername(skin.get()))
                    throw new SkinRequestException(Locale.INVALID_PLAYER.replace("%player", skin.get()));

                textures = mojangAPI.getSkin(skin.get());

                if (!textures.isPresent())
                    throw new SkinRequestException(Locale.ERROR_NO_SKIN);

                setSkinData(skin.get(), textures.get());

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
    public Optional<String> getSkinOfPlayer(String playerName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYER_TABLE + " WHERE Nick=?", playerName);

            if (crs != null)
                try {
                    final String skin = crs.getString("Skin");

                    //maybe useless
                    if (skin.isEmpty()) {
                        removeSkinOfPlayer(playerName);
                        return Optional.empty();
                    }

                    return Optional.of(skin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {
            playerName = removeForbiddenChars(playerName);
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
    private IProperty createProperty(String playerName, boolean updateOutdated, String value, String signature, String timestamp) throws SkinRequestException {
        if (updateOutdated && C.validMojangUsername(playerName) && isExpired(Long.parseLong(timestamp))) {
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
            playerName = removeForbiddenChars(playerName);
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
            playerName = removeForbiddenChars(playerName);
            Path playerFile = playersFolder.resolve(playerName + ".player");

            try {
                try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(playerFile), StandardCharsets.UTF_8)) {
                    skinName = removeWhitespaces(skinName);
                    skinName = removeForbiddenChars(skinName);

                    writer.write(skinName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Optional<IProperty> getSkinData(String skinName) {
        return getSkinData(skinName, true);
    }

    // #getSkinData() also create while we have #getSkinForPlayer()
    @Override
    public Optional<IProperty> getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKIN_TABLE + " WHERE Nick=?", skinName);
            if (crs != null)
                try {
                    final String value = crs.getString("Value");
                    final String signature = crs.getString("Signature");
                    final String timestamp = crs.getString("timestamp");

                    return Optional.of(createProperty(skinName, updateOutdated, value, signature, timestamp));
                } catch (Exception e) {
                    removeSkinData(skinName);
                    logger.info("Unsupported skin format.. removing (" + skinName + ").");
                }
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = removeForbiddenChars(skinName);
            Path skinFile = skinsFolder.resolve(skinName + ".skin");

            try {
                if (!Files.exists(skinFile))
                    return Optional.empty();

                List<String> lines = Files.readAllLines(skinFile);

                String value = lines.get(0);
                String signature = lines.get(1);
                String timestamp = lines.get(2);

                return Optional.of(createProperty(skinName, updateOutdated, value, signature, timestamp));
            } catch (Exception e) {
                removeSkinData(skinName);
                logger.info("Unsupported skin format.. removing (" + skinName + ").");
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
            skinName = removeForbiddenChars(skinName);
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
            skinName = removeForbiddenChars(skinName);
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
    public Map<String, IProperty> getSkins(int number) {
        //Using mysql
        Map<String, IProperty> list = new TreeMap<>();

        if (Config.MYSQL_ENABLED) {
            String filterBy = "";
            String orderBy = "Nick";

            // custom gui
            if (Config.CUSTOM_GUI_ENABLED) {
                StringBuilder sb = new StringBuilder();
                if (Config.CUSTOM_GUI_ONLY) {
                    Config.CUSTOM_GUI_SKINS.forEach(sb.append("|")::append);

                    filterBy = "WHERE Nick RLIKE '" + sb.substring(1) + "'";
                } else {
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> sb.append(", '").append(skin).append("'"));

                    orderBy = "FIELD(Nick" + sb + ") DESC, Nick";
                }
            }

            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKIN_TABLE + " " + filterBy + " ORDER BY " + orderBy);
            int i = 0;
            try {
                do {
                    if (i >= number)
                        list.put(crs.getString("Nick").toLowerCase(), SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME, crs.getString("Value"), crs.getString("Signature")));
                    i++;
                } while (crs.next());
            } catch (SQLException ignored) {
            }
        } else { // When not using mysql
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
                if (i >= number) {
                    if (Config.CUSTOM_GUI_ONLY) { //Show only Config.CUSTOM_GUI_SKINS in the gui
                        for (String GuiSkins : Config.CUSTOM_GUI_SKINS) {
                            if (skinName.toLowerCase().contains(GuiSkins.toLowerCase()))
                                getSkinData(skinName, false).ifPresent(property -> list.put(skinName.toLowerCase(), property));
                        }
                    } else {
                        getSkinData(skinName, false).ifPresent(property -> list.put(skinName.toLowerCase(), property));
                    }
                }
                i++;
            }
        }

        return list;
    }

    // TODO: remove duplicated code and use existing methods....
    // TODO: needs a lot refactoring!
    // TODO: We should _always_ return our own Property object and cast to the platform specific one just before actually setting the skin.
    // TODO: That should save lots of duplicated code
    public Map<String, GenericProperty> getSkinsRaw(int number) {
        Map<String, GenericProperty> list = new TreeMap<>();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKIN_TABLE + " ORDER BY `Nick`");
            int i = 0;
            int foundSkins = 0;
            try {
                do {
                    if (i >= number && foundSkins <= 25) {
                        GenericProperty prop = new GenericProperty();
                        prop.setName(IProperty.TEXTURES_NAME);
                        prop.setValue(crs.getString("Value"));
                        prop.setSignature(crs.getString("Signature"));
                        list.put(crs.getString("Nick"), prop);
                        foundSkins++;
                    }
                    i++;
                } while (crs.next());
            } catch (SQLException ignored) {
            }
        } else {
            List<Path> files = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
                stream.forEach(files::add);
            } catch (IOException e) {
                e.printStackTrace();
            }

            files.sort(Comparator.comparing(o -> o.getFileName().toString()));

            int i = 0;
            int foundSkins = 0;
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String skinName = fileName.substring(0, fileName.length() - 5);
                if (i >= number && foundSkins <= 25) {
                    try {
                        if (!Files.exists(file))
                            continue;

                        List<String> lines = Files.readAllLines(file);

                        GenericProperty prop = new GenericProperty();
                        prop.setName(IProperty.TEXTURES_NAME);
                        prop.setValue(lines.get(0));
                        prop.setSignature(lines.get(1));
                        list.put(skinName, prop);

                        foundSkins++;
                    } catch (Exception ignored) {
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
            skinName = removeForbiddenChars(skinName);

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
     * @see SkinStorage#getDefaultSkinName(String, boolean)
     */
    public String getDefaultSkinName(String playerName) {
        return getDefaultSkinName(playerName, false);
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
     * @param clear      return player instead of his set skin
     * @return setSkin or DefaultSkin, if player has no setSkin or default skin, we return his name
     */
    public String getDefaultSkinName(String playerName, boolean clear) {
        // Trim player name
        playerName = TRIM_PATTERN.matcher(playerName).replaceAll("");

        if (Config.DEFAULT_SKINS_ENABLED) {
            // don't return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (C.validMojangUsername(playerName) && mojangAPI.getUUID(playerName) != null) {
                        // player is premium, return his skin name instead of default skin
                        return playerName;
                    }
                } catch (SkinRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set, or we want to clear to default
            if (!getSkinOfPlayer(playerName).isPresent() || clear) {
                final List<String> skins = Config.DEFAULT_SKINS;

                String randomSkin = skins.size() > 1 ? skins.get(new Random().nextInt(skins.size())) : skins.get(0);

                // return player name if there are no default skins set
                return randomSkin != null ? randomSkin : playerName;
            }
        }

        // return the player name if we want to clear the skin
        if (clear)
            return playerName;

        // empty if player has no custom skin, we'll return his name then
        return getSkinOfPlayer(playerName).orElse(playerName);
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

    private String removeForbiddenChars(String str) {
        // Escape all Windows / Linux forbidden printable ASCII characters
        return FORBIDDEN_CHARS_PATTERN.matcher(str).replaceAll("·");
    }

    //todo remove all whitespace after last starting space.
    private String removeWhitespaces(String str) {
        // Remove all whitespace expect when startsWith " ".
        if (str.startsWith(" ")) {
            return str;
        }
        return WHITESPACE_PATTERN.matcher(str).replaceAll("");
    }

    public boolean purgeOldSkins(int Days) {
        int removedSkins = 0;
        long targetPurgeTimestamp = ((long) Days * 86400 * 1000) - System.currentTimeMillis();
        if (Config.MYSQL_ENABLED) {
            // delete if name not start with " " and timestamp below targetPurgeTimestamp
            mysql.execute("DELETE FROM " + Config.MYSQL_PLAYER_TABLE + " WHERE Nick NOT LIKE ' %' AND timestamp <>0 AND timestamp=<?", targetPurgeTimestamp);
            return true;
        } else {
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(skinsFolder, "*.skin")) {
                stream.forEach(files::add);

                for (Path file : files) {
                    try {
                        if (!Files.exists(file))
                            continue;

                        List<String> lines = Files.readAllLines(file);
                        Long timestamp = Long.valueOf(lines.get(2));

                        if (!(timestamp.equals(0L)) && timestamp < targetPurgeTimestamp) {
                            Files.deleteIfExists(file);
                            removedSkins++;
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
