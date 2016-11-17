package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

public class MojangAuthAPI {

	private final static String authserver = "https://authserver.mojang.com";
	private final static String api = "https://api.mojang.com";

	// Werks
	// All data stored fine
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
		String authtoken = MojangAPI.getStringBetween(output, authBeg, authEnd);
		// Getting the client token just for the sake of comparing with our
		// generated one
		String clienttoken = MojangAPI.getStringBetween(output, authEnd, clientEnd);

		if (!clienttoken.equalsIgnoreCase(genClientToken))
			throw new Exception("Client tokens dont match! Wtf \n" + clienttoken + "\n" + genClientToken);

		String idBeg = "\"selectedProfile\":{\"id\":\"";
		String nameBeg = "\",\"name\":\"";
		String nameEnd = "\"},\"availableProfiles\"";

		// There are more instances of the same pattern, better be sure to get
		// the right one
		String id = MojangAPI.getStringBetween(output, idBeg, nameBeg);
		String name = MojangAPI.getStringBetween(output, idBeg + id + nameBeg, nameEnd);

		System.out.println(name + " " + id + " " + authtoken + " " + clienttoken);

		return new AuthSession(name, id, authtoken, clienttoken);
	}

	// Forbidden 403
	// Tested this stuff, it just doesnt work, majong pls
	public static String uploadSkin(String id, String authtoken, String url, boolean slim) throws Exception {
		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());

		con.setRequestProperty("Content-Type", "aplication/json");
		con.setRequestProperty("User-Agent", "SkinsRestorer");
		con.setRequestMethod("POST");
		con.setDoOutput(true);

		con.addRequestProperty("Authorization", "Bearer " + authtoken);
		con.setRequestProperty("Authorization", "Bearer " + authtoken);

		OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());

		out.write("model=" + (slim ? URLEncoder.encode("slim", "UTF-8") : URLEncoder.encode("", "UTF-8")) + "&url="
				+ URLEncoder.encode("https://skins.minecraft.net/MinecraftSkins/GamerGirl268.png", "UTF-8"));

		out.flush();
		con.connect();

		return con.getResponseMessage() + "  " + con.getResponseCode();
	}

	// Error 402 or 500
	public static String reset(String id, String authtoken) throws Exception {

		HttpsURLConnection con = (HttpsURLConnection) (new URL(api + "/user/profile/" + id + "/skin").openConnection());

		con.setRequestProperty("Authorization", "Bearer " + authtoken);
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

	// These 4 methods should werk fine

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

	// Simple payload posting and reading the outpust
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

}
