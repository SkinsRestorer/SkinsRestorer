package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

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
		else if (output.contains("\"error\""))
			throw new SkinRequestException(Locale.RATE_LIMITED);

		return output.substring(7, 39);
	}

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property (old or new)
	 * 
	 * @return Property object (New Mojang, Old Mojang or Bungee)
	 * 
	 **/
	public static Object getSkinProperty(String skin, String uuid) throws SkinRequestException {
		String output = readURL(skinurl + uuid + "?unsigned=false");

		String sigbeg = "\"signature\":\"";
		String mid = "\",\"name\":\"textures\",\"value\":\"";
		String valend = "\"}]";

		String signature = "", value = "";

		// Remember kids, output will never, ever be null
		if (output.isEmpty() || output.contains("\"error\"")) {

			output = readURL(Config.ALT_SKIN_PROPERTY_URL + skin).replace(" ", "");

			String uid = getStringBetween(output, "{\"uuid\":\"", "\",\"id\":\"");

			if (output.isEmpty() || uid.toLowerCase().contains("null") || output.contains("\"error\""))
				throw new SkinRequestException(Locale.ALT_API_FAILED);

			sigbeg = "\",\"signature\":\"";
			mid = "\"value\":\"";
			valend = "\"},\"properties_";

			value = getStringBetween(output, mid, sigbeg);
			signature = getStringBetween(output, sigbeg, valend);

			// Temporar fix for that mcapi BS
			if (Base64Coder.decodeString(value).contains("\\/"))
				throw new SkinRequestException(Locale.ALT_API_FAILED);
		} else {
			value = getStringBetween(output, mid, valend);
			signature = getStringBetween(output, sigbeg, mid);
		}

		return SkinStorage.createProperty("textures", value, signature);
	}

	private static String readURL(String url) {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "SkinsRestorer");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setDoOutput(true);

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
