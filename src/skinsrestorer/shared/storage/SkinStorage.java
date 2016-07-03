/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package skinsrestorer.shared.storage;

import java.io.File;
import java.util.Random;

import javax.sql.rowset.CachedRowSet;

import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.YamlConfig;

public class SkinStorage {

	private static YamlConfig cache;
	private static MySQL mysql;

	public static void init() {
		cache = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "cache");

	}

	public static void init(MySQL mysql) {
		SkinStorage.mysql = mysql;
	}

	public static void removePlayerSkin(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {
			mysql.execute("delete from " + Config.MYSQL_PLAYERTABLE + " where Nick=?", name);
		} else {
			cache.set("Players." + name + ".Skin", null);
			cache.set("Players." + name, null);
			cache.set(name, null);
		}

	}

	public static void removeSkinData(String name) {
		name = name.toLowerCase();
		if (Config.USE_MYSQL) {
			mysql.execute("delete from " + Config.MYSQL_SKINTABLE + " where Nick=?", name);
		} else {
			cache.set("Skins." + name + ".Value", null);
			cache.set("Skins." + name + ".Signature", null);
			cache.set("Skins." + name, null);
			cache.set(name, null);
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
		} else
			cache.set("Players." + name + ".Skin", skin);

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
			value = (String) ReflectionUtil.invokeMethod(textures.getClass(), textures, "getValue");
			signature = (String) ReflectionUtil.invokeMethod(textures.getClass(), textures, "getSignature");
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
			cache.set("Skins." + name + ".Value", value);
			cache.set("Skins." + name + ".Signature", signature);
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

			String value = cache.getString("Skins." + name + ".Value");
			String signature = cache.getString("Skins." + name + ".Signature");

			if (value.isEmpty() || signature.isEmpty())
				return null;

			Object textures = createProperty("textures", value, cache.getString("Skins." + name + ".Signature"));

			return textures;
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
			String skin = cache.getString("Players." + name + ".Skin");

			if (skin == null || skin.isEmpty() || skin.equalsIgnoreCase(name)) {
				removePlayerSkin(name);
				skin = name;
			}

			return skin;
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
	 * @return Property object (either Mojang or Bungee)
	 */
	public static Object createProperty(String name, String value, String signature) {
		try {
			return ReflectionUtil.invokeConstructor(Class.forName("com.mojang.authlib.properties.Property"),
					new Class<?>[] { String.class, String.class, String.class }, name, value, signature);
		} catch (Exception e) {
			try {
				return ReflectionUtil.invokeConstructor(
						Class.forName("net.md_5.bungee.connection.LoginResult$Property"),
						new Class<?>[] { String.class, String.class, String.class }, name, value, signature);
			} catch (Exception ex) {
				try {
					return ReflectionUtil.invokeConstructor(
							Class.forName("net.minecraft.util.com.mojang.authlib.properties.Property"),
							new Class<?>[] { String.class, String.class, String.class }, name, value, signature);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}
		return null;
	}
}
