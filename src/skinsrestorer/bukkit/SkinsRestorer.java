package skinsrestorer.bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import skinsrestorer.bukkit.commands.Commands;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.bukkit.metrics.Metrics;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.Factory;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.Updater;

public class SkinsRestorer extends JavaPlugin implements Listener {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	public static ScheduledExecutorService getExecutor() {
		return executor;
	}

	private boolean autoIn = false;
	private static SkinsRestorer instance;

	public static SkinsRestorer getInstance() {
		return instance;
	}

	private Logger log;
	private ConsoleCommandSender coloredLog = null;
	private skinsrestorer.shared.utils.Updater updater;

	private String version;
	private Factory factory;
	private MySQL mysql;
	private File debug;

	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		coloredLog = Bukkit.getConsoleSender();
		getDataFolder().mkdirs();
		ConfigStorage.getInstance().init(this.getResource("config.yml"), false);
		LocaleStorage.getInstance().init(this.getResource("messages.yml"), false);
		if (ConfigStorage.getInstance().USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(ConfigStorage.getInstance().MYSQL_HOST,
					ConfigStorage.getInstance().MYSQL_PORT, ConfigStorage.getInstance().MYSQL_DATABASE,
					ConfigStorage.getInstance().MYSQL_USERNAME, ConfigStorage.getInstance().MYSQL_PASSWORD));
		else {
			SkinStorage.init();
			executor.scheduleWithFixedDelay(CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);
		}

		getCommand("skinsrestorer").setExecutor(new Commands());
		getCommand("skin").setExecutor(new Commands());

		factory = new Factory();
		if (getServer().getPluginManager().isPluginEnabled("AutoIn")) {
			coloredLog.sendMessage(ChatColor.GREEN + "SkinsRestorer has detected that you are using AutoIn.");
			coloredLog.sendMessage(ChatColor.GREEN + "Check the USE_AUTOIN_SKINS option in your config!");
			autoIn = true;
		}

		if (ConfigStorage.getInstance().UPDATE_CHECK == true) {
			updater = new Updater(getDescription().getVersion());
			updater.checkUpdates();

		} else {
			coloredLog.sendMessage(ChatColor.RED + "SkinsRestorer Updater is Disabled!");
			updater = null;
		}
		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
		String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
		this.version = version.replace(".", "");

