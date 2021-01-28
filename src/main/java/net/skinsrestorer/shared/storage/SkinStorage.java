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
import net.skinsrestorer.shared.utils.Property;
import net.skinsrestorer.shared.utils.ReflectionUtil;

import javax.sql.RowSet;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SkinStorage {
    private Class<?> property;

    private @Getter
    @Setter
    MySQL mysql;
    private File folder;

    @SuppressWarnings("unused")
    private boolean isBukkit;
    @SuppressWarnings("unused")
    private boolean isBungee;
    private boolean isSponge;
    @SuppressWarnings("unused")
    private boolean isVelocity;

    private @Getter
    @Setter
    MojangAPI mojangAPI;

    public SkinStorage(Platform platform) {
        isBukkit = platform.isBukkit();
        isBungee = platform.isBungee();
        isSponge = platform.isSponge();
        isVelocity = platform.isVelocity();

        try {
            if (isBukkit) {
                try {
                    property = Class.forName("com.mojang.authlib.properties.Property");
                } catch (ClassNotFoundException e) {
                    property = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property");
                }
            } else if (isBungee) {
                property = Class.forName("net.md_5.bungee.connection.LoginResult$Property");
            } else if (isSponge) {
                property = Class.forName("org.spongepowered.api.profile.property.ProfileProperty");
            } else if (isVelocity) {
                property = Class.forName("com.velocitypowered.api.util.GameProfile$Property");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void loadFolders(File pluginFolder) {
        folder = pluginFolder;

        File tempFolder = new File(folder.getAbsolutePath() + File.separator + "Skins" + File.separator);
        tempFolder.mkdirs();

        tempFolder = new File(folder.getAbsolutePath() + File.separator + "Players" + File.separator);
        tempFolder.mkdirs();
    }

    public void preloadDefaultSkins() {
        if (!Config.DEFAULT_SKINS_ENABLED)
            return;

        List<String> toRemove = new ArrayList<>();
        Config.DEFAULT_SKINS.forEach(skin -> {
            //todo: add try for skinUrl
            try {
                if (!C.validUrl(skin)) {
                    this.getOrCreateSkinForPlayer(skin, false);
                }
            } catch (SkinRequestException e) {
                //removing skin from list
                toRemove.add(skin);
                System.out.println("[SkinsRestorer] [WARNING] DefaultSkin '" + skin + "' could not be found or requested! Removing from list..");

                if (Config.DEBUG)
                    System.out.println("[SkinsRestorer] [DEBUG] DefaultSkin '" + skin + "' error: " + e.getMessage());
            }
        });
        Config.DEFAULT_SKINS.removeAll(toRemove);
    }


    public Object createProperty(String name, String value, String signature) {
        // use our own propery class if we are on skinsrestorer.sponge
        if (isSponge) {
            Property p = new Property();

            p.setName(name);
            p.setValue(value);
            p.setSignature(signature);

            return p;
        }

        try {
            return ReflectionUtil.invokeConstructor(property,
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
     * @return Property    Platform specific Property Object
     * @throws SkinRequestException If MojangAPI lookup errors
     **/
    public Object getOrCreateSkinForPlayer(final String name, boolean silent) throws SkinRequestException {
        String skin = getPlayerSkin(name);

        if (skin == null) {
            skin = name.toLowerCase();
        }

        Object textures = getSkinData(skin);

        if (textures != null)
            return textures;

        // No cached skin found, get from MojangAPI, save and return
        try {
            textures = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(skin, true));

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

        return textures;
    }

    /**
     * Returns the custom skin name that player has set.
     * <p>
     * Returns null if player has no custom skin set.
     **/
    public String getPlayerSkin(String name) {
        name = name.toLowerCase();

        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);

            if (crs != null)
                try {
                    String skin = crs.getString("Skin");

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
            //Escape all windows / linux forbidden printable ASCII characters
            name = name.replaceAll("[\\\\/:*?\"<>|]", "·");
            File playerFile = new File(folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

            try {
                if (!playerFile.exists())
                    return null;

                String skin;
                try (BufferedReader buf = new BufferedReader(new FileReader(playerFile))) {
                    String line;
                    skin = null;
                    if ((line = buf.readLine()) != null)
                        skin = line;
                }

                //maybe useless
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
    //getSkinData also create while we have getOrCreateSkinForPlayer
    public Object getSkinData(String name, boolean updateOutdated) {
        name = name.toLowerCase();

        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", name);
            if (crs != null)
                try {
                    String value = crs.getString("Value");
                    String signature = crs.getString("Signature");
                    String timestamp = crs.getString("timestamp");

                    if (updateOutdated && isOld(Long.parseLong(timestamp))) {
                        Object skin = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(name, true));
                        if (skin != null) {
                            this.setSkinData(name, skin);
                            return skin;
                        }
                    }

                    return createProperty("textures", value, signature);

                } catch (Exception e) {
                    removeSkinData(name);
                    System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
                }

        } else {
            File skinFile = new File(folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

            try {
                if (!skinFile.exists())
                    return null;

                String value;
                String signature;
                String timestamp;

                try (BufferedReader buf = new BufferedReader(new FileReader(skinFile))) {

                    String line;
                    value = "";
                    signature = "";
                    timestamp = "";
                    for (int i = 0; i < 3; i++)
                        if ((line = buf.readLine()) != null)
                            if (value.isEmpty()) {
                                value = line;
                            } else if (signature.isEmpty()) {
                                signature = line;
                            } else {
                                timestamp = line;
                            }
                }

                if (updateOutdated && isOld(Long.parseLong(timestamp))) {
                    Object skin = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(name, true));

                    if (skin != null) {
                        this.setSkinData(name, skin);
                        return skin;
                    }
                }

                return this.createProperty("textures", value, signature);

            } catch (Exception e) {
                removeSkinData(name);
                System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
            }
        }
        return null;
    }

    public Object getSkinData(String name) {
        return this.getSkinData(name, true);
    }

    private boolean isOld(long timestamp) {
        return timestamp + TimeUnit.MINUTES.toMillis(Config.SKIN_EXPIRES_AFTER) <= System.currentTimeMillis();
    }

    /**
     * Removes custom players skin name from database
     *
     * @param name - Players name
     **/
    public void removePlayerSkin(String name) {
        name = name.toLowerCase();

        if (Config.USE_MYSQL) {
            mysql.execute("DELETE FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);
        } else {
            //Escape all windows / linux forbidden printable ASCII characters
            name = name.replaceAll("[\\\\/:*?\"<>|]", "·");
            File playerFile = new File(folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

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

        if (Config.USE_MYSQL) {
            mysql.execute("DELETE FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", name);
        } else {
            File skinFile = new File(folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

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

        if (Config.USE_MYSQL) {
            //todo optimization
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_PLAYERTABLE + " WHERE Nick=?", name);

            if (crs == null)
                mysql.execute("INSERT INTO " + Config.MYSQL_PLAYERTABLE + " (Nick, Skin) VALUES (?,?)", name, skin);
            else
                mysql.execute("UPDATE " + Config.MYSQL_PLAYERTABLE + " SET Skin=? WHERE Nick=?", skin, name);
        } else {
            //Escape all windows / linux forbidden printable ASCII characters
            name = name.replaceAll("[\\\\/:*?\"<>|]", "·");
            File playerFile = new File(folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

            try {
                if (!playerFile.exists() && !playerFile.createNewFile())
                    throw new IOException("Could not create player file!");

                try (FileWriter writer = new FileWriter(playerFile)) {
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
    public void setSkinData(String name, Object textures, String timestamp) {
        name = name.toLowerCase();
        String value = "";
        String signature = "";

        try {
            value = (String) ReflectionUtil.invokeMethod(textures, "getValue");
            signature = (String) ReflectionUtil.invokeMethod(textures, "getSignature");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("SELECT * FROM " + Config.MYSQL_SKINTABLE + " WHERE Nick=?", name);

            if (crs == null)
                mysql.execute("INSERT INTO " + Config.MYSQL_SKINTABLE + " (Nick, Value, Signature, timestamp) VALUES (?,?,?,?)",
                        name, value, signature, timestamp);
            else
                mysql.execute("UPDATE " + Config.MYSQL_SKINTABLE + " SET Value=?, Signature=?, timestamp=? WHERE Nick=?",
                        value, signature, timestamp, name);
        } else {
            File skinFile = new File(folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

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

    public void setSkinData(String name, Object textures) {
        setSkinData(name, textures, Long.toString(System.currentTimeMillis()));
    }

    //todo: CUSTOM_GUI
    // seems to be that crs order is ignored...
    public Map<String, Object> getSkins(int number) {
        //Using mysql
        if (Config.USE_MYSQL) {
            Map<String, Object> list = new TreeMap<>();
            String filterBy = "";
            String orderBy = "Nick";

            // custom gui
            if (Config.CUSTOM_GUI_ENABLED) {
                if (Config.CUSTOM_GUI_ONLY) {
                    StringBuilder sb = new StringBuilder();
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> sb.append("|").append(skin));

                    filterBy = " WHERE Nick RLIKE '" + sb.substring(1) + "'";
                } else {
                    StringBuilder sb = new StringBuilder();
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
            return list;

            // When not using mysql
        } else {
            Map<String, Object> list = new TreeMap<>();
            String path = folder.getAbsolutePath() + File.separator + "Skins" + File.separator;
            File folder = new File(path);

            //filter out non "*.skin" files.
            FilenameFilter skinFileFilter = (dir, name) -> name.endsWith(".skin");

            String[] fileNames = folder.list(skinFileFilter);

            if (fileNames == null)
                return list;

            Arrays.sort(fileNames);
            int i = 0;
            for (String file : fileNames) {
                String skinName = file.replace(".skin", "");
                if (i >= number) {
                    if (Config.CUSTOM_GUI_ONLY) { //Show only Config.CUSTOM_GUI_SKINS in the gui
                        for (String Guiskins : Config.CUSTOM_GUI_SKINS) {
                            if (skinName.toLowerCase().contains(Guiskins.toLowerCase()))
                                list.put(skinName.toLowerCase(), this.getSkinData(skinName, false));
                        }
                    } else {
                        list.put(skinName.toLowerCase(), this.getSkinData(skinName, false));
                    }
                }
                i++;
            }
            return list;
        }
    }

    // Todo: remove duplicated code and use existing methods....
    // Todo: needs a lot refactoring!
    // Todo: We should _always_ return our own Property object and cast to the platform specific one just before actually setting the skin.
    // Todo: That should save lots of duplicated code
    public Map<String, Property> getSkinsRaw(int number) {
        Map<String, Property> list = new TreeMap<>();
        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("SELECT Nick, Value, Signature FROM " + Config.MYSQL_SKINTABLE + " ORDER BY `Nick`");
            int i = 0;
            int foundSkins = 0;
            try {
                do {
                    if (i >= number && foundSkins <= 26) {
                        Property prop = new Property();
                        prop.setName("textures");
                        prop.setValue(crs.getString("Value"));
                        prop.setSignature(crs.getString("Signature"));
                        list.put(crs.getString("Nick"), prop);
                        foundSkins++;
                    }
                    i++;
                } while (crs.next());
            } catch (java.sql.SQLException ignored) {
                ignored.printStackTrace();
            }
        } else {
            String path = folder.getAbsolutePath() + File.separator + "Skins" + File.separator;
            File folder = new File(path);

            //filter out non "*.skin" files.
            FilenameFilter skinFileFilter = (dir, name) -> name.endsWith(".skin");

            String[] fileNames = folder.list(skinFileFilter);

            if (fileNames == null)
                return list;

            Arrays.sort(fileNames);
            int i = 0;
            int foundSkins = 0;
            for (String file : fileNames) {
                String skinName = file.replace(".skin", "");

                File skinFile = new File(path + file);
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
                                if ((line = buf.readLine()) != null)
                                    if (value.isEmpty()) {
                                        value = line;
                                    } else if (signature.isEmpty()) {
                                        signature = line;
                                    }
                        }

                        Property prop = new Property();
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

    // skin update [include custom skin flag]
    public boolean forceUpdateSkinData(String skin) {
        try {
            Object textures = this.getMojangAPI().getSkinPropertyMojang(this.getMojangAPI().getUUIDMojang(skin));

            if (textures != null) {
                this.setSkinData(skin, textures);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // If clear is true, it doesn't return the custom skin a user has set

    /**
     * Filters player name to exclude non [a-z_]
     * Checks and process default skin.
     * IF no default skin:
     * 1: Return player if clear
     * 2: Return skin if found
     * Else: return player
     *
     * @param player - Player name
     * @param clear  - Should we ignore player set skin?
     * @return - setSkin or DefaultSkin, if player has no setSkin or default skin, we return his name
     */
    public String getDefaultSkinNameIfEnabled(String player, boolean clear) {
        // Remove all non [a-z_] chars to allow pre/sub fixes
        player = player.replaceAll("\\W", "");

        if (Config.DEFAULT_SKINS_ENABLED && !Config.DEFAULT_SKINS.isEmpty()) {
            // don't return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (this.getMojangAPI().getUUID(player, true) != null) {
                        // player is premium, return his skin name instead of default skin
                        return player;
                    }
                } catch (SkinRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set or we want to clear to default
            if (this.getPlayerSkin(player) == null || clear) {
                final List<String> skins = Config.DEFAULT_SKINS;
                int r = 0;
                if (skins.size() > 1)
                    r = (int) (Math.random() * skins.size());
                String randomSkin = skins.get(r);
                // return player name if there are no default skins set
                return randomSkin != null ? randomSkin : player;
            }
        }

        // return the player name if we want to clear the skin
        if (clear)
            return player;

        // return the custom skin user has set
        String skin = this.getPlayerSkin(player);

        // null if player has no custom skin, we'll return his name then
        return skin == null ? player : skin;
    }

    public String getDefaultSkinNameIfEnabled(String player) {
        return getDefaultSkinNameIfEnabled(player, false);
    }

    public enum Platform {
        BUKKIT(true, false, false, false),
        BUNGEECORD(false, true, false, false),
        SPONGE(false, false, true, false),
        VELOCITY(false, false, false, true);

        private final @Getter
        boolean isBukkit;
        private final @Getter
        boolean isBungee;
        private final @Getter
        boolean isSponge;
        private final @Getter
        boolean isVelocity;

        Platform(boolean isBukkit, boolean isBungee, boolean isSponge, boolean isVelocity) {
            this.isBukkit = isBukkit;
            this.isBungee = isBungee;
            this.isSponge = isSponge;
            this.isVelocity = isVelocity;
        }
    }
}
