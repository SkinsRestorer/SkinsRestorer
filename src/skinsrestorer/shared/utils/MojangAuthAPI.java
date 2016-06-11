package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class MojangAuthAPI {

	private final static String authserver = "https://authserver.mojang.com";
	private final static String api = "https://api.mojang.com";

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
	public static String uploadSkin(String id, String authtoken, File image, boolean slim) throws Exception {

		String boundary = "===" + System.currentTimeMillis() + "===";
		final String line = "\r\n";

		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());

		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		con.setRequestProperty("Authorization", "Bearer " + authtoken);
		con.setDoOutput(true);

		OutputStream outs = con.getOutputStream();
		PrintWriter out = new PrintWriter(outs);

		// Writing model type (empty string = Steve, "slim" = Alex)
		out.append("--" + boundary);
		out.append(line);
		out.append("Content-Disposition: form-data; name=\"model\"");
		out.append(line);
		out.append(line);
		if (slim)
			out.append("slim");
		out.append(line);

		// Writing the skin image file
		out.append("--" + boundary);
		out.append(line);
		out.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + image.getName() + "\"");
		out.append(line);
		out.append("Content-Type: image/png");
		out.append(line);
		out.append(line);

		// Writing the file itself
		FileInputStream fin = new FileInputStream(image);
		byte[] buf = new byte[4096];
		int readBytes = -1;
		while ((readBytes = fin.read(buf)) != -1)
			outs.write(buf, 0, readBytes);

		fin.close();

		out.append(line);
		out.append("--" + boundary + "--");
		out.append(line);
		out.close();
		outs.close();

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
		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user").openConnection());

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