		// Registering the factory using reflection now. I'm tired of version
		// checks xD
		try {
			Class<?> factory = Class.forName("skinsrestorer.bukkit.UniversalSkinFactory");
			this.factory = (Factory) factory.newInstance();
			coloredLog.sendMessage("[SkinsRestorer] Loaded Skin Factory for " + getBukkitVersion());
		} catch (ClassNotFoundException e) {
			coloredLog.sendMessage(ChatColor.RED + "[SkinsRestorer] The version " + getBukkitVersion()
					+ " is not supported by SkinsModule.");
			Bukkit.getPluginManager().disablePlugin(this);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		if (updater != null) {
			if (Updater.updateAvailable()) {
				coloredLog.sendMessage(ChatColor.DARK_GREEN + "==============================================");
				coloredLog.sendMessage(ChatColor.YELLOW + "  SkinsRestorer Updater  ");
				coloredLog.sendMessage(ChatColor.YELLOW + " ");
				coloredLog.sendMessage(ChatColor.GREEN + "    An update for SkinsRestorer has been found!");
				coloredLog.sendMessage(
						ChatColor.AQUA + "    SkinsRestorer " + ChatColor.GREEN + "v" + Updater.getHighest());
				coloredLog.sendMessage(
						ChatColor.AQUA + "    You are running " + ChatColor.RED + "v" + getDescription().getVersion());
				coloredLog.sendMessage(ChatColor.YELLOW + " ");
				coloredLog.sendMessage(ChatColor.YELLOW + "    Download at" + ChatColor.GREEN
						+ " https://www.spigotmc.org/resources/skinsrestorer.2124/");
				coloredLog.sendMessage(ChatColor.DARK_GREEN + "==============================================");
			} else {
				coloredLog.sendMessage(ChatColor.DARK_GREEN + "==============================================");
				coloredLog.sendMessage(ChatColor.YELLOW + "  SkinsRestorer Updater");
				coloredLog.sendMessage(ChatColor.YELLOW + " ");
				coloredLog.sendMessage(ChatColor.AQUA + "    You are running " + "v" + ChatColor.GREEN
						+ getDescription().getVersion());
				coloredLog.sendMessage(ChatColor.GREEN + "    The latest version of SkinsRestorer!");
				coloredLog.sendMessage(ChatColor.YELLOW + " ");
				coloredLog.sendMessage(ChatColor.DARK_GREEN + "==============================================");
			}
		}

		if (ConfigStorage.getInstance().DEBUG_ENABLED) {

			debug = new File(getDataFolder(), "debug.txt");

			PrintWriter out = null;

			try {
				if (!debug.exists())
					debug.createNewFile();
				out = new PrintWriter(new FileOutputStream(debug), true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {

				out.println("Java version: " + System.getProperty("java.version"));
				out.println("Bukkit version: " + Bukkit.getBukkitVersion());
				out.println("SkinsRestoerer version: " + getDescription().getVersion());
				out.println();

				String plugins = "";
				for (Plugin plugin : Bukkit.getPluginManager().getPlugins())
					plugins += plugin.getName() + " (" + plugin.getDescription().getVersion() + "), ";

				out.println("Plugin list: " + plugins);
				out.println();
				out.println("Property output from MojangAPI (Notch) : ");

				SkinProfile sp = MojangAPI.getSkinProfile(MojangAPI.getProfile("Notch").getId(), "Notch");

				out.println("Name: " + sp.getSkinProperty().getName());
				out.println("Value: " + sp.getSkinProperty().getValue());
				out.println("Signature: " + sp.getSkinProperty().getSignature());
				out.println();

				out.println("Raw data from MojangAPI (Blackfire62): ");
				Method m = MojangAPI.class.getDeclaredMethod("readURL", URL.class);

				m.setAccessible(true);
				String output = (String) m.invoke(null,
						new URL("https://sessionserver.mojang.com/session/minecraft/profile/"
								+ MojangAPI.getProfile("Blackfire62").getId() + "?unsigned=false"));

				out.println(output);

				out.println("\n\n\n\n\n\n\n\n\n\n");

			} catch (Exception e) {
				out.println("=========================================");
				e.printStackTrace(out);
				out.println("=========================================");
			}

			coloredLog.sendMessage(
					ChatColor.RED + "[SkinsRestorer] Debug file crated! Automatically setting debug mode to false... ");
			ConfigStorage.getInstance().config.set("Debug Enabled", false);
			ConfigStorage.getInstance().config.save();
			coloredLog.sendMessage(ChatColor.RED
					+ "[SkinsRestorer] Please check the contents of the file and send the contents to developers, if you are experiencing problems!");
			coloredLog.sendMessage(ChatColor.RED + "[SkinsRestorer] URL for error reporting: " + ChatColor.YELLOW
					+ "https://github.com/Th3Tr0LLeR/SkinsRestorer---Maro/issues");

		}

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			coloredLog.sendMessage(ChatColor.RED + "Failed to start Metrics.");
		}
	}

	@Override
	public void onDisable() {
		executor.shutdown();
		instance = null;
	}

	@Deprecated
	public void setSkin(final String playerName, final String skinName) throws SkinFetchFailedException {
		SkinsRestorerAPI.setSkin(playerName, skinName);
	}

	@Deprecated
	public boolean hasSkin(String playerName) {
		return SkinsRestorerAPI.hasSkin(playerName);
	}

	@Deprecated
	public void applySkin(Player player) {
		factory.applySkin(player);
	}

	@Deprecated
	public void removeSkin(Player player) {
		factory.removeSkin(player);
	}

	public com.gmail.bartlomiejkmazur.autoin.api.AutoInAPI getAutoInAPI() {
		return com.gmail.bartlomiejkmazur.autoin.api.APICore.getAPI();
	}

	public boolean isAutoInEnabled() {
		return autoIn;
	}

	public Factory getFactory() {
		return factory;
	}

	public String getVersion() {
		return this.getDescription().getVersion();
	}

	public String getBukkitVersion() {
		return version;
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public ConsoleCommandSender getColoredLog() {
		return coloredLog;
	}
}
