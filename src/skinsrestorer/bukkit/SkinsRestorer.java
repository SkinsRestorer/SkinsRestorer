package skinsrestorer.bukkit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import skinsrestorer.bukkit.commands.ClearCommand;
import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.bukkit.listeners.LogoutListener;
import skinsrestorer.bukkit.skinfactory.SkinFactory;
import skinsrestorer.bukkit.skinfactory.UniversalSkinFactory;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinsRestorer extends JavaPlugin {

	private static SkinsRestorer instance;
	private SkinFactory factory;
	private MySQL mysql;
	private boolean bungeeEnabled;

	@Override
	public void onEnable() {
		instance = this;
		final ConsoleCommandSender console = Bukkit.getConsoleSender();
		try {
		    Class.forName("net.minecraftforge.cauldron.CauldronHooks");
			console.sendMessage(C.c("&aSkinsRestorer doesn't support Cauldron, Thermos or KCauldron, Sorry :("));
		Bukkit.getPluginManager().disablePlugin(this);
		return;
		} catch(Exception e) {
		
		try {
			factory = (SkinFactory) Class
					.forName("skinsrestorer.bukkit.skinfactory.SkinFactory_" + ReflectionUtil.serverVersion)
					.newInstance();
		} catch (Exception ex) {
			factory = new UniversalSkinFactory();
		}
		}
		console.sendMessage(C.c("&aDetected Minecraft &e" + ReflectionUtil.serverVersion + "&a, using &e"
				+ factory.getClass().getSimpleName()));

		try {
			bungeeEnabled = YamlConfiguration.loadConfiguration(new File("spigot.yml"))
					.getBoolean("settings.bungeecord");
		} catch (Exception e) {
			bungeeEnabled = false;
		}

		if (bungeeEnabled) {

			Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
			Bukkit.getMessenger().registerIncomingPluginChannel(this, "SkinsRestorer", new PluginMessageListener() {
				@Override
				public void onPluginMessageReceived(String channel, final Player player, final byte[] message) {
					if (!channel.equals("SkinsRestorer"))
						return;

					Bukkit.getScheduler().runTaskAsynchronously(getInstance(), new Runnable() {

						@Override
						public void run() {

							DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));

							try {
								String subchannel = in.readUTF();

								if (subchannel.equalsIgnoreCase("SkinUpdate")) {
									try {
										Object textures = SkinStorage.createProperty(in.readUTF(), in.readUTF(),
												in.readUTF());

										factory.applySkin(player, textures);
									} catch (Exception e) {
									}
									factory.updateSkin(player);
								}else if (subchannel.equalsIgnoreCase("SkinRemove")) {
									factory.removeSkin(player);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			});

			Bukkit.getPluginManager().registerEvents(new Listener() {

				@EventHandler(priority = EventPriority.LOW)
				public void onJoin(PlayerJoinEvent e) {
					Player p = e.getPlayer();
					if (p.hasPermission("skinsrestorer.cmds") || p.isOp())
						sendBungeePermission(p, "skinsrestorer.cmds");
					if (p.hasPermission("skinsrestorer.playercmds") || p.isOp())
						sendBungeePermission(p, "skinsrestorer.playercmds");
				}

			}, this);
        if (Config.UPDATER_ENABLED){
			if (checkStableVersion().equals(getVersion())) {
				console.sendMessage("");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
				console.sendMessage(ChatColor.GREEN + "    |---------------|");
				console.sendMessage(ChatColor.GREEN + "    |  Bungee Mode  |");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage("");
				console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
				console.sendMessage(ChatColor.GREEN + "    The latest version!");
				console.sendMessage("");
			} else if (checkDevVersion().equals(getVersion())) {
				console.sendMessage("");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
				console.sendMessage(ChatColor.GREEN + "    |---------------|");
				console.sendMessage(ChatColor.GREEN + "    |  Bungee Mode  |");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage("");
				console.sendMessage(ChatColor.GOLD + "    DEVELOPER BUILD");
				console.sendMessage("");
				console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
				console.sendMessage(ChatColor.GOLD + "    The latest developer build!");
				console.sendMessage("");
			} else {
				console.sendMessage("");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
				console.sendMessage(ChatColor.GREEN + "    |---------------|");
				console.sendMessage(ChatColor.GREEN + "    |  Bungee Mode  |");
				console.sendMessage(ChatColor.GREEN + "    +===============+");
				console.sendMessage("");
				console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
				console.sendMessage(ChatColor.RED + "    A new version is available! Download it at:");
				console.sendMessage(ChatColor.YELLOW + "    https://www.spigotmc.org/resources/skinsrestorer.2124/");
				console.sendMessage("");
			}
        }
			return;
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
		getCommand("clearskin").setExecutor(new ClearCommand());

		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
		Bukkit.getPluginManager().registerEvents(new LogoutListener(), this);
		
     if (Config.UPDATER_ENABLED){
		if (checkStableVersion().equals(getVersion())) {
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    STABLE BUILD");
			console.sendMessage("");
			console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
			console.sendMessage(ChatColor.GREEN + "    The latest version!");
			console.sendMessage("");
		} else if (checkDevVersion().equals(getVersion())) {
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage("");
			console.sendMessage(ChatColor.GOLD + "    DEVELOPER BUILD");
			console.sendMessage("");
			console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
			console.sendMessage(ChatColor.GOLD + "    The latest developer build!");
			console.sendMessage("");
		} else {
			console.sendMessage("");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage(ChatColor.GREEN + "    | SkinsRestorer |");
			console.sendMessage(ChatColor.GREEN + "    +===============+");
			console.sendMessage("");
			console.sendMessage(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
			console.sendMessage(ChatColor.RED + "    A new version is available!");
			console.sendMessage("");
		}
     }

		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

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

	public void sendBungeePermission(Player p, String perm) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);

			out.writeUTF("SkinPermissions");
			out.writeUTF(perm);

			p.sendPluginMessage(getInstance(), "BungeeCord", b.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String checkStableVersion() {
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

	public String checkDevVersion() {
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

	public SkinFactory getFactory() {
		return factory;
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

			Path target = new File("plugins" + File.separator + "SkinsRestorer.jar").toPath();

			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
