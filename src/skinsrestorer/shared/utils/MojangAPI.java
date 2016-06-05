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
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException.Reason;

/** Class by Blackfire62 **/

public class MojangAPI {

	private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";

	private static final String altskinurl = ConfigStorage.getInstance().GET_SKIN_PROFILE_URL;

	public static Profile getProfile(String name) throws MalformedURLException, SkinFetchFailedException {
		name = name.toLowerCase();
		String output = readURL(new URL(uuidurl + name));

		if (output == null || output.isEmpty())
			throw new SkinFetchUtils.SkinFetchFailedException(Reason.NO_PREMIUM_PLAYER);

		return new Profile(output.substring(7, 39), name);
	}

	public static SkinProfile getSkinProfile(String uuid, String name)
			throws MalformedURLException, SkinFetchFailedException {
		name = name.toLowerCase();
		String output = readURL(new URL(skinurl + uuid + "?unsigned=false"));

		String sigbeg = "[{\"signature\":\"";
		String mid = "\",\"name\":\"textures\",\"value\":\"";
		String valend = "\"}]}";

		if (output == null || output.contains("TooManyRequestsException")) {

			if (!ConfigStorage.getInstance().MCAPI_ENABLED)
				throw new SkinFetchUtils.SkinFetchFailedException(Reason.RATE_LIMITED);

			output = readURL(new URL(altskinurl.replace("{uuid}", uuid))).replace(" ", "");
			System.out.println("[SkinsRestorer] Using McAPI for skin " + name);

			String uid = getStringBetween(output, "{\"uuid\":\"", "\",\"uuid_formatted");

			if (uid.toLowerCase().contains("null"))
				throw new SkinFetchUtils.SkinFetchFailedException(Reason.MCAPI_FAILED);

			sigbeg = "\"signature\":\"";
			mid = "\",\"name\":\"textures\",\"value\":\"";
			valend = "\"}],\"properties_decoded";

		}

		String value = getStringBetween(output, mid, valend).replace("\\/", "/");
		String signature = getStringBetween(output, sigbeg, mid).replace("\\/", "/");

		return new SkinProfile(new Profile(uuid, name), new SkinProperty("textures", value, signature),
				System.currentTimeMillis());
	}

	private static String readURL(URL url) {
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);

			String line;
			StringBuilder output = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			while ((line = in.readLine()) != null)
				output.append(line);

			in.close();

			return output.toString();
		} catch (Exception e) {
			return null;
		}
	}

	private static String getStringBetween(final String base, final String begin, final String end) {

		Pattern patbeg = Pattern.compile(Pattern.quote(begin));
		Pattern patend = Pattern.compile(Pattern.quote(end));

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
