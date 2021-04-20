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

import lombok.Getter;
import lombok.Setter;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.MojangAPI;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.shared.utils.property.*;

import javax.sql.RowSet;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SkinStorage {
    private final Platform platform;
    private final SRLogger logger;
    private Class<?> property;
    @Getter
    @Setter
    private MySQL mysql;
    private File skinsFolder;
    private File playersFolder;
    @Getter
    @Setter
    private MojangAPI mojangAPI;

    public SkinStorage(SRLogger logger, Platform platform) {
        this.logger = logger;
        this.platform = platform;

        if (platform == Platform.BUKKIT) {
            property = BukkitProperty.class;
        } else if (platform == Platform.BUNGEECORD) {
            property = BungeeProperty.class;
        } else if (platform == Platform.VELOCITY) {
            property = VelocityProperty.class;
        }
    }

    public void loadFolders(File pluginFolder) {
        skinsFolder = new File(pluginFolder, "Skins");
        skinsFolder.mkdirs();

        playersFolder = new File(pluginFolder, "Players");
        playersFolder.mkdirs();
    }

    public void preloadDefaultSkins() {
        if (!Config.DEFAULT_SKINS_ENABLED)
            return;

        List<String> toRemove = new ArrayList<>();
        Config.DEFAULT_SKINS.forEach(skin -> {
            //todo: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    getOrCreateSkinForPlayer(skin, false);
                }
            } catch (SkinRequestException e) {
                // removing skin from list
                toRemove.add(skin);
                logger.warning("[WARNING] DefaultSkin '" + skin + "' could not be found or requested! Removing from list..");

                logger.debug("[DEBUG] DefaultSkin '" + skin + "' error: ");
                if (Config.DEBUG)
                    e.printStackTrace();
            }
        });
        Config.DEFAULT_SKINS.removeAll(toRemove);
    }


    public IProperty createProperty(String name, String value, String signature) {
        // use our own property class if we are on sponge
        if (platform == Platform.SPONGE) {
            GenericProperty p = new GenericProperty();

            p.setName(name);
            p.setValue(value);
            p.setSignature(signature);

            return p;
        }

        try {
            return (IProperty) ReflectionUtil.invokeConstructor(property,
                    new Class<?>[]{String.class, String.class, String.class}, name, value, signature);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * This methods seeks out players actual skin (chosen or not) and returns
     * either null (if no skin data found) or the property object conatining all
     * the skin data.
     * <p>
     * Also, it schedules a skin update to stay up to date with skin changes.
     *
     * @param name   Player name to search skin for
     * @param silent Whether to throw errors or not
     * @throws SkinRequestException If MojangAPI lookup errors
     **/
    public IProperty getOrCreateSkinForPlayer(final String name, boolean silent) throws SkinRequestException {
        String skin = getPlayerSkin(name);

        if (skin == null) {
            skin = name.toLowerCase();
        }

        IProperty textures = getSkinData(skin);

        if (textures == null) {
            // No cached skin found, get from MojangAPI, save and return
            try {
                if (C.validMojangUsername(skin)) {
                    textures = getMojangAPI().getSkinProperty(getMojangAPI().getUUID(skin, true));
                }

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
     * <p>
     * Returns null if player has no custom skin set.
     **/
    public String getPlayerSkin(String name) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);

            if (crs != null)
                try {
                    final String skin = crs.getString("Skin");

                    //maybe useless
                    if (skin.isEmpty()) {
                        removePlayerSkin(name);
                        return null;
                    }

                    return skin;
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } else {
            name = removeForbiddenChars(name);
            File playerFile = new File(playersFolder, name + ".player");

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
                    removePlayerSkin(name);
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
     * @param name           - Skin name
     * @param updateOutdated - On true we update the skin if expired
     **/
    // #getSkinData() also create while we have #getOrCreateSkinForPlayer()
    public IProperty getSkinData(String name, boolean updateOutdated) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", name);
            if (crs != null)
                try {
                    final String value = crs.getString("Value");
                    final String signature = crs.getString("Signature");
                    final String timestamp = crs.getString("timestamp");

                    return updateOutdated(name, updateOutdated, value, signature, timestamp);
                } catch (Exception e) {
                    removeSkinData(name);
                    logger.info("Unsupported player format.. removing (" + name + ").");
                }
        } else {
            name = removeWhitespaces(name);
            name = removeForbiddenChars(name);
            File skinFile = new File(skinsFolder, name + ".skin");

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

                return updateOutdated(name, updateOutdated, value, signature, timestamp);
            } catch (Exception e) {
                removeSkinData(name);
                logger.info("Unsupported player format.. removing (" + name + ").");
            }
        }

        return null;
    }

    private IProperty updateOutdated(String name, boolean updateOutdated, String value, String signature, String timestamp) throws SkinRequestException {
        if (updateOutdated && C.validMojangUsername(name) && isOld(Long.parseLong(timestamp))) {
            IProperty skin = getMojangAPI().getSkinProperty(getMojangAPI().getUUID(name, true));

            if (skin != null) {
                setSkinData(name, skin);
                return skin;
            }
        }

        return createProperty("textures", value, signature);
    }

    public IProperty getSkinData(String name) {
        return getSkinData(name, true);
    }

    /**
     * Checks if updating skins is disabled and if skin expired
     *
     * @param timestamp in milliseconds
     * @return true if skin is outdated
     */
    private boolean isOld(long timestamp) {
        // Don't update if timestamp is not 0 or update is disabled.
        if (timestamp == 0 || Config.DISABLE_AUTO_UPDATE_SKIN)
            return false;

        return timestamp + TimeUnit.MINUTES.toMillis(Config.SKIN_EXPIRES_AFTER) <= System.currentTimeMillis();
    }

    /**
     * Removes custom players skin name from database
     *
     * @param name - Players name
     **/
    public void removePlayerSkin(String name) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);
        } else {
            name = removeForbiddenChars(name);
            File playerFile = new File(playersFolder, name + ".player");

            if (playerFile.exists()) {
                try {
                    Files.delete(playerFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Removes skin data from database
     *
     * @param name - Skin name
     **/
    public void removeSkinData(String name) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("DELETE FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", name);
        } else {
            name = removeWhitespaces(name);
            name = removeForbiddenChars(name);
            File skinFile = new File(skinsFolder, name + ".skin");

            if (skinFile.exists()) {
                try {
                    Files.delete(skinFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Saves custom player's skin name to database
     *
     * @param name - Players name
     * @param skin - Skin name
     **/
    public void setPlayerSkin(String name, String skin) {
        name = name.toLowerCase();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_PLAYERTABLE + " (Nick, Skin) VALUES (?,?)"
                    + " ON DUPLICATE KEY UPDATE Skin=?", name, skin, skin);
        } else {
            name = removeForbiddenChars(name);
            File playerFile = new File(playersFolder, name + ".player");

            try {
                if (!playerFile.exists() && !playerFile.createNewFile())
                    throw new IOException("Could not create player file!");

                try (FileWriter writer = new FileWriter(playerFile)) {
                    skin = removeWhitespaces(skin);
                    skin = removeForbiddenChars(skin);

                    writer.write(skin);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves skin data to database
     *
     * @param name      - Skin name
     * @param textures  - Property object
     * @param timestamp - timestamp string in millis
     **/
    public void setSkinData(String name, IProperty textures, String timestamp) {
        name = name.toLowerCase();
        String value = textures.getValue();
        String signature = textures.getSignature();

        if (Config.MYSQL_ENABLED) {
            mysql.execute("INSERT INTO " + Config.MYSQL_SKINTABLE + " (Nick, Value, Signature, timestamp) VALUES (?,?,?,?)"
                    + " ON DUPLICATE KEY UPDATE Value=?, Signature=?, timestamp=?", name, value, signature, timestamp, value, signature, timestamp);
        } else {
            // Remove all whitespace
            name = name.replaceAll("\\s", "");
            //Escape all Windows / Linux forbidden printable ASCII characters
            name = name.replaceAll("[\\\\/:*?\"<>|]", "·");
            File skinFile = new File(skinsFolder, name + ".skin");

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

    public void setSkinData(String name, IProperty textures) {
        setSkinData(name, textures, Long.toString(System.currentTimeMillis()));
    }

    // todo: CUSTOM_GUI
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
                        list.put(crs.getString("Nick").toLowerCase(), createProperty("textures", crs.getString("Value"), crs.getString("Signature")));
                    i++;
                } while (crs.next());
            } catch (java.sql.SQLException ignored) {
            }

            // When not using mysql
        } else {

            //filter out non "*.skin" files.
            FilenameFilter skinFileFilter = (dir, name) -> name.endsWith(".skin");

            String[] fileNames = skinsFolder.list(skinFileFilter);

            if (fileNames == null)
                //todo should this not also be null if no skin is valid?
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

    // Todo: remove duplicated code and use existing methods....
    // Todo: needs a lot refactoring!
    // Todo: We should _always_ return our own Property object and cast to the platform specific one just before actually setting the skin.
    // Todo: That should save lots of duplicated code
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
     * @param skin
     * @return True on updated
     * @throws SkinRequestException On updating disabled OR invalid username + api error
     */
    // skin update [include custom skin flag]
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean updateSkinData(String skin) throws SkinRequestException {
        if (!C.validMojangUsername(skin))
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);

        // Check if updating is disabled for skin (by timestamp = 0)
        String timestamp = "";
        if (Config.MYSQL_ENABLED) {
            RowSet crs = mysql.query("SELECT timestamp FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", skin);
            if (crs != null)
                try {
                    timestamp = crs.getString("timestamp");
                } catch (Exception ignored) {
                }
        } else {
            skin = removeWhitespaces(skin);
            skin = removeForbiddenChars(skin);

            File skinFile = new File(skinsFolder, skin + ".skin");

            try {
                if (!skinFile.exists()) {
                    timestamp = "";
                } else {
                    try (BufferedReader buf = new BufferedReader(new FileReader(skinFile))) {
                        for (int i = 0; i < 3; i++) {
                            String line = buf.readLine();

                            if (i == 2)
                                timestamp = line;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (timestamp.equals("0") || C.validMojangUsername(skin))
            throw new SkinRequestException(Locale.ERROR_UPDATING_CUSTOMSKIN);

        // Update Skin
        try {
            IProperty textures = getMojangAPI().getSkinPropertyMojang(getMojangAPI().getUUIDMojang(skin));

            if (textures != null) {
                setSkinData(skin, textures);
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

    public String getDefaultSkinNameIfEnabled(String player) {
        return getDefaultSkinNameIfEnabled(player, false);
    }

    /**
     * Filters player name to exclude non [a-z_]
     * Checks and process default skin.
     * IF no default skin:
     * 1: Return player if clear
     * 2: Return skin if found
     * Else: return player
     *
     * @param player - Player name
     * @param clear  - return player instead of his set skin
     * @return - setSkin or DefaultSkin, if player has no setSkin or default skin, we return his name
     */
    public String getDefaultSkinNameIfEnabled(String player, boolean clear) {
        // LTrim and RTrim player name
        player = player.replaceAll("^\\\\s+", "");
        player = player.replaceAll("\\\\s+$", "");

        if (Config.DEFAULT_SKINS_ENABLED && !Config.DEFAULT_SKINS.isEmpty()) {
            // don't return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (C.validMojangUsername(player) || getMojangAPI().getUUID(player, true) != null) {
                        // player is premium, return his skin name instead of default skin
                        return player;
                    }
                } catch (SkinRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set or we want to clear to default
            if (getPlayerSkin(player) == null || clear) {
                final List<String> skins = Config.DEFAULT_SKINS;
                int r = 0;
                if (skins.size() > 1)
                    r = new Random().nextInt(skins.size());
                String randomSkin = skins.get(r);
                // return player name if there are no default skins set
                return randomSkin != null ? randomSkin : player;
            }
        }

        // return the player name if we want to clear the skin
        if (clear)
            return player;

        // return the custom skin user has set
        String skin = getPlayerSkin(player);

        // null if player has no custom skin, we'll return his name then
        return skin == null ? player : skin;
    }

    private String removeForbiddenChars(String str) {
        // Escape all Windows / Linux forbidden printable ASCII characters
        return str.replaceAll("[\\\\/:*?\"<>|]", "·");
    }

    private String removeWhitespaces(String str) {
        // Remove all whitespace
        return str.replaceAll("\\s", "");
    }

    public enum Platform {
        BUKKIT(true, false, false, false),
        BUNGEECORD(false, true, false, false),
        SPONGE(false, false, true, false),
        VELOCITY(false, false, false, true);

        @Getter
        private final boolean isBukkit;
        @Getter
        private final boolean isBungee;
        @Getter
        private final boolean isSponge;
        @Getter
        private final boolean isVelocity;

        Platform(boolean isBukkit, boolean isBungee, boolean isSponge, boolean isVelocity) {
            this.isBukkit = isBukkit;
            this.isBungee = isBungee;
            this.isSponge = isSponge;
            this.isVelocity = isVelocity;
        }
    }
}
