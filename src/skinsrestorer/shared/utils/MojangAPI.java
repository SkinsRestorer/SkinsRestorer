package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

public class MojangAPI {

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

	private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";

	private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";

	/**
	 * Returned object needs to be casted to either BungeeCord's property or
	 * Mojang's property (old or new)
	 *
	 * @return Property object (New Mojang, Old Mojang or Bungee)
	 *
	 **/
	public static Object getSkinProperty(String skin, String uuid) throws SkinRequestException {
		String output;
		try {
			output = readURL(skinurl + uuid + "?unsigned=false");

			String sigbeg = "\"signature\":\"";
			String mid = "\",\"name\":\"textures\",\"value\":\"";
			String valend = "\"}]";

			String signature = "", value = "";

			// Remember kids, output will never, ever be null
			if (output.isEmpty() || output.contains("\"error\""))
				throw new SkinRequestException(Locale.ALT_API_FAILED);
			else {
				value = getStringBetween(output, mid, valend);
				signature = getStringBetween(output, sigbeg, mid);
			}

			return SkinStorage.createProperty("textures", value, signature);
		} catch (Exception e) {
			if (e.getMessage().contains("429"))
				throw new SkinRequestException(Locale.ALT_API_FAILED);
			/*
			 * try { output = readURL(Config.ALT_PROPERTY_URL + skin);
			 *
			 * System.out.println(output); String uid = getStringBetween(output,
			 * "{\"uuid\":\"", "\",\"id\":\"");
			 *
			 * if (output.isEmpty() || uid.toLowerCase().contains("null") ||
			 * output.contains("\"error\"")) throw new
			 * SkinRequestException(Locale.ALT_API_FAILED);
			 *
			 * String sigbeg = "[{\"signature\":"; String mid =
			 * "\",\"name\":\"textures\",\"value\":\""; String valend =
			 * "\"}],\"decoded\"";
			 *
			 * String value = getStringBetween(output, mid, sigbeg); String
			 * signature = getStringBetween(output, sigbeg, valend);
			 * System.out.println("Value: "+value); System.out.println(
			 * "Signature: "+signature); return
			 * SkinStorage.createProperty("textures", value, signature); } catch
			 * (IOException e1) { // TODO Auto-generated catch block
			 * e1.printStackTrace(); }
			 */
		}
		return null;
	}

	public static String getStringBetween(final String base, final String begin, final String end) {
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

	/**
	 *
	 * @param name
	 *            - Name of the player
	 * @return Dash-less UUID (String)
	 *
	 * @throws SkinRequestException
	 *             - If player is NOT_PREMIUM or server is RATE_LIMITED
	 */
	public static String getUUID(String name) throws SkinRequestException {
		String output;
		try {
			output = readURL(uuidurl + name);

			if (output.isEmpty())
				throw new SkinRequestException(Locale.NOT_PREMIUM);
			else if (output.contains("\"error\""))
				throw new SkinRequestException(Locale.WAIT_A_MINUTE);

			return output.substring(7, 39);
		} catch (IOException e) {
			throw new SkinRequestException(Locale.WAIT_A_MINUTE);
		}
	}

	private static String readURL(String url) throws SkinRequestException, MalformedURLException, IOException {

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
	}

}
