package skinsrestorer.bungee;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.MessageListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MySQL;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;
	private MySQL mysql;

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		CommandSender console = getProxy().getConsole();
		instance = this;
		Config.load(getResourceAsStream("config.yml"));
		Locale.load();

		if (Config.USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
					Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
		else
			SkinStorage.init();

		getProxy().getScheduler().schedule(this, CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);

		this.getProxy().getPluginManager().registerListener(this, new LoginListener());
		this.getProxy().getPluginManager().registerListener(this, new MessageListener());
		this.getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		this.getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		this.getProxy().registerChannel("SkinUpdate");

		if (!checkVersion().equals(getVersion())) {
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage("");
			console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
			console.sendMessage(ChatColor.RED + "    A new version is available!");
			console.sendMessage("");
		} else {
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage("");
			console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
			console.sendMessage(ChatColor.GREEN + "    The latest version!");
			console.sendMessage("");
		}

	}

	public static SkinsRestorer getInstance() {
		return instance;
	}

	public String checkVersion() {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
					.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream()
					.write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=2124")
							.getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			if (version.length() <= 7) {
				return version;
			}
		} catch (Exception ex) {
			System.out.println("Failed to check for an update on spigot.");
		}
		return getVersion();
	}

	public String getVersion() {
		return getDescription().getVersion();
	}

	public MySQL getMySQL() {
		return mysql;
	}
}
