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
		String output = readURL(Config.ALT_UUID_URL + name);

		String idbeg = "\"uuid\":\"";
		String idend = "\"}";

		if (output.isEmpty() || output.contains("\"error\":\"Unknown Username\"")) {
			output = readURL(uuidurl + name);

			if (output.isEmpty() || output.contains("TooManyRequestsException")) {
				output = readURL(Config.ALT_UUID_URL2 + name).replace(" ", "");

				idbeg = "\"uuid\":\"";
				idend = "\",\"id\":";

				String response = getStringBetween(output, idbeg, idend);

				if (response.startsWith("[{\"uuid\":null"))
					throw new SkinRequestException(Locale.NOT_PREMIUM);

				return response;
			}
			return output.substring(7, 39);
		}
		return getStringBetween(output, idbeg, idend);
	}

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property
	 * 
	 * @return Property object (Mojang or Bungee)
	 * 
	 **/
	public static Object getSkinProperty(String uuid) throws SkinRequestException {
		String output = readURL(skinurl + uuid + "?unsigned=false");

		String sigbeg = "[{\"signature\":\"";
		String mid = "\",\"name\":\"textures\",\"value\":\"";
		String valend = "\"}]";

		String value;

		if (output == null || output.isEmpty() || output.contains("TooManyRequestsException")) {

			output = readURL(Config.ALT_SKIN_PROPERTY_URL + uuid).replace(" ", "");

			String uid = getStringBetween(output, "{\"uuid\":\"", "\",\"uuid_formatted");

			if (uid.toLowerCase().contains("null"))
				throw new SkinRequestException(Locale.ALT_API_FAILED);

			sigbeg = "\"signature\":\"";
			mid = "\",\"name\":\"textures\",\"value\":\"";
			valend = "\"}],\"properties_decoded";

			value = getStringBetween(output, mid, valend).replace("\\/", "/");

			if (value.startsWith("{\"uuid\":\""))
				throw new SkinRequestException(Locale.ALT_API_FAILED);
		}

		value = getStringBetween(output, mid, valend).replace("\\/", "/");
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
		try {
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
		} catch (Exception e) {
			return base;
		}
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
