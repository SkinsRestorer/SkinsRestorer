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

import javax.sql.rowset.CachedRowSet;

import skinsrestorer.shared.format.Profile;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.utils.DataFiles;
import skinsrestorer.shared.utils.MySQL;

public class SkinStorage {

	private static SkinStorage instance = new SkinStorage();
	private static DataFiles cache;
	private static MySQL mysql;

	public static SkinStorage getInstance() {
		return instance;
	}

	public static void init() {
		cache = new DataFiles("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "cache");

	}

	public static void init(MySQL mysql) {
		SkinStorage.mysql = mysql;
	}

	public void removePlayerSkin(String name) {
		name = name.toLowerCase();
		if (ConfigStorage.getInstance().USE_MYSQL) {
			mysql.execute(mysql.prepareStatement(
					"delete from " + ConfigStorage.getInstance().MYSQL_PLAYERTABLE + " where Nick=?", name));
		} else {
			name = name.toLowerCase();
			if (cache.getString("Players." + name) != null) {
				cache.removePath("Players." + name);
				cache.save();
			}
		}

	}

	public void removeSkinData(String name) {
		name = name.toLowerCase();
		if (ConfigStorage.getInstance().USE_MYSQL) {
			mysql.execute(mysql.prepareStatement(
					"delete from " + ConfigStorage.getInstance().MYSQL_SKINTABLE + " where Nick=?", name));
		} else {
			name = name.toLowerCase();
			if (cache.getString(name) != null) {
				cache.removePath("Skins." + name);
				cache.removePath(name);
				cache.save();
			}
		}

	}

	public void setPlayerSkin(String name, String skin) {
		name = name.toLowerCase();
		if (ConfigStorage.getInstance().USE_MYSQL) {
			CachedRowSet crs = mysql.query(mysql.prepareStatement(
					"select * from " + ConfigStorage.getInstance().MYSQL_PLAYERTABLE + " where Nick=?", name));

			if (crs == null)
				mysql.execute(mysql.prepareStatement(
						"insert into " + ConfigStorage.getInstance().MYSQL_PLAYERTABLE + " (Nick, Skin) values (?,?)",
						name, skin));
			else
				mysql.execute(mysql.prepareStatement(
						"update " + ConfigStorage.getInstance().MYSQL_PLAYERTABLE + " set Skin=? where Nick=?", skin,
						name));
		} else {
			cache.set("Players." + name + ".Skin", skin);
			cache.save();
		}
	}

	public void setSkinData(SkinProfile profile) {
		if (ConfigStorage.getInstance().USE_MYSQL) {
			CachedRowSet crs = mysql.query(mysql.prepareStatement(
					"select * from " + ConfigStorage.getInstance().MYSQL_SKINTABLE + " where Nick=?",
					profile.getName().toLowerCase()));

			if (crs == null)
				mysql.execute(mysql.prepareStatement(
						"insert into " + ConfigStorage.getInstance().MYSQL_SKINTABLE
								+ " (Nick, Value, Signature, Timestamp) values (?,?,?,?)",
						profile.getName().toLowerCase(), profile.getSkinProperty().getValue(),
						profile.getSkinProperty().getSignature(), String.valueOf(System.currentTimeMillis())));
			else
				mysql.execute(mysql.prepareStatement(
						"update " + ConfigStorage.getInstance().MYSQL_SKINTABLE
								+ " set Value=?, Signature=?, Timestamp=? where Nick=?",
						profile.getSkinProperty().getValue(), profile.getSkinProperty().getSignature(),
						String.valueOf(System.currentTimeMillis()), profile.getName().toLowerCase()));
		} else {

			cache.set("Skins." + profile.getName() + ".Value", profile.getSkinProperty().getValue());
			cache.set("Skins." + profile.getName() + ".Signature", profile.getSkinProperty().getSignature());
			cache.set("Skins." + profile.getName() + ".Timestamp", System.currentTimeMillis());
			cache.save();
		}
	}

	public SkinProfile getSkinData(String name) {
		name = name.toLowerCase();
		if (ConfigStorage.getInstance().USE_MYSQL) {

			CachedRowSet crs = mysql.query(mysql.prepareStatement(
					"select * from " + ConfigStorage.getInstance().MYSQL_SKINTABLE + " where Nick=?",
					name.toLowerCase()));

			if (crs == null) {
				return null;
			} else {
				try {
					String value = crs.getString("Value");
					String signature = crs.getString("Signature");
					String timestamp = crs.getString("Timestamp");

					return new SkinProfile(new Profile(null, name), new SkinProperty("textures", value, signature),
							Long.valueOf(timestamp));

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;

		} else {
			Long timestamp = System.currentTimeMillis();

			try {
				timestamp = Long.parseLong(cache.getString("Skins." + name + ".Timestamp"));
			} catch (Throwable e) {
			}

			SkinProfile profile = new SkinProfile(new Profile(null, name), new SkinProperty("textures",
					cache.getString("Skins." + name + ".Value"), cache.getString("Skins." + name + ".Signature")),
					timestamp);

			if (profile.getSkinProperty().getSignature() == null)
				return null;

			return profile;

		}
	}

	public String getPlayerSkin(String name) {
		name = name.toLowerCase();
		if (ConfigStorage.getInstance().USE_MYSQL) {

			CachedRowSet crs = mysql.query(mysql.prepareStatement(
					"select * from " + ConfigStorage.getInstance().MYSQL_PLAYERTABLE + " where Nick=?",
					name.toLowerCase()));

			if (crs == null) {
				return null;
			} else {
				try {
					String skin = crs.getString("Skin");

					if (skin.equalsIgnoreCase(name))
						removePlayerSkin(name);

					return skin;

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;

		} else {
			String skin = cache.getString("Players." + name + ".Skin");

			if (skin != null && skin.equalsIgnoreCase(name))
				removePlayerSkin(name);

			return skin;
		}
	}

	public SkinProfile getOrCreateSkinForPlayer(String name) {
		name = name.toLowerCase();
		String skin = getPlayerSkin(name);

		if (skin == null)
			skin = name;

		SkinProfile skinprofile = getSkinData(skin);
		if (skinprofile == null)
			skinprofile = new SkinProfile(new Profile(null, name), new SkinProperty("textures", "", ""), 0);

		return skinprofile;
	}
}
