package skinsrestorer.bungee;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.MessageListener;
import skinsrestorer.bungee.listeners.PermissionListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;
	private MySQL mysql;

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		final CommandSender console = getProxy().getConsole();
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
		this.getProxy().getPluginManager().registerListener(this, new PermissionListener());
		this.getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		this.getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		this.getProxy().registerChannel("SkinsRestorer");

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

		getProxy().getScheduler().runAsync(this, new Runnable() {

			@Override
			public void run() {
				if (Config.DEFAULT_SKINS_ENABLED)
					for (String skin : Config.DEFAULT_SKINS) {
						try {
							SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
						} catch (SkinRequestException e) {
							if (SkinStorage.getSkinData(skin) == null)
								console.sendMessage(
										ChatColor.RED + "Default Skin '" + skin + "' request error: " + e.getReason());
						}
					}
			}

		});

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
					.write(("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=25777")
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

	public boolean downloadUpdate() {
		try {
			InputStream in = new URL("https://api.spiget.org/v1/resources/1884/download").openStream();

			System.out.println(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

			Path target = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).toPath();

			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
