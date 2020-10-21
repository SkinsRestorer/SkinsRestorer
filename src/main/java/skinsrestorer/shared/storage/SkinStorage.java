package skinsrestorer.shared.storage;

import lombok.Getter;
import lombok.Setter;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.utils.*;

import javax.sql.RowSet;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SkinStorage {

    private Class<?> property;
    @Getter
    @Setter
    private MySQL mysql;
    private File folder;
    private boolean isBungee = false;
    private boolean isVelocity = false;
    private boolean isSponge = false;
    @Getter
    @Setter
    private MojangAPI mojangAPI;

    private void load() {
        try {
            property = Class.forName("org.spongepowered.api.profile.property.ProfileProperty");
            isSponge = true;
        } catch (Exception exe) {
            try {
                property = Class.forName("com.mojang.authlib.properties.Property");
            } catch (Exception e) {
                try {
                    property = Class.forName("net.md_5.bungee.connection.LoginResult$Property");
                    isBungee = true;
                } catch (Exception ex) {
                    try {
                        property = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property");
                    } catch (Exception exc) {
                        try {
                            property = Class.forName("com.velocitypowered.api.util.GameProfile$Property");
                            isVelocity = true;
                        } catch (Exception exce) {
                            System.out.println("[SkinsRestorer] Could not find a valid Property class! Plugin will not work properly");
                        }
                    }
                }
            }
        }
    }

    public SkinStorage() {
        this.load();
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
                    this.getOrCreateSkinForPlayer(skin);
                }
            } catch (SkinRequestException e) {
                //removing skin from list
                toRemove.add(skin);
                System.out.println("[SkinsRestorer] [WARNING] DefaultSkin '" + skin + "' could not be found or requested! Removing from list..");
                if (Config.DEBUG)
                    System.out.println("[SkinsRestorer] [DEBUG] DefaultSkin '" + skin + "' error: " + e.getReason());
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

        if (textures != null) {
            return textures;
        }

        // No cached skin found, get from MojangAPI, save and return
        try {
            textures = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(skin));
            if (textures == null) {
                throw new SkinRequestException(Locale.ERROR_NO_SKIN);
            }
            setSkinData(skin, textures);
        } catch (SkinRequestException e) {
            if (!silent)
                throw new SkinRequestException(e.getReason());
        } catch (Exception e) {
            e.printStackTrace();
            if (!silent)
                throw new SkinRequestException(Locale.WAIT_A_MINUTE);
        }

        return textures;
    }

    public Object getOrCreateSkinForPlayer(final String name) throws SkinRequestException {
        return getOrCreateSkinForPlayer(name, false);
    }

    /**
     * Returns the custom skin name that player has set.
     *
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

                BufferedReader buf = new BufferedReader(new FileReader(playerFile));

                String line, skin = null;
                if ((line = buf.readLine()) != null)
                    skin = line;

                buf.close();

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
                        Object skin = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(name));
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

                BufferedReader buf = new BufferedReader(new FileReader(skinFile));

                String line, value = "", signature = "", timestamp = "";
                for (int i = 0; i < 3; i++)
                    if ((line = buf.readLine()) != null)
                        if (value.isEmpty()) {
                            value = line;
                        } else if (signature.isEmpty()) {
                            signature = line;
                        } else {
                            timestamp = line;
                        }
                buf.close();

                if (updateOutdated && isOld(Long.parseLong(timestamp))) {
                    Object skin = this.getMojangAPI().getSkinProperty(this.getMojangAPI().getUUID(name));
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

            if (playerFile.exists())
                playerFile.delete();
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

            if (skinFile.exists())
                skinFile.delete();
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
                if (!playerFile.exists())
                    playerFile.createNewFile();

                FileWriter writer = new FileWriter(playerFile);

                writer.write(skin);
                writer.close();
            } catch (Exception e) {
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

                if (!skinFile.exists())
                    skinFile.createNewFile();

                FileWriter writer = new FileWriter(skinFile);

                writer.write(value + "\n" + signature + "\n" + timestamp);
                writer.close();
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
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> {
                        sb.append("|").append(skin);
                    });
                    filterBy = " WHERE Nick RLIKE '" + sb.substring(1) + "'";
                } else {
                    StringBuilder sb = new StringBuilder();
                    Config.CUSTOM_GUI_SKINS.forEach(skin -> {
                        sb.append(", '").append(skin).append("'");
                    });
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
                    if (Config.CUSTOM_GUI_ONLY){ //Show only Config.CUSTOM_GUI_SKINS in the gui
                        for (String Guiskins : Config.CUSTOM_GUI_SKINS){
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

                        BufferedReader buf = new BufferedReader(new FileReader(skinFile));

                        String line, value = "", signature = "", timestamp = "";
                        for (int i2 = 0; i2 < 3; i2++)
                            if ((line = buf.readLine()) != null)
                                if (value.isEmpty()) {
                                    value = line;
                                } else if (signature.isEmpty()) {
                                    signature = line;
                                } else {
                                    timestamp = line;
                                }
                        buf.close();

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
     * @param player      - Player name
     * @param clear       - Should we ignore player set skin?
     * @return            - setSkin or DefaultSkin, if player has no setSkin or default skin, we return his name
     */
    public String getDefaultSkinNameIfEnabled(String player, boolean clear) {
        // Remove all non [a-z_] chars to allow pre/sub fixes
        player = player.replaceAll("\\W", "");
        if (Config.DEFAULT_SKINS_ENABLED && !Config.DEFAULT_SKINS.isEmpty()) {
            // don't return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (this.getMojangAPI().getUUID(player) != null) {
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

    //wip
    /*public boolean iscustom(String skin) {
        try {
            //code
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    } */
}
