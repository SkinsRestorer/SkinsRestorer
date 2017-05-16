package skinsrestorer.shared.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.rowset.CachedRowSet;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinStorage {

	private static Class<?> property;
	private static MySQL mysql;
	private static File folder;
	private static ExecutorService exe;
	private static boolean isBungee;

	static {
		try {
			exe = Executors.newCachedThreadPool();
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
	 * @param name
	 *            - Players name
	 * 
	 **/
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

	/**
	 * Removes skin data from database
	 * 
	 * @param name
	 *            - Skin name
	 * 
	 **/
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

	/**
	 * Saves custom player's skin name to dabase
	 * 
	 * @param name
	 *            - Players name
	 * 
	 * @param skin
	 *            - Skin name
	 * 
	 **/
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
	 * @param name
	 *            - Skin name
	 * @param textures
	 *            - Property object
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
	 * Returns property object containing skin data of the wanted skin
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
					return null;

				BufferedReader buf = new BufferedReader(new FileReader(skinFile));

				String line, value = "", signature = "";
				for (int i = 0; i < 2; i++) {
					if ((line = buf.readLine()) != null) {
						if (value.isEmpty())
							value = line;
						else
							signature = line;
					}
				}

				buf.close();

				return SkinStorage.createProperty("textures", value, signature);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}
	}

	/*
	 * Returns the custom skin name that player has set.
	 * 
	 * Returns null if player has no custom skin set. (even if its his own name)
	 */
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

			return name;

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

				if (skin.equalsIgnoreCase(name))
					playerFile.delete();

				return skin;

			} catch (Exception e) {
				e.printStackTrace();
			}

			return name;
		}
	}

	/**
	 * This methods seeks out players actual skin (chosen or not) and returns
	 * either null (if no skin data found) or the property object conatining all
	 * the skin data.
	 * 
	 * Also, it schedules a skin update to stay up to date with skin changes.
	 * 
	 * 
	 * @return Property object
	 * 
	 **/
	public static Object getOrCreateSkinForPlayer(final String name) {
		String skin = getPlayerSkin(name);

		if (skin == null)
			skin = name.toLowerCase();

		Object textures = getSkinData(skin);
		if (textures == null) {
			if (Config.DEFAULT_SKINS_ENABLED)
				textures = getSkinData(Config.DEFAULT_SKINS.get(new Random().nextInt(Config.DEFAULT_SKINS.size())));

		}
		// Schedule skin update for next login
		final String sname = skin;
		final Object oldprops = textures;
		exe.submit(new Runnable() {

			@Override
			public void run() {

				try {
					Object props = null;
					try {
						props = MojangAPI.getSkinProperty(sname, MojangAPI.getUUID(sname));
					} catch (SkinRequestException e) {
						return;
					}

					if (props == null)
						return;

					boolean shouldUpdate = false;

					String value = Base64Coder.decodeString((String) ReflectionUtil.invokeMethod(props, "getValue"));

					String urlbeg = "url\":\"";
					String urlend = "\"}}";

					String newurl = MojangAPI.getStringBetween(value, urlbeg, urlend);

					try {
						value = Base64Coder.decodeString((String) ReflectionUtil.invokeMethod(oldprops, "getValue"));

						String oldurl = MojangAPI.getStringBetween(value, urlbeg, urlend);

						shouldUpdate = !oldurl.equals(newurl);
					} catch (Exception e) {
						shouldUpdate = true;
					}

					setSkinData(sname, props);

					if (shouldUpdate) {
						if (isBungee)
							skinsrestorer.bungee.SkinApplier.applySkin(name);
						else {
							SkinsRestorer.getInstance().getFactory().applySkin(org.bukkit.Bukkit.getPlayer(name),
									props);
							SkinsRestorer.getInstance().getFactory().updateSkin(org.bukkit.Bukkit.getPlayer(name));
						}
					}
				} catch (Exception e) {
				}
			}

		});

		return textures;
	}

	/*
	 * Return the executor service which hosts all the skin updates
	 */
	public static ExecutorService getExecutor() {
		return exe;
	}

	/**
	 * Returns property object with filled data depending on the server version
	 * and type
	 * 
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
