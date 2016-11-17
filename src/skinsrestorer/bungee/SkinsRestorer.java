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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.MessageListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;
	private MySQL mysql;
	private boolean multibungee;
	private ExecutorService exe;
	private boolean outdated;

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		final CommandSender console = getProxy().getConsole();
		instance = this;
		Config.load(getResourceAsStream("config.yml"));
		Locale.load();

		exe = Executors.newCachedThreadPool();

		if (Config.USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
					Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
		else
			SkinStorage.init(getDataFolder());

		//Needed that, cause bungeecord throws "Illegal access exception otherwise.
		this.getProxy().getPluginManager().registerListener(this, new LoginListener());
		
		
		this.getProxy().getPluginManager().registerListener(this, new MessageListener());
		this.getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		this.getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		this.getProxy().registerChannel("SkinsRestorer");

		multibungee = Config.MULTIBUNGEE_ENABLED
				|| ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

		exe.submit(new Runnable() {

			@Override
			public void run() {
				if (Config.UPDATER_ENABLED) {
					if (checkVersion().equals(getVersion())) {
						outdated = false;
						console.sendMessage("");
						console.sendMessage(ChatColor.GREEN + "    +===============+");
						console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
						console.sendMessage(ChatColor.GREEN + "    +===============+");
						console.sendMessage("");
						console.sendMessage(ChatColor.GREEN + "    STABLE BUILD");
						console.sendMessage("");
						console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
						console.sendMessage(ChatColor.GREEN + "    The latest version!");
						console.sendMessage("");
					} else {
						outdated = true;
						console.sendMessage("");
						console.sendMessage(ChatColor.GREEN + "    +===============+");
						console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
						console.sendMessage(ChatColor.GREEN + "    +===============+");
						console.sendMessage("");
						console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
						console.sendMessage(ChatColor.RED + "    A new version is available! Download it at:");
						console.sendMessage(
								ChatColor.YELLOW + "    https://www.spigotmc.org/resources/skinsrestorer.2124");
						console.sendMessage("");
					}
				}

				if (Config.DEFAULT_SKINS_ENABLED)
					for (String skin : Config.DEFAULT_SKINS) {
						try {
							SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(skin, MojangAPI.getUUID(skin)));
						} catch (SkinRequestException e) {
							if (SkinStorage.getSkinData(skin) == null)
								console.sendMessage(
										ChatColor.RED + "Default Skin '" + skin + "' request error: " + e.getReason());
						}
					}
			}

		});

	}

	@Override
	public void onDisable() {
		exe.shutdown();
	}

	public static SkinsRestorer getInstance() {
		return instance;
	}

	public boolean isOutdated() {
		return outdated;
	}

	public ExecutorService getExecutor() {
		return exe;
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

	public boolean isMultiBungee() {
		return multibungee;
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public boolean downloadUpdate() {
		try {
			InputStream in = new URL("http://api.spiget.org/v1/resources/2124/download").openStream();

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
