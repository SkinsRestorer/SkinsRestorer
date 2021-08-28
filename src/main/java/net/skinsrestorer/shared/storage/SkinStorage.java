/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.sql.RowSet;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SkinStorage {
    private final SRLogger logger;
    private final MojangAPI mojangAPI;
    @Setter
    private MySQL mysql;
    private File skinsFolder;
    private File playersFolder;

    public void loadFolders(File pluginFolder) {
        skinsFolder = new File(pluginFolder, "Skins");
        //noinspection ResultOfMethodCallIgnored
        skinsFolder.mkdirs();

        playersFolder = new File(pluginFolder, "Players");
        //noinspection ResultOfMethodCallIgnored
        playersFolder.mkdirs();
    }

    public void preloadDefaultSkins() {
        if (!Config.DEFAULT_SKINS_ENABLED)
            return;

        List<String> toRemove = new ArrayList<>();
        Config.DEFAULT_SKINS.forEach(skin -> {
            // TODO: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    getSkinForPlayer(skin, false);
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

    /**
     * This method seeks out a players actual skin (chosen or not) and returns
     * either null (if no skin data found) or the property containing all
     * the skin data.
     * It also schedules a skin update to stay up to date with skin changes.
     *
     * @param name   Player name to search skin for
     * @param silent Whether to throw errors or not
     * @throws SkinRequestException If MojangAPI lookup errors
     **/
    public IProperty getSkinForPlayer(final String name, boolean silent) throws SkinRequestException {
        String skin = getSkinName(name);

        if (skin == null) {
            skin = name.toLowerCase();
        }

        IProperty textures = getSkinData(skin);

        if (textures == null) {
            // No cached skin found, get from MojangAPI, save and return
            try {
                if (!C.validMojangUsername(skin))
                    throw new SkinRequestException(Locale.INVALID_PLAYER.replace("%player", skin));

                textures = mojangAPI.getProfile(mojangAPI.getUUID(skin));

                if (textures == null)
                    throw new SkinRequestException(Locale.ERROR_NO_SKIN);

                setSkinData(skin, textures);
            } catch (SkinRequestException e) {
                if (!silent)
                    throw e;
            } catch (Exception e) {
                e.printStackTrace();

                if (!silent)
                    throw new SkinRequestException(Locale.WAIT_A_MINUTE);
            }
        }

        return textures;
    }

    /**
     * Returns the custom skin name that player has set.
     * Returns null if player has no custom skin set.
     **/
    public String getSkinName(String playerName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", playerName);

            if (crs != null)
                try {
                    final String skin = crs.getString("Skin");

                    //maybe useless
                    if (skin.isEmpty()) {
                        removeSkin(playerName);
                        return null;
                    }

                    return skin;
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {
            playerName = removeForbiddenChars(playerName);
            File playerFile = new File(playersFolder, playerName + ".player");

            try {
                if (!playerFile.exists())
                    return null;

                String skin = null;

                try (BufferedReader buf = new BufferedReader(new FileReader(playerFile))) {
                    final String line = buf.readLine();
                    if (line != null)
                        skin = line;
                }

                // Maybe useless
                if (skin == null) {
                    removeSkin(playerName);
                    return null;
                }

                return skin;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Returns property object containing skin data of the wanted skin
     *
     * @param skinName       Skin name
     * @param updateOutdated On true, we update the skin if expired
     **/
    // #getSkinData() also create while we have #getSkinForPlayer()
    public IProperty getSkinData(String skinName, boolean updateOutdated) {
        skinName = skinName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", skinName);
            if (crs != null)
                try {
                    final String value = crs.getString("Value");
                    final String signature = crs.getString("Signature");
                    final String timestamp = crs.getString("timestamp");

                    return updateOutdated(skinName, updateOutdated, value, signature, timestamp);
                } catch (Exception e) {
                    removeSkinData(skinName);
                    logger.info("Unsupported player format.. removing (" + skinName + ").");
                }
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = removeForbiddenChars(skinName);
            File skinFile = new File(skinsFolder, skinName + ".skin");

            try {
                if (!skinFile.exists())
                    return null;

                String value = null;
                String signature = null;
                String timestamp = null;

                try (BufferedReader buf = new BufferedReader(new FileReader(skinFile))) {
                    for (int i = 0; i < 3; i++) {
                        switch (i) {
                            case 0:
                                value = buf.readLine();
                                break;
                            case 1:
                                signature = buf.readLine();
                                break;
                            case 2:
                                timestamp = buf.readLine();
                                break;
                            default:
                                break;
                        }
                    }
                }

                return updateOutdated(skinName, updateOutdated, value, signature, timestamp);
            } catch (Exception e) {
                removeSkinData(skinName);
                logger.info("Unsupported player format.. removing (" + skinName + ").");
            }
        }

        return null;
    }

    private IProperty updateOutdated(String name, boolean updateOutdated, String value, String signature, String timestamp) throws SkinRequestException {
        if (updateOutdated && C.validMojangUsername(name) && isOld(Long.parseLong(timestamp))) {
            IProperty skin = mojangAPI.getProfile(mojangAPI.getUUID(name));

            if (skin != null) {
                setSkinData(name, skin);
                return skin;
            }
        }

        return mojangAPI.createProperty("textures", value, signature);
    }

    /**
     * @see SkinStorage#getSkinData(String, boolean)
     */
    public IProperty getSkinData(String skinName) {
        return getSkinData(skinName, true);
    }

    /**
     * Checks if updating skins is disabled and if skin expired
     *
     * @param timestamp in milliseconds
     * @return true if skin is outdated
     */
    private boolean isOld(long timestamp) {
        // Don't update if timestamp is not 0 or update is disabled.
        if (timestamp == 0 || Config.DISALLOW_AUTO_UPDATE_SKIN)
            return false;

        return timestamp + TimeUnit.MINUTES.toMillis(Config.SKIN_EXPIRES_AFTER) <= System.currentTimeMillis();
    }

    /**
     * Removes custom players skin name from database
     *
     * @param name - Players name
     **/
    public void removeSkin(String name) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);
        } else {
            name = removeForbiddenChars(name);
            File playerFile = new File(playersFolder, name + ".player");

            try {
                Files.deleteIfExists(playerFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes skin data from database
     *
     * @param skinName - Skin name
     **/
    public void removeSkinData(String skinName) {
        skinName = skinName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", skinName);
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = removeForbiddenChars(skinName);
            File skinFile = new File(skinsFolder, skinName + ".skin");

            try {
                Files.deleteIfExists(skinFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves custom player's skin name to database
     *
     * @param playerName Players name
     * @param skinName   Skin name
     **/
    public void setSkinName(String playerName, String skinName) {
        playerName = playerName.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_PLAYERTABLE + " (Nick, Skin) VALUES (?,?)"
                    + " ON DUPLICATE KEY UPDATE Skin=?", playerName, skinName, skinName);
        } else {
            playerName = removeForbiddenChars(playerName);
            File playerFile = new File(playersFolder, playerName + ".player");

            try {
                if (!playerFile.exists() && !playerFile.createNewFile())
                    throw new IOException("Could not create player file!");

                try (FileWriter writer = new FileWriter(playerFile)) {
                    skinName = removeWhitespaces(skinName);
                    skinName = removeForbiddenChars(skinName);

                    writer.write(skinName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves skin data to database
     *
     * @param skinName  - Skin name
     * @param textures  - Property object
     * @param timestamp - timestamp string in millis
     **/
    public void setSkinData(String skinName, IProperty textures, String timestamp) {
        skinName = skinName.toLowerCase();
        String value = textures.getValue();
        String signature = textures.getSignature();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_SKINTABLE + " (Nick, Value, Signature, timestamp) VALUES (?,?,?,?)"
                    + " ON DUPLICATE KEY UPDATE Value=?, Signature=?, timestamp=?", skinName, value, signature, timestamp, value, signature, timestamp);
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = removeForbiddenChars(skinName);
            File skinFile = new File(skinsFolder, skinName + ".skin");

            try {
                if (value.isEmpty() || signature.isEmpty() || timestamp.isEmpty())
                    return;

                if (!skinFile.exists() && !skinFile.createNewFile())
                    throw new IOException("Could not create skin file!");

                try (FileWriter writer = new FileWriter(skinFile)) {
                    writer.write(value + "\n" + signature + "\n" + timestamp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @see SkinStorage#setSkinData(String, IProperty, String)
     */
    public void setSkinData(String skinName, IProperty textures) {
        setSkinData(skinName, textures, Long.toString(System.currentTimeMillis()));
    }

    // TODO: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, Object> getSkins(int number) {
        //Using mysql
        Map<String, Object> list = new TreeMap<>();
        if (Config.MYSQL_ENABLED) {
            String filterBy = "";
            String orderBy = "Nick";

            // custom gui
            if (Config.CUSTOM_GUI_ENABLED) {
                StringBuilder sb = new StringBuilder();
                if (Config.CUSTOM_GUI_ONLY) {
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> sb.append("|").append(skin));

                    filterBy = " WHERE Nick RLIKE '" + sb.substring(1) + "'";
                } else {
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> sb.append(", '").append(skin).append("'"));

                    orderBy = "FIELD(Nick" + sb + ") DESC, Nick";
                }
            }

            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKINTABLE + filterBy + " ORDER BY " + orderBy);
            int i = 0;
            try {
                do {
                    if (i >= number)
                        list.put(crs.getString("Nick").toLowerCase(), mojangAPI.createProperty("textures", crs.getString("Value"), crs.getString("Signature")));
                    i++;
                } while (crs.next());
            } catch (SQLException ignored) {
            }

            // When not using mysql
        } else {

            //filter out non "*.skin" files.
            FilenameFilter skinFileFilter = (dir, name) -> name.endsWith(".skin");

            String[] fileNames = skinsFolder.list(skinFileFilter);

            if (fileNames == null)
                // TODO: should this not also be null if no skin is valid?
                return list;

            Arrays.sort(fileNames);
            int i = 0;
            for (String file : fileNames) {
                String skinName = file.replace(".skin", "");
                if (i >= number) {
                    if (Config.CUSTOM_GUI_ONLY) { //Show only Config.CUSTOM_GUI_SKINS in the gui
                        for (String GuiSkins : Config.CUSTOM_GUI_SKINS) {
                            if (skinName.toLowerCase().contains(GuiSkins.toLowerCase()))
                                list.put(skinName.toLowerCase(), getSkinData(skinName, false));
                        }
                    } else {
                        list.put(skinName.toLowerCase(), getSkinData(skinName, false));
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
            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKINTABLE + " ORDER BY `Nick`");
            int i = 0;
            int foundSkins = 0;
            try {
                do {
                    if (i >= number && foundSkins <= 26) {
                        GenericProperty prop = new GenericProperty();
                        prop.setName("textures");
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
            // filter out non "*.skin" files.
            FilenameFilter skinFileFilter = (dir, name) -> name.endsWith(".skin");

            String[] fileNames = skinsFolder.list(skinFileFilter);

            if (fileNames == null)
                return list;

            Arrays.sort(fileNames);
            int i = 0;
            int foundSkins = 0;
            for (String file : fileNames) {
                String skinName = file.replace(".skin", "");

                File skinFile = new File(skinsFolder, file);
                if (i >= number && foundSkins <= 26) {

                    try {
                        if (!skinFile.exists())
                            return null;

                        String value;
                        String signature;
                        try (BufferedReader buf = new BufferedReader(new FileReader(skinFile))) {
                            String line;
                            value = "";
                            signature = "";

                            for (int i2 = 0; i2 < 3; i2++)
                                if ((line = buf.readLine()) != null) {
                                    if (value.isEmpty()) {
                                        value = line;
                                    } else if (signature.isEmpty()) {
                                        signature = line;
                                    }
                                }
                        }

                        GenericProperty prop = new GenericProperty();
                        prop.setName("textures");
                        prop.setValue(value);
                        prop.setSignature(signature);
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
            RowSet crs = mysql.query("SELECT timestamp FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", skinName);
            if (crs != null)
                try {
                    updateDisabled = crs.getString("timestamp").equals("0");
                } catch (Exception ignored) {
                }
        } else {
            skinName = removeWhitespaces(skinName);
            skinName = removeForbiddenChars(skinName);

            File skinFile = new File(skinsFolder, skinName + ".skin");

            try {
                if (skinFile.exists()) {
                    try (BufferedReader buf = new BufferedReader(new FileReader(skinFile))) {
                        for (int i = 0; i < 3; i++) {
                            String line = buf.readLine();

                            if (i == 2)
                                updateDisabled = line.equals("0");
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (updateDisabled)
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);

        // Update Skin
        try {
            IProperty textures = mojangAPI.getProfileMojang(mojangAPI.getUUIDMojang(skinName, true), true);

            if (textures != null) {
                setSkinData(skinName, textures);
                return true;
            }
        } catch (SkinRequestException e) {
            if (e.getMessage().equals(Locale.NOT_PREMIUM))
                throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);
            else
                throw e;
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
        // LTrim and RTrim player name
        playerName = playerName.replaceAll("^\\\\s+", "");
        playerName = playerName.replaceAll("\\\\s+$", "");

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

            // return default skin name if user has no custom skin set or we want to clear to default
            if (getSkinName(playerName) == null || clear) {
                final List<String> skins = Config.DEFAULT_SKINS;
                int r = 0;
                if (skins.size() > 1)
                    r = new Random().nextInt(skins.size());
                String randomSkin = skins.get(r);
                // return player name if there are no default skins set
                return randomSkin != null ? randomSkin : playerName;
            }
        }

        // return the player name if we want to clear the skin
        if (clear)
            return playerName;

        // return the custom skin user has set
        String skin = getSkinName(playerName);

        // null if player has no custom skin, we'll return his name then
        return skin == null ? playerName : skin;
    }

    private String removeForbiddenChars(String str) {
        // Escape all Windows / Linux forbidden printable ASCII characters
        return str.replaceAll("[\\\\/:*?\"<>|]", "·");
    }

    //todo remove all whitespace after last starting space.
    private String removeWhitespaces(String str) {
        // Remove all whitespace expect when startsWith " ".
        if (str.startsWith(" ")) {
            return str;
        }
        return str.replaceAll("\\s", "");
    }
}
