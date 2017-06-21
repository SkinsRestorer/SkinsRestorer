package skinsrestorer.bukkit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import skinsrestorer.bukkit.commands.GUICommand;
import skinsrestorer.bukkit.commands.SkinCommand;
import skinsrestorer.bukkit.commands.SrCommand;
import skinsrestorer.bukkit.menu.SkinsGUI;
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
	ConsoleCommandSender con = Bukkit.getConsoleSender();
	public static SkinsRestorer getInstance() {
		return instance;
	}

	private SkinFactory factory;
	private MySQL mysql;
	private boolean bungeeEnabled;

	private boolean outdated;

	public void log(String msg){
		con.sendMessage(C.c("&e[&2SkinsRestorer&e] &r"+msg));
	}
	
	public String checkVersion() {
		try {
			HttpURLConnection con = (HttpURLConnection) new URL("http://www.spigotmc.org/api/general.php")
					.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.getOutputStream()
					.write("key=98BE0FE67F88AB82B4C197FAF1DC3B69206EFDCC4D3B80FC83A00037510B99B4&resource=2124"
							.getBytes("UTF-8"));
			String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
			if (version.length() <= 13)
				return version;
		} catch (Exception ex) {
			log("&cFailed to check for an update on spigot.");
		}
		return getVersion();
	}

	public SkinFactory getFactory() {
		return factory;
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public String getVersion() {
		return getDescription().getVersion();
	}

	public boolean isOutdated() {
		return outdated;
	}

	@Override
	public void onEnable() {
		instance = this;

		try {
			// Doesn't support Cauldron and stuff..
			Class.forName("net.minecraftforge.cauldron.CauldronHooks");
			log("&aSkinsRestorer doesn't support Cauldron, Thermos or KCauldron, Sorry :(");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		} catch (Exception e) {
			try {
				// Checking for old versions
				factory = (SkinFactory) Class
						.forName("skinsrestorer.bukkit.skinfactory.SkinFactory_" + ReflectionUtil.serverVersion)
						.newInstance();
			} catch (Exception ex) {
				// 1.8+++
				factory = new UniversalSkinFactory();
			}
		}
		log("&aDetected Minecraft &e" + ReflectionUtil.serverVersion + "&a, using &e"
				+ factory.getClass().getSimpleName());

		
		// Multiverse Core support.
				MCoreAPI.init();
				if (MCoreAPI.check())
					log("&aDetected &eMultiverse-Core &aUsing it for dimensions.");
		
		
		
		// Bungeecord stuff
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
										factory.applySkin(player,
												SkinStorage.createProperty(in.readUTF(), in.readUTF(), in.readUTF()));
									} catch (Exception e) {
									}
									factory.updateSkin(player);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			});

			// Updater stuff
			if (Config.UPDATER_ENABLED)
				if (checkVersion().equals(getVersion())) {
					outdated = false;
					log("&a----------------------------------------------");
					log(ChatColor.GREEN + "    +===============+");
					log(ChatColor.GREEN + "    | SkinsRestorer |");
					log(ChatColor.GREEN + "    |---------------|");
					log(ChatColor.GREEN + "    |  Bungee Mode  |");
					log(ChatColor.GREEN + "    +===============+");
					log("&a----------------------------------------------");
					log(ChatColor.AQUA + "    Current version: " + ChatColor.GREEN + getVersion());
					log(ChatColor.GREEN + "    The latest version!");
					log("&a----------------------------------------------");
				} else {
					outdated = true;
					log("&a----------------------------------------------");
					log(ChatColor.GREEN + "    +===============+");
					log(ChatColor.GREEN + "    | SkinsRestorer |");
					log(ChatColor.GREEN + "    |---------------|");
					log(ChatColor.GREEN + "    |  Bungee Mode  |");
					log(ChatColor.GREEN + "    +===============+");
					log("&a----------------------------------------------");
					log(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
					log(ChatColor.RED + "    A new version is available! Download it at:");
					log(
							ChatColor.YELLOW + "    https://www.spigotmc.org/resources/skinsrestorer.2124/");
					log("&a----------------------------------------------");
				}
			return;
		}

		// Config stuff
		Config.load(getResource("config.yml"));
		Locale.load();
		MojangAPI.get().loadProxies();

		if (Config.USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(Config.MYSQL_HOST, Config.MYSQL_PORT, Config.MYSQL_DATABASE,
					Config.MYSQL_USERNAME, Config.MYSQL_PASSWORD));
		else
			SkinStorage.init(getDataFolder());

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new CooldownStorage(), 0, 1 * 20);

		// Commands
		getCommand("skinsrestorer").setExecutor(new SrCommand());
		getCommand("skin").setExecutor(new SkinCommand());
		getCommand("skins").setExecutor(new GUICommand());
		getCommand("skinver").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] arg3) {
				sender.sendMessage(C.c("&8This server is kindly running &aSkinsRestorer &e"
						+ SkinsRestorer.getInstance().getVersion() + "&8, made with love by &c"
						+ SkinsRestorer.getInstance().getDescription().getAuthors().get(0)
						+ "&8, utilizing Minecraft &a" + ReflectionUtil.serverVersion + "&8."));
				return false;
			}

		});
        Bukkit.getPluginManager().registerEvents(new SkinsGUI(), this);
		Bukkit.getPluginManager().registerEvents(new Listener() {

			// LoginEvent happens on attemptLogin so its the best place to set
			// the skin
			@EventHandler
			public void onLogin(PlayerJoinEvent e) {
				try {
					if (Config.DISABLE_ONJOIN_SKINS) {
						factory.applySkin(e.getPlayer(),
								SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.getPlayer().getName())));
						return;
					}
					if (Config.DEFAULT_SKINS_ENABLED)
						if (SkinStorage.getPlayerSkin(e.getPlayer().getName()) == null) {
							List<String> skins = Config.DEFAULT_SKINS;
							int randomNum = 0 + (int) (Math.random() * skins.size());
							factory.applySkin(e.getPlayer(),
									SkinStorage.getOrCreateSkinForPlayer(skins.get(randomNum)));
							return;
						}
					factory.applySkin(e.getPlayer(), SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName()));
				} catch (Exception ex) {
				}
			}
		}, this);

		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

			@Override
			public void run() {

				if (Config.UPDATER_ENABLED)
					if (checkVersion().equals(getVersion())) {
						outdated = false;
						log("&a----------------------------------------------");
						log(ChatColor.GREEN + "    +===============+");
						log(ChatColor.GREEN + "    | SkinsRestorer |");
						log(ChatColor.GREEN + "    +===============+");
						log("&a----------------------------------------------");
						log(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
						log(ChatColor.GREEN + "    The latest version!");
						log("&a----------------------------------------------");
					} else {
						outdated = true;
						log("");
						log(ChatColor.GREEN + "    +===============+");
						log(ChatColor.GREEN + "    | SkinsRestorer |");
						log(ChatColor.GREEN + "    +===============+");
						log("&a----------------------------------------------");
						log(ChatColor.AQUA + "    Current version: " + ChatColor.RED + getVersion());
						log(ChatColor.RED + "    A new version is available! Download it at:");
						log(
								ChatColor.YELLOW + "    https://www.spigotmc.org/resources/skinsrestorer.2124/");
						log("&a----------------------------------------------");
					}

				if (Config.DEFAULT_SKINS_ENABLED)
					for (String skin : Config.DEFAULT_SKINS)
						try {
							SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
						} catch (SkinRequestException e) {
							if (SkinStorage.getSkinData(skin) == null)
								log(
										ChatColor.RED + "Default Skin '" + skin + "' request error: " + e.getReason());
						}
			}

		});

	}
}
