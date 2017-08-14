package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

public class MojangAPI {

	public static List<String> proxies = new ArrayList<String>();

	private static MojangAPI mojangapi = new MojangAPI();
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
	public static Object getSkinProperty(String uuid) throws SkinRequestException {
		String output;
		try {
			output = readURL(skinurl + uuid + "?unsigned=false");

			String sigbeg = "\"signature\":\"";
			String mid = "\",\"name\":\"textures\",\"value\":\"";
			String valend = "\"}]";

			String signature = "", value = "";

				value = getStringBetween(output, mid, valend);
				signature = getStringBetween(output, sigbeg, mid);

			return SkinStorage.createProperty("textures", value, signature);
		} catch (Exception e) {
			return getSkinPropertyProxy(uuid);
		}
	}
	public static Object getSkinPropertyProxy(String uuid) throws SkinRequestException {
		String output;
		try {
			output = readURLProxy(skinurl + uuid + "?unsigned=false");

			String sigbeg = "\"signature\":\"";
			String mid = "\",\"name\":\"textures\",\"value\":\"";
			String valend = "\"}]";

			String signature = "", value = "";

				value = getStringBetween(output, mid, valend);
				signature = getStringBetween(output, sigbeg, mid);

			return SkinStorage.createProperty("textures", value, signature);
		} catch (Exception e) {
           return getSkinPropertyProxy(uuid);
		}
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
				return getUUIDProxy(name);

			return output.substring(7, 39);
		} catch (IOException e) {
			return getUUIDProxy(name);
		}
	}
	public static String getUUIDProxy(String name) throws SkinRequestException {
		String output;
		try {
			output = readURLProxy(uuidurl + name);

			if (output.isEmpty())
				throw new SkinRequestException(Locale.NOT_PREMIUM);
			else if (output.contains("\"error\""))
				throw new SkinRequestException(Locale.ALT_API_FAILED);

			return output.substring(7, 39);
		} catch (IOException e) {
			throw new SkinRequestException(e.getMessage());
		}
	}
	public static MojangAPI get(){
		return mojangapi;
	}
    public void loadProxies(){
    	String line;
    	try (
    	    InputStream fis = getClass().getResourceAsStream("/proxy.txt"); 
    	    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
    	    BufferedReader br = new BufferedReader(isr);
    	) {
    	    while ((line = br.readLine()) != null) {
    	         proxies.add(line);
    	    }
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    public static int rand(int High){
    	Random r = new Random();
    	return r.nextInt(High-1) + 1;
    }
	private static String readURL(String url) throws MalformedURLException, IOException, SkinRequestException {

		HttpsURLConnection con = (HttpsURLConnection) new URL(url).openConnection();
		
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
	private static String readURLProxy(String url) throws MalformedURLException, IOException, SkinRequestException {
		HttpsURLConnection con = null;
		String ip = null;
		int port = 0;
		String proxyStr = null;
			proxyStr = proxies.get(rand(proxies.size()-1));
			String[] realProxy = proxyStr.split(":");
			ip=realProxy[0];port=Integer.valueOf(realProxy[1]);
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
			con = (HttpsURLConnection) new URL(url).openConnection(proxy);

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
