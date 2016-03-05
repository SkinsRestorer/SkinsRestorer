package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class Updater {
	private String currVer;
	final int resource = 2124;
	private static String latestVersion = "";
	private static boolean updateAvailable = false;

	public Updater(String currVer) {
		this.currVer = currVer;
		;
	}

	private String getSpigotVersion() {
		try {
			HttpURLConnection URL = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
					.openConnection();
			URL.setDoOutput(true);
			URL.setRequestMethod("POST");
			URL.getOutputStream()
					.write("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=2124"
							.getBytes("UTF-8"));
			String str = new BufferedReader(new InputStreamReader(URL.getInputStream())).readLine();
			if (str.length() <= 7) {
				return str;
			}
		} catch (Exception exception) {
			System.out.println("----------------------------");
			System.out.println("  SkinsRestorer Updater");
			System.out.println(" ");
			System.out.println("Could not connect to spigotmc.org");
			System.out.println("to check for updates! ");
			System.out.println(" ");
			System.out.println("----------------------------");
		}
		return null;
	}

	private boolean checkHigher(String String1, String String2) {
		String str1 = toReadable(String1);
		String str2 = toReadable(String2);
		return str1.compareTo(str2) < 0;
	}

	public boolean checkUpdates() {
		if (getHighest() != "") {
			return true;
		}
		String str = getSpigotVersion();
		if ((str != null) && (checkHigher(currVer, str))) {
			latestVersion = str;
			updateAvailable = true;
			return true;
		}

		return false;
	}

	public static boolean updateAvailable() {
		return updateAvailable;
	}

	public static String getHighest() {
		return latestVersion;
	}

	private String toReadable(String string) {
		String[] arrayOfString1 = Pattern.compile(".", 16).split(string.replace("v", ""));
		string = "";
		for (String str : arrayOfString1)
			string = string + String.format("%4s", new Object[] { str });
		return string;
	}
}