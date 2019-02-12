package skinsrestorer.shared.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SkinStorage {

    private static Class<?> property;
    private static MySQL mysql;
    private static File folder;
    private static boolean isBungee;

    static {
        try {
            property = Class.forName("com.mojang.authlib.properties.Property");
            isBungee = false;
        } catch (Exception e) {
            try {
                property = Class.forName("net.md_5.bungee.connection.LoginResult$Property");
                isBungee = true;
            } catch (Exception ex) {
                try {
                    property = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property");
                    isBungee = false;
                } catch (Exception exc) {
                    System.out.println(
                            "[SkinsRestorer] Could not find a valid Property class! Plugin will not work properly");
                }
            }
        }
    }

    public static Object createProperty(String name, String value, String signature) {
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
     * @return Property object
     **/
    public static Object getOrCreateSkinForPlayer(final String name, boolean silent) throws SkinRequestException {
        String skin = getPlayerSkin(name);

        if (skin == null) {
            skin = name.toLowerCase();
        }

        // System.out.println("Skin: " + skin);

        Object textures = null;
        // if (Config.DEFAULT_SKINS_ENABLED) {
        //     textures = getSkinData(Config.DEFAULT_SKINS.get(new Random().nextInt(Config.DEFAULT_SKINS.size())));
        // }

        textures = getSkinData(skin);

        if (textures != null) {
            return textures;
        }

        // Schedule skin update for next login
        final String sname = skin;
        final Object oldprops = textures;
        // No cached skin found, get from MojangAPI, save and return
        try {
            Object props = null;

            textures = MojangAPI.getSkinProperty(MojangAPI.getUUID(sname));

            boolean shouldUpdate = false;

            String value = Base64Coder.decodeString((String) ReflectionUtil.invokeMethod(textures, "getValue"));

            JsonElement element = new JsonParser().parse(value);
            JsonObject obj = element.getAsJsonObject();
            JsonObject textureObj = obj.get("textures").getAsJsonObject();

            String newurl;
            if (textureObj.has("SKIN")) {
                newurl = textureObj.get("SKIN").getAsJsonObject().get("url").getAsString();
            }

            // TODO: This is useless! oldprops is always null and always triggers an "shouldUpdate".
            /*try {
                value = Base64Coder.decodeString((String) ReflectionUtil.invokeMethod(oldprops, "getValue"));

                String oldurl = MojangAPI.getStringBetween(value, urlbeg, urlend);

                System.out.println("oldurl: " + oldurl);

                shouldUpdate = !oldurl.equals(newurl);
            } catch (Exception e) {
                e.printStackTrace();
                shouldUpdate = true;
            }*/

            setSkinData(sname, textures);

            if (shouldUpdate)
                if (isBungee)
                    skinsrestorer.bungee.SkinApplier.applySkin(name);
                else {
                    SkinsRestorer.getInstance().getFactory().applySkin(org.bukkit.Bukkit.getPlayer(name), textures);
                }
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

    public static Object getOrCreateSkinForPlayer(final String name) throws SkinRequestException {
        return getOrCreateSkinForPlayer(name, false);
    }

    /*
     * Returns the custom skin name that player has set.
     *
     * Returns null if player has no custom skin set. (even if its his own name)
     */
    public static String getPlayerSkin(String name) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("select * from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);

            if (crs != null)
                try {
                    String skin = crs.getString("Skin");

                    if (skin.isEmpty() || skin.equalsIgnoreCase(name)) {
                        removePlayerSkin(name);
                        return null;
                    }

                    return skin;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            return null;

        } else {
            File playerFile = new File(
                    folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

            try {
                if (!playerFile.exists())
                    return null;

                BufferedReader buf = new BufferedReader(new FileReader(playerFile));

                String line, skin = null;
                if ((line = buf.readLine()) != null)
                    skin = line;

                buf.close();

                assert skin != null;
                if (skin.equalsIgnoreCase(name))
                    playerFile.delete();

                return skin;

            } catch (IOException e) {
                e.printStackTrace();
            }

            return name;
        }
    }

    /**
     * Returns property object containing skin data of the wanted skin
     **/
    public static Object getSkinData(String name) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL) {

            RowSet crs = mysql.query("select * from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);
            if (crs != null)
                try {
                    String value = crs.getString("Value");
                    String signature = crs.getString("Signature");
                    String timestamp = crs.getString("timestamp");

                    if (isOld(Long.valueOf(timestamp))) {
                        //removeSkinData(name); Remove that cause its useless
                        Object skin = MojangAPI.getSkinProperty(MojangAPI.getUUID(name));
                        if (skin != null) {
                            SkinStorage.setSkinData(name, skin);
                            //TODO: return skin object from MojangAPI instead old one!
                            //return skin;
                        }
                    }
                    return createProperty("textures", value, signature);

                } catch (Exception e) {
                    removeSkinData(name);
                    System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
                }

            return null;

        } else {
            File skinFile = new File(
                    folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

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
                if (isOld(Long.valueOf(timestamp))) {
                    //removeSkinData(name);
                    Object skin = MojangAPI.getSkinProperty(MojangAPI.getUUID(name));
                    if (skin != null) {
                        SkinStorage.setSkinData(name, skin);
                        //TODO: return skin object from MojangAPI instead old one!
                    }
                }
                return SkinStorage.createProperty("textures", value, signature);

            } catch (Exception e) {
                removeSkinData(name);
                System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
            }

            return null;
        }
    }

    private static boolean isOld(long timestamp) {
        return timestamp + TimeUnit.MINUTES.toMillis(Config.SKIN_EXPIRES_AFTER) <= System.currentTimeMillis();
    }

    public static void init(File pluginFolder) {
        folder = pluginFolder;
        File tempFolder = new File(folder.getAbsolutePath() + File.separator + "Skins" + File.separator);
        tempFolder.mkdirs();
        tempFolder = new File(folder.getAbsolutePath() + File.separator + "Players" + File.separator);
        tempFolder.mkdirs();
    }

    public static void init(MySQL mysql) {
        SkinStorage.mysql = mysql;
    }

    /**
     * Removes custom players skin name from database
     *
     * @param name - Players name
     **/
    public static void removePlayerSkin(String name) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL)
            mysql.execute("delete from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);
        else {
            File playerFile = new File(
                    folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

            if (playerFile.exists())
                playerFile.delete();
        }

    }

    /**
     * Removes skin data from database
     *
     * @param name - Skin name
     **/
    public static void removeSkinData(String name) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL)
            mysql.execute("delete from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);
        else {
            File skinFile = new File(
                    folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

            if (skinFile.exists())
                skinFile.delete();
        }

    }

    /**
     * Saves custom player's skin name to dabase
     *
     * @param name - Players name
     * @param skin - Skin name
     **/
    public static void setPlayerSkin(String name, String skin) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("select * from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);

            if (crs == null)
                mysql.execute("insert into " + Config.MYSQL_PLAYERTABLE + " (Nick, Skin) values (?,?)", name, skin);
            else
                mysql.execute("update " + Config.MYSQL_PLAYERTABLE + " set Skin=? where Nick=?", skin, name);
        } else {
            File playerFile = new File(
                    folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

            try {
                if (skin.equalsIgnoreCase(name) && playerFile.exists()) {
                    playerFile.delete();
                    return;
                }

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
     * @param name     - Skin name
     * @param textures - Property object
     **/
    public static void setSkinData(String name, Object textures) {
        name = name.toLowerCase();
        String value = "";
        String signature = "";
        String timestamp = "";
        try {
            value = (String) ReflectionUtil.invokeMethod(textures, "getValue");
            signature = (String) ReflectionUtil.invokeMethod(textures, "getSignature");
            timestamp = String.valueOf(System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Config.USE_MYSQL) {
            RowSet crs = mysql.query("select * from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);

            if (crs == null)
                mysql.execute("insert into " + Config.MYSQL_SKINTABLE + " (Nick, Value, Signature, timestamp) values (?,?,?,?)",
                        name, value, signature, timestamp);
            else
                mysql.execute("update " + Config.MYSQL_SKINTABLE + " set Value=?, Signature=?, timestamp=? where Nick=?", value,
                        signature, timestamp, name);
        } else {
            File skinFile = new File(
                    folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

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

    public static Map<String, Object> getSkins(int number) {
        ConcurrentHashMap<String, Object> thingy = new ConcurrentHashMap<String, Object>();
        Map<String, Object> list = new TreeMap<String, Object>(thingy);
        String path = SkinsRestorer.getInstance().getDataFolder() + "/Skins/";
        File folder = new File(path);
        String[] fileNames = folder.list();
        int i = 0;
        assert fileNames != null;
        for (String file : fileNames) {
            if (i >= number) {
                list.put(file.replace(".skin", ""), SkinStorage.getSkinDataMenu(file.replace(".skin", "")));
            }
            i++;
        }
        return list;
    }


    //Getting skin data for menu
    public static Object getSkinDataMenu(String name) {
        name = name.toLowerCase();
        if (Config.USE_MYSQL) {

            RowSet crs = mysql.query("select * from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);
            if (crs != null)
                try {
                    String value = crs.getString("Value");
                    String signature = crs.getString("Signature");
                    @SuppressWarnings("unused")
                    String timestamp = crs.getString("timestamp");

                    return createProperty("textures", value, signature);

                } catch (Exception e) {
                    System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
                }

            return null;

        } else {
            File skinFile = new File(
                    folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

            try {
                if (!skinFile.exists())
                    return null;

                BufferedReader buf = new BufferedReader(new FileReader(skinFile));

                @SuppressWarnings("unused")
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
                return SkinStorage.createProperty("textures", value, signature);

            } catch (Exception e) {
                System.out.println("[SkinsRestorer] Unsupported player format.. removing (" + name + ").");
            }

            return null;
        }
    }


    public static boolean forceUpdateSkinData(String skin) {
        try {
            Object textures = MojangAPI.getSkinPropertyBackup(MojangAPI.getUUIDBackup(skin));
            if (textures != null) {
                SkinStorage.setSkinData(skin, textures);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // If clear is true, it doesn't return the custom skin a user has set
    public static String getDefaultSkinNameIfEnabled(String player, boolean clear) {
        if (Config.DEFAULT_SKINS_ENABLED) {
            // dont return default skin name for premium players if enabled
            if (!Config.DEFAULT_SKINS_PREMIUM) {
                // check if player is premium
                try {
                    if (MojangAPI.getUUID(player) != null) {
                        // player is premium, return his skin name instead of default skin
                        return player;
                    }
                } catch (SkinRequestException ignored) {
                    // Player is not premium catching exception here to continue returning a default skin name
                }
            }

            // return default skin name if user has no custom skin set or we want to clear to default
            if (SkinStorage.getPlayerSkin(player) == null || clear) {
                List<String> skins = Config.DEFAULT_SKINS;
                int randomNum = (int) (Math.random() * skins.size());
                String randomSkin = skins.get(randomNum);
                // return player name if there are no default skins set
                return randomSkin != null ? randomSkin : player;
            }
        }

        // return the player name if we want to clear the skin
        if (clear)
            return player;

        // return the custom skin user has set
        String skin = SkinStorage.getPlayerSkin(player);

        // null if player has no custom skin, we'll return his name then
        return skin == null ? player : skin;
    }

    public static String getDefaultSkinNameIfEnabled(String player) {
        return getDefaultSkinNameIfEnabled(player, false);
    }
}