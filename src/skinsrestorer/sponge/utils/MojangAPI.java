package skinsrestorer.sponge.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.property.ProfileProperty;

/** Class by Blackfire62 **/

public class MojangAPI {

	private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";

	private static final String altskinurl = "https://mcapi.ca/name/uuid/";
	private static final String altuuidurl = "https://us.mc-api.net/v3/uuid/";

	public static Optional<String> getUUID(String name) {
		String output = readURL(uuidurl + name);

		if (output == null || output.isEmpty() || output.contains("\"error\":\"TooManyRequestsException\"")) {

			output = readURL(altuuidurl + name);

			if (output == null || output.isEmpty() || output.contains("\"error\":\"Unknown Username\""))
				return Optional.empty();

			String idbeg = "\"uuid\":\"";
			String idend = "\"}";

			return Optional.<String> of(getStringBetween(output, idbeg, idend));
		}

		return Optional.<String> of(output.substring(7, 39));
	}

	public static Optional<ProfileProperty> getSkinProperty(String uuid) {
		String output = readURL(skinurl + uuid + "?unsigned=false");

		String sigbeg = "[{\"signature\":\"";
		String mid = "\",\"name\":\"textures\",\"value\":\"";
		String valend = "\"}]";

		if (output == null || output.isEmpty() || output.contains("TooManyRequestsException")) {

			output = readURL(altskinurl + uuid).replace(" ", "");

			String uid = getStringBetween(output, "{\"uuid\":\"", "\",\"uuid_formatted");

			if (uid.toLowerCase().contains("null"))
				return Optional.empty();

			sigbeg = "\"signature\":\"";
			mid = "\",\"name\":\"textures\",\"value\":\"";
			valend = "\"}],\"properties_decoded";
		}

		String value = getStringBetween(output, mid, valend).replace("\\/", "/");
		String signature = getStringBetween(output, sigbeg, mid).replace("\\/", "/");

		return Optional.<ProfileProperty> of(
				Sponge.getServer().getGameProfileManager().createProfileProperty("textures", value, signature));
	}

	private static String readURL(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "SkinsRestorer");
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
