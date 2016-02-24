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

package skinsrestorer.bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.rowset.CachedRowSet;

import skinsrestorer.libs.com.google.gson.Gson;
import skinsrestorer.libs.com.google.gson.GsonBuilder;
import skinsrestorer.libs.com.google.gson.JsonIOException;
import skinsrestorer.libs.com.google.gson.reflect.TypeToken;
import skinsrestorer.shared.format.Profile;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.utils.IOUils;

public class SkinStorage {

	private static SkinStorage instance = new SkinStorage();;
	private static MySQL mysql;

	public static SkinStorage getInstance() {
		return instance;
	}

	private static String cachefile;
	private static Gson gson;
	private static Type type;
	private static ConcurrentHashMap<String, SkinProfile> skins;
	protected static File pluginfolder;

	public static void init(File pluginfolder) {
		cachefile = "cache.json";
		gson = new GsonBuilder().registerTypeHierarchyAdapter(SkinProfile.class, new SkinProfile.GsonTypeAdapter())
				.setPrettyPrinting().create();
		type = new TypeToken<ConcurrentHashMap<String, SkinProfile>>() {
		}.getType();
		skins = new ConcurrentHashMap<String, SkinProfile>();

		SkinStorage.pluginfolder = pluginfolder;
		instance.loadData();
	}

	public static void init(MySQL mysql) {
		SkinStorage.mysql = mysql;
	}

	public boolean isSkinDataForced(String name) {
		if (ConfigStorage.getInstance().USE_MYSQL) {
			// w/e
			return false;
		} else {
			SkinProfile profile = skins.get(name.toLowerCase());
			if (profile != null && profile.isForced()) {
				return true;
			}
			return false;
		}
	}

	public void removeSkinData(String name) {
		if (ConfigStorage.getInstance().USE_MYSQL) {
			mysql.execute(mysql.prepareStatement("delete from "+ConfigStorage.getInstance().MYSQL_TABLE+" where Nick=?", name));
		} else
			skins.remove(name.toLowerCase());
	}

	public void setSkinData(String name, SkinProfile profile) {
		if (ConfigStorage.getInstance().USE_MYSQL) {
			CachedRowSet crs = mysql.query(mysql.prepareStatement("select * from "+ConfigStorage.getInstance().MYSQL_TABLE+" where Nick=?", name));

			if (crs == null)
				mysql.execute(mysql.prepareStatement("insert into "+ConfigStorage.getInstance().MYSQL_TABLE+" (Nick, Value, Signature) values (?,?,?)", name,
						profile.getSkinProperty().getValue(), profile.getSkinProperty().getSignature()));
			else
				mysql.execute(mysql.prepareStatement("update "+ConfigStorage.getInstance().MYSQL_TABLE+" set Value=?, Signature=? where Nick=?",
						profile.getSkinProperty().getValue(), profile.getSkinProperty().getSignature(), name));
		} else
			skins.put(name.toLowerCase(), profile.cloneAsForced());
	}

	// Justin case
	public SkinProfile getOrCreateSkinData(String name) {
		if (ConfigStorage.getInstance().USE_MYSQL) {

			SkinProfile sp;
			if ((sp = getSkinData(name)) == null)
				return new SkinProfile(new Profile(null, name), null, 0, false);
			else
				return sp;
		} else {

			SkinProfile emptyprofile = new SkinProfile(new Profile(null, name), null, 0, false);
			SkinProfile profile = skins.putIfAbsent(name, emptyprofile);

			return profile != null ? profile : emptyprofile;

		}

	}

	public SkinProfile getSkinData(String name) {
		if (ConfigStorage.getInstance().USE_MYSQL) {

			CachedRowSet crs = mysql
					.query(mysql.prepareStatement("select * from "+ConfigStorage.getInstance().MYSQL_TABLE+" where Nick=?", name.toLowerCase()));

			if (crs == null) {
				return null;
			} else {
				try {
					String value = crs.getString("Value");
					String signature = crs.getString("Signature");

					return new SkinProfile(new Profile(null, name), new SkinProperty("textures", value, signature), 0,
							false);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return null;

		} else {

			SkinProfile profile = skins.get(name.toLowerCase());
			if (profile == null) {
				return null;
			}
			return profile;

		}
	}

	public void loadData() {
		try (InputStreamReader reader = IOUils.createReader(new File(pluginfolder, cachefile))) {
			Map<String, SkinProfile> gsondata = gson.fromJson(reader, type);
			if (gsondata != null) {
				skins.putAll(gsondata);
			}
		} catch (JsonIOException | IOException e) {
		}
	}

	public void saveData() {
		pluginfolder.mkdirs();
		
		try (OutputStreamWriter writer = IOUils.createWriter(new File(pluginfolder, cachefile))) {
			ConcurrentHashMap<String, SkinProfile> toSerialize = new ConcurrentHashMap<String, SkinProfile>();
			for (Entry<String, SkinProfile> entry : skins.entrySet()) {

				if (entry.getValue().shouldSerialize()) {
					toSerialize.put(entry.getKey(), entry.getValue());
				}
			}
			gson.toJson(toSerialize, type, writer);
		} catch (JsonIOException | IOException e) {
		}
	}
}
