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

package skinsrestorer.shared.utils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import skinsrestorer.libs.org.json.simple.JSONArray;
import skinsrestorer.libs.org.json.simple.JSONObject;
import skinsrestorer.libs.org.json.simple.parser.JSONParser;
import skinsrestorer.libs.org.json.simple.parser.ParseException;
import skinsrestorer.shared.format.Profile;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.apacheutils.IOUtils;

public class MojangAPI {
		private static final String profileurl = "https://api.mojang.com/profiles/minecraft";
		private static final String mcapipurl = ConfigStorage.getInstance().GET_PROFILE_URL;
		public static Profile getProfile(String nick) throws SkinFetchFailedException, IOException, ParseException {
			
			//open connection
			HttpsURLConnection connection = (HttpsURLConnection) setupConnection(new URL(profileurl));
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			
			//write body
			DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
			writer.write(JSONArray.toJSONString(Arrays.asList(nick)).getBytes(StandardCharsets.UTF_8));
			writer.flush();
			writer.close();
			
			//check response code
			if (connection.getResponseCode() == 429) {
				connection = (HttpsURLConnection) setupConnection(new URL(mcapipurl.replace("{username}", nick).replace("'", String.valueOf('"'))));
				connection.setRequestMethod("GET");
				connection.setRequestProperty("Content-Type", "application/json");
				
				//write body
			    writer = new DataOutputStream(connection.getOutputStream());
				writer.flush();
				writer.close();
				
				InputStream is = connection.getInputStream();
				String result = IOUtils.toString(is, StandardCharsets.UTF_8);
				IOUtils.closeQuietly(is);
				JSONArray jsonProfiles = (JSONArray) new JSONParser().parse(result);
				if (jsonProfiles.size() > 0) {
					JSONObject jsonProfile = (JSONObject) jsonProfiles.get(0);
					System.out.println("[MCAPI] Got skin profile for "+nick);
					return new Profile((String) jsonProfile.get("uuid"), (String) jsonProfile.get("name"));
				}
				throw new SkinFetchFailedException(SkinFetchFailedException.Reason.NO_PREMIUM_PLAYER);
			}
			
			//read response
			InputStream is = connection.getInputStream();
			String result = IOUtils.toString(is, StandardCharsets.UTF_8);
			IOUtils.closeQuietly(is);
			JSONArray jsonProfiles = (JSONArray) new JSONParser().parse(result);
			if (jsonProfiles.size() > 0) {
				JSONObject jsonProfile = (JSONObject) jsonProfiles.get(0);
				return new Profile((String) jsonProfile.get("id"), (String) jsonProfile.get("name"));
			}
			throw new SkinFetchFailedException(SkinFetchFailedException.Reason.NO_PREMIUM_PLAYER);
		}

		private static final String skullbloburl = "https://sessionserver.mojang.com/session/minecraft/profile/";
		private static final String mcapiurl = ConfigStorage.getInstance().GET_SKIN_PROFILE_URL;
		public static SkinProfile getSkinProfile(String id) throws IOException, ParseException, SkinFetchFailedException {
			
			//open connection
			HttpsURLConnection connection =  (HttpsURLConnection) setupConnection(new URL(skullbloburl+id.replace("-", "")+"?unsigned=false"));
			
			//check response code
			if (connection.getResponseCode() == 429) {
				connection = (HttpsURLConnection) setupConnection(new URL(mcapiurl.replace("{uuid}", id.replace("-", ""))));
				System.out.println(LocaleStorage.getInstance().TRYING_TO_USE_NCAPI);
			}
			
			//read response
			InputStream is = connection.getInputStream();
			String result = IOUtils.toString(is, StandardCharsets.UTF_8);
			IOUtils.closeQuietly(is);
			JSONObject obj = (JSONObject) new JSONParser().parse(result);
			String username = (String) obj.get("name");
			JSONArray properties = (JSONArray) (obj).get("properties");
			for (int i = 0; i < properties.size(); i++) {
				JSONObject property = (JSONObject) properties.get(i);
				String name = (String) property.get("name");
				String value = (String) property.get("value");
				String signature = (String) property.get("signature");
				if (name.equals("textures")) {
					return new SkinProfile(new Profile(id, username), new SkinProperty(name, value, signature), System.currentTimeMillis(), false);
				}
			}
			throw new SkinFetchFailedException(SkinFetchFailedException.Reason.NO_SKIN_DATA);
		}
		private static URLConnection setupConnection(URL url) throws IOException {
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			return connection;
		}
	}
