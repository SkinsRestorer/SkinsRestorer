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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import skinsrestorer.shared.format.Profile;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.ConfigStorage;

public class MojangAPI {

	private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";

	private static final String altskinurl = ConfigStorage.getInstance().GET_SKIN_PROFILE_URL;

	private static final String haspaidurl = "https://minecraft.net/haspaid.jsp?user=";

	public static boolean hasPaid(String name) throws MalformedURLException {
		return Boolean.parseBoolean(readURL(new URL(haspaidurl + name)));
	}

	public static Profile getProfile(String name) throws MalformedURLException {

		if (hasPaid(name))
			return new Profile(readURL(new URL(uuidurl + name)).substring(7, 39), name);
		else
			return null;
	}

	public static SkinProfile getSkinProfile(String uuid, String name) throws MalformedURLException {
		String output = readURL(new URL(skinurl + uuid + "?unsigned=false"));

		String valuebeg = "value\":\"";
		String mid = "\",\"signature\":\"";
		String signatureend = "\"}]}";

		if (output == null || output.contains("TooManyRequestsException")) {

			if (!ConfigStorage.getInstance().MCAPI_ENABLED)
				return null;

			output = readURL(new URL(altskinurl.replace("{uuid}", uuid))).replace(" ", "");
			System.out.println("[SkinsRestorer] Using McAPI for this skin..");

			String uid = getStringBetween(output, "\"properties\": ", "\"properties_decoded\":");

			if (uid.toLowerCase().contains("null"))
				return null;

			valuebeg = ",\"value\": \"";
			mid = "\",\"signature\": \"";
			signatureend = "\"},\"properties";
		}

		String value = getStringBetween(output, valuebeg, mid);
		String signature = getStringBetween(output, mid, signatureend);

		return new SkinProfile(new Profile(uuid, name), new SkinProperty("textures", value, signature),
				System.currentTimeMillis(), true);
	}

	private static String readURL(URL url) {
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setUseCaches(false);

			String line;
			StringBuilder output = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			while ((line = in.readLine()) != null)
				output.append(line);

			in.close();

			return output.toString();
		} catch (Exception e) {
		}
		return null;
	}

	private static String getStringBetween(final String base, final String begin, final String end) {

		Pattern patbeg = Pattern.compile(begin);
		Pattern patend = Pattern.compile(end);

		int resbeg = 0;
		int resend = base.length() - 1;

		Matcher matbeg = patbeg.matcher(base);

		while (matbeg.find())
			resbeg = matbeg.end();

		Matcher matend = patend.matcher(base);

		while (matend.find())
			resend = matend.start();

		return base.substring(resbeg, resend);
	}

}
