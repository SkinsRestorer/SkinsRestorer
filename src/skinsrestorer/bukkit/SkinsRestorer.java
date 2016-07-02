package skinsrestorer.bukkit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.bukkit.listeners.SkinsPacketHandler;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinsRestorer extends JavaPlugin {

	private static SkinsRestorer instance;
	private MySQL mysql;
	private boolean bungeeEnabled;
	private boolean v18plus;

	@Override
	public void onEnable() {
		instance = this;
		ConsoleCommandSender console = Bukkit.getConsoleSender();

		try {
			bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml"))
					.getBoolean("settings.bungeecord");
		} catch (Exception e) {
			bungeeEnabled = false;
		}

		if (bungeeEnabled) {

			Bukkit.getMessenger().registerIncomingPluginChannel(this, "SkinUpdate", new PluginMessageListener() {
				@Override
				public void onPluginMessageReceived(String channel, final Player player, byte[] message) {
					if (!channel.equals("SkinUpdate"))
						return;

					Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

						@Override
						public void run() {

							DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

							try {
								// Skipping the channel and playername
								in.readUTF();
								in.readUTF();

								Object textures = SkinStorage.createProperty(in.readUTF(), in.readUTF(), in.readUTF());

								Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
								Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
								Object profile = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
								applyToGameProfile(profile, textures);
							} catch (Exception e) {
							}
							SkinsPacketHandler.updateSkin(player);
						}
					});
				}
			});
			return;
		}

		try {
			v18plus = Class.forName("io.netty.channel.Channel") != null;
		} catch (ClassNotFoundException e) {
			v18plus = false;
		}

		Config.load(getResource("config.yml"));
		Locale.load();

		if (Config.USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
					Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
		else
			SkinStorage.init();

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, CooldownStorage.cleanupCooldowns, 0, 60 * 20);

		getCommand("skinsrestorer").setExecutor(new SrCommand());
		getCommand("skin").setExecutor(new SkinCommand());

		for (Player p : Bukkit.getOnlinePlayers())
			SkinsPacketHandler.inject(p);

		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);

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

	@Override
	public void onDisable() {
		if (!bungeeEnabled)
			for (Player p : Bukkit.getOnlinePlayers())
				SkinsPacketHandler.uninject(p);
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

	public void applyToGameProfile(Object profile, Object textures) {
		try {
			Object propmap = ReflectionUtil.invokeMethod(profile.getClass(), profile, "getProperties");
			Object delegate = ReflectionUtil.invokeMethod(propmap.getClass(), propmap, "delegate");
			Object propcollection = null;

			for (Method me : delegate.getClass().getMethods()) {
				if (me.getName().equalsIgnoreCase("get")) {
					me.setAccessible(true);
					propcollection = me.invoke(delegate, "textures");
					ReflectionUtil.invokeMethod(propcollection.getClass(), propcollection, "clear");
				}
			}

			for (Method me : delegate.getClass().getMethods()) {
				if (me.getName().equalsIgnoreCase("put")) {
					me.setAccessible(true);
					propcollection = me.invoke(delegate, "textures", textures);
				}
			}
			/*
			 * for (Method me : delegate.getClass().getMethods()) { if
			 * (me.getName().equalsIgnoreCase("get")) { me.setAccessible(true);
			 * propcollection = me.invoke(delegate, "textures"); for (Object o :
			 * (Collection<?>) propcollection) { String value = (String)
			 * ReflectionUtil.invokeMethod(o.getClass(), o, "getValue"); String
			 * signature = (String) ReflectionUtil.invokeMethod(o.getClass(), o,
			 * "getSignature");
			 * 
			 * System.out.println(value); System.out.println(signature); } } }
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean is18plus() {
		return v18plus;
	}

}
