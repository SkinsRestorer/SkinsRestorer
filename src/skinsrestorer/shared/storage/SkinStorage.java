package skinsrestorer.shared.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;

import javax.sql.rowset.CachedRowSet;

import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinStorage {

	private static Class<?> property;
	private static MySQL mysql;
	private static File folder;

	static {
		try {
			property = Class.forName("com.mojang.authlib.properties.Property");
		} catch (Exception e) {
			try {
				property = Class.forName("net.md_5.bungee.connection.LoginResult$Property");
			} catch (Exception ex) {
				try {
					property = Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property");
				} catch (Exception exc) {
					System.out.println(
							"[SkinsRestorer] Could not find a valid Property class! Plugin will not work properly");
				}
			}
		}
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

	public static void removePlayerSkin(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {
			mysql.execute("delete from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);
		} else {
			File playerFile = new File(
					folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

			if (playerFile.exists())
				playerFile.delete();
		}

	}

	public static void removeSkinData(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {
			mysql.execute("delete from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);
		} else {
			File skinFile = new File(
					folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

			if (skinFile.exists())
				skinFile.delete();
		}

	}

	public static void setPlayerSkin(String name, String skin) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {
			CachedRowSet crs = mysql.query("select * from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);

			if (crs == null)
				mysql.execute("insert into " + Config.MYSQL_PLAYERTABLE + " (Nick, Skin) values (?,?)", name, skin);
			else
				mysql.execute("update " + Config.MYSQL_PLAYERTABLE + " set Skin=? where Nick=?", skin, name);
		} else {
			File playerFile = new File(
					folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

			try {
				if (!playerFile.exists())
					playerFile.createNewFile();

				if (skin.equalsIgnoreCase(name)) {
					playerFile.delete();
					return;
				}

				FileWriter writer = new FileWriter(playerFile);

				writer.write(skin);
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Param textures is either BungeeCord's property or Mojang's property
	 * 
	 **/
	public static void setSkinData(String name, Object textures) {
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
			CachedRowSet crs = mysql.query("select * from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);

			if (crs == null)
				mysql.execute("insert into " + Config.MYSQL_SKINTABLE + " (Nick, Value, Signature) values (?,?,?)",
						name, value, signature);
			else
				mysql.execute("update " + Config.MYSQL_SKINTABLE + " set Value=?, Signature=? where Nick=?", value,
						signature, name);
		} else {
			File skinFile = new File(
					folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

			try {
				if (value.isEmpty() || signature.isEmpty())
					return;

				if (!skinFile.exists())
					skinFile.createNewFile();

				FileWriter writer = new FileWriter(skinFile);

				writer.write(value + "\n" + signature);
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property
	 * 
	 * @return com.mojang.authlib.properties.Property (1.8+)
	 *         <p>
	 *         net.md_5.bungee.connection.LoginResult.Property (BungeeCord)
	 *         <p>
	 *         net.minecraft.util.com.mojang.authlib.properties.Property
	 *         (1.7.10)
	 * 
	 **/
	public static Object getSkinData(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {

			CachedRowSet crs = mysql.query("select * from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);

			if (crs != null) {
				try {
					String value = crs.getString("Value");
					String signature = crs.getString("Signature");

					return createProperty("textures", value, signature);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;

		} else {
			File skinFile = new File(
					folder.getAbsolutePath() + File.separator + "Skins" + File.separator + name + ".skin");

			try {
				if (!skinFile.exists())
					skinFile.createNewFile();

				BufferedReader buf = new BufferedReader(new FileReader(skinFile));

				String line, value = "", signature = "";
				for (int i = 0; i < 2; i++) {
					if ((line = buf.readLine()) != null) {
						if (value == null)
							value = line;
						else
							signature = line;
					}
				}

				buf.close();

				if (!value.isEmpty() && !signature.isEmpty())
					return SkinStorage.createProperty("textures", value, signature);
				else
					skinFile.delete();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	public static String getPlayerSkin(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {

			CachedRowSet crs = mysql.query("select * from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);

			if (crs != null) {
				try {
					String skin = crs.getString("Skin");

					if (skin.isEmpty() || skin.equalsIgnoreCase(name)) {
						removePlayerSkin(name);
						skin = name;
					}

					return skin;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;

		} else {
			File playerFile = new File(
					folder.getAbsolutePath() + File.separator + "Players" + File.separator + name + ".player");

			try {
				if (!playerFile.exists())
					playerFile.createNewFile();

				BufferedReader buf = new BufferedReader(new FileReader(playerFile));

				String line, skin = null;
				if ((line = buf.readLine()) != null)
					skin = line;

				buf.close();

				if (skin.equalsIgnoreCase(name)) {
					playerFile.delete();
				}

				return skin;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property
	 * 
	 * @return com.mojang.authlib.properties.Property (1.8+)
	 *         <p>
	 *         net.md_5.bungee.connection.LoginResult.Property (BungeeCord)
	 *         <p>
	 *         net.minecraft.util.com.mojang.authlib.properties.Property
	 *         (1.7.10)
	 * 
	 **/
	public static Object getOrCreateSkinForPlayer(String name) {
		name = name.toLowerCase();
		String skin = getPlayerSkin(name);

		if (skin == null || skin.isEmpty())
			skin = name;

		Object textures = getSkinData(skin);
		if (textures == null) {
			if (Config.DEFAULT_SKINS_ENABLED) {
				textures = getSkinData(Config.DEFAULT_SKINS.get(new Random().nextInt(Config.DEFAULT_SKINS.size())));
				if (textures == null)
					textures = createProperty("textures", "", "");
			} else
				textures = createProperty("textures", "", "");
		}

		return textures;
	}

	/**
	 * 
	 * @param name
	 * @param value
	 * @param signature
	 * @return Property object (either oldMojang, newMojang or Bungee one)
	 */
	public static Object createProperty(String name, String value, String signature) {
		try {
			return ReflectionUtil.invokeConstructor(property,
					new Class<?>[] { String.class, String.class, String.class }, name, value, signature);
		} catch (Exception e) {
		}

		return null;
	}

}
