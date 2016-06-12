package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class MojangAuthAPI {

	private final static String authserver = "https://authserver.mojang.com";
	private final static String api = "https://api.mojang.com";

	// Werks
	public static AuthSession authenticate(String username, String password) throws Exception {

		String genClientToken = UUID.randomUUID().toString();

		// Setting up json POST request
		String payload = "{\"agent\": {\"name\": \"Minecraft\",\"version\": 1},\"username\": \"" + username
				+ "\",\"password\": \"" + password + "\",\"clientToken\": \"" + genClientToken + "\"}";

		String output = postReadURL(payload, new URL(authserver + "/authenticate"));

		// Setting up patterns
		String authBeg = "{\"accessToken\":\"";
		String authEnd = "\",\"clientToken\":\"";
		String clientEnd = "\",\"selectedProfile\"";

		// What we are looking for
		String authtoken = getStringBetween(output, authBeg, authEnd);
		// Getting the client token just for the sake of comparing with our
		// generated one
		String clienttoken = getStringBetween(output, authEnd, clientEnd);

		if (!clienttoken.equalsIgnoreCase(genClientToken))
			throw new Exception("Client tokens dont match! Wtf \n" + clienttoken + "\n" + genClientToken);

		String idBeg = "\"selectedProfile\":{\"id\":\"";
		String nameBeg = "\",\"name\":\"";
		String nameEnd = "\"},\"availableProfiles\"";

		// There are more instances of the same pattern, better be sure to get
		// the right one
		String id = getStringBetween(output, idBeg, nameBeg);
		String name = getStringBetween(output, idBeg + id + nameBeg, nameEnd);

		return new AuthSession(name, id, authtoken, clienttoken);
	}

	// Still error 500, cmonBruh
	// THERE IS STILL WORK TO BE DONE STAY TUNED
	// Bc Mojang API for skins still does not accept the auth tokens
	// pls
	public static String uploadSkin(String id, String authtoken, File image, boolean slim) throws Exception {

		String boundary = "===" + System.currentTimeMillis() + "===";
		final String line = "\r\n";

		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());

		con.setRequestMethod("PUT");
		con.setRequestProperty("Authorization", "Bearer " + authtoken);
		con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);

		/*
		 * OutputStream outs = con.getOutputStream(); DataOutputStream out = new
		 * DataOutputStream(con.getOutputStream());
		 */

		StringBuilder builder = new StringBuilder();
		builder.append(con.getResponseCode()).append(" ").append(con.getResponseMessage()).append("\n");

		Map<String, List<String>> map = con.getHeaderFields();
		for (Map.Entry<String, List<String>> entry : map.entrySet()) {
			if (entry.getKey() == null)
				continue;
			builder.append(entry.getKey()).append(": ");

			List<String> headerValues = entry.getValue();
			Iterator<String> it = headerValues.iterator();
			if (it.hasNext()) {
				builder.append(it.next());

				while (it.hasNext()) {
					builder.append(", ").append(it.next());
				}
			}

			builder.append(line);
		}

		System.out.println(builder);

		return "";

	}

	public static String change(String id, String authtoken) throws Exception {

		HttpURLConnection con = (HttpURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());
		con.setRequestMethod("POST");
		con.setRequestProperty("Authorization", "Bearer " + authtoken);
		System.out.println(authtoken);
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);

		DataOutputStream out = new DataOutputStream(con.getOutputStream());

		out.write(
				"model=\"slim\"&url=\"http://lh3.googleusercontent.com/8DexHt1uhFcMgCy9jAMoj7hOJPLFj0vN0v6KBXVhz9hcxiPp9c4ZAI7Qa1o5uOuAIG4KG6DwjY6O-4RzQwJS"
						.getBytes("UTF-8"));

		out.flush();
		out.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

		String output = "";
		String rline = null;
		while ((rline = in.readLine()) != null)
			output += rline;

		in.close();

		return output;
	}

	public static String reset(String id, String authtoken) throws Exception {

		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());

		con.setRequestProperty("Authorization", "Bearer " + authtoken);
		System.out.println(authtoken);
		con.setRequestMethod("DELETE");
		con.setDoInput(true);
		con.setDoOutput(true);
		con.setUseCaches(false);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

		String output = "";
		String rline = null;
		while ((rline = in.readLine()) != null)
			output += rline;

		in.close();

		return output;
	}

	// Werks
	public static String info(String authtoken) throws Exception {
		HttpURLConnection con = (HttpURLConnection) (new URL(api + "/user").openConnection());

		con.setRequestProperty("Authorization", "Bearer " + authtoken);
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

		String output = "";
		String rline = null;
		while ((rline = in.readLine()) != null)
			output += rline;

		in.close();

		return output;
	}

	public static void invalidate(String authtoken, String clienttoken) throws Exception {
		String payload = "{\"accessToken\": \"" + authtoken + "\",\"clientToken\": \"" + clienttoken + "\"}";

		String output = postReadURL(payload, new URL(authserver + "/invalidate"));

		if (!output.isEmpty() || output != null)
			throw new Exception("Response is not empty ! : " + output);

	}

	public static void signout(String username, String password) throws Exception {
		String payload = "{\"username\": \"" + username + "\",\"password\": \"" + password + "\"}";

		String output = postReadURL(payload, new URL(authserver + "/signout"));

		if (!output.isEmpty() || output != null)
			throw new Exception("Response is not empty ! : " + output);

	}

	public static void validate(String authtoken, String clienttoken) throws Exception {
		String payload = "{\"accessToken\": \"" + authtoken + "\",\"clientToken\": \"" + clienttoken + "\"}";

		String output = postReadURL(payload, new URL(authserver + "/validate"));

		if (!output.isEmpty() || output != null)
			throw new Exception("Response is not empty ! : " + output);

	}

	public static void testrefresh(String name, String id, String authtoken, String clienttoken) throws Exception {
		String payload = "{\"accessToken\": \"" + authtoken + "\",\"clientToken\": \"" + clienttoken
				+ "\",\"selectedProfile\": {\"id\": \"" + id + "\",\"name\": \"" + name + "\"}}";

		String output = postReadURL(payload, new URL(authserver + "/refresh"));

		System.out.println(output);

	}

	private static String postReadURL(String payload, URL url) throws Exception {
		HttpsURLConnection con = (HttpsURLConnection) (url.openConnection());

		con.setReadTimeout(15000);
		con.setConnectTimeout(15000);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setDoInput(true);
		con.setDoOutput(true);

		OutputStream out = con.getOutputStream();
		out.write(payload.getBytes("UTF-8"));
		out.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

		String output = "";
		String line = null;
		while ((line = in.readLine()) != null)
			output += line;

		in.close();

		return output;
	}

	private static String getStringBetween(String base, String begin, String end) {

		Pattern patbeg = Pattern.compile(Pattern.quote(begin));
		Pattern patend = Pattern.compile(Pattern.quote(end));

		int resbeg = 0;
		int resend = base.length() - 1;

		Matcher matbeg = patbeg.matcher(base);

		if (matbeg.find())
			resbeg = matbeg.end();

		Matcher matend = patend.matcher(base);

		if (matend.find())
			resend = matend.start();

		return base.substring(resbeg, resend);
	}

}
