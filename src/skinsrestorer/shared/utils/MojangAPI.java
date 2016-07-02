package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

public class MojangAPI {

	private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";

	public static String getUUID(String name) throws SkinRequestException {
		String output = readURL(uuidurl + name);

		if (output.isEmpty())
			throw new SkinRequestException(Locale.NOT_PREMIUM);

		else if (output.contains("TooManyRequestsException")) {
			output = readURL(Config.ALT_SKIN_PROPERTY_URL + name);

			if (output.isEmpty())
				throw new SkinRequestException(Locale.ALT_API_FAILED);

			else if (output.contains("\"error\":\"Unknown Username\""))
				throw new SkinRequestException(Locale.NOT_PREMIUM);

			String idbeg = "\"uuid\":\"";
			String idend = "\"}";

			return getStringBetween(output, idbeg, idend);
		}

		return output.substring(7, 39);
	}

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property
	 * 
	 * @return com.mojang.authlib.properties.Property
	 *         <p>
	 *         net.md_5.bungee.connection.LoginResult.Property
	 * 
	 **/
	public static Object getSkinProperty(String uuid) throws SkinRequestException {
		String output = readURL(skinurl + uuid + "?unsigned=false");

		String sigbeg = "[{\"signature\":\"";
		String mid = "\",\"name\":\"textures\",\"value\":\"";
		String valend = "\"}]";

		if (output == null || output.isEmpty() || output.contains("TooManyRequestsException")) {

			output = readURL(Config.ALT_SKIN_PROPERTY_URL + uuid).replace(" ", "");

			String uid = getStringBetween(output, "{\"uuid\":\"", "\",\"uuid_formatted");

			if (uid.toLowerCase().contains("null"))
				throw new SkinRequestException(Locale.ALT_API_FAILED);

			sigbeg = "\"signature\":\"";
			mid = "\",\"name\":\"textures\",\"value\":\"";
			valend = "\"}],\"properties_decoded";

		}

		String value = getStringBetween(output, mid, valend).replace("\\/", "/");
		String signature = getStringBetween(output, sigbeg, mid).replace("\\/", "/");

		return SkinStorage.createProperty("textures", value, signature);
	}

	private static String readURL(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "SkinsRestorer");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setUseCaches(false);
			con.setDefaultUseCaches(false);

			String line;
			StringBuilder output = new StringBuilder();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			while ((line = in.readLine()) != null)
				output.append(line);

			in.close();

			return output.toString();
		} catch (Exception e) {
			return "";
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

	public static class SkinRequestException extends Exception {

		private static final long serialVersionUID = 5969055162529998032L;
		private String reason;

		public SkinRequestException(String reason) {
			this.reason = reason;
		}

		public String getReason() {
			return reason;
		}

	}

}
