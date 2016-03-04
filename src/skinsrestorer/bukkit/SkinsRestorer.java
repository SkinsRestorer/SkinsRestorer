package skinsrestorer.bukkit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import skinsrestorer.bukkit.commands.AdminCommands;
import skinsrestorer.bukkit.commands.PlayerCommands;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.Updater;

public class SkinsRestorer extends JavaPlugin implements Listener {

	public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private boolean autoIn = false;
	static SkinsRestorer instance;

	public static SkinsRestorer getInstance() {
		return instance;
	}

	static Logger log;
	public ConsoleCommandSender log1 = null;
	private skinsrestorer.shared.utils.Updater updater;

	private String version;
	private Factory factory;

	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		log1 = Bukkit.getConsoleSender();
		getDataFolder().mkdirs();
		ConfigStorage.init(getDataFolder());
		LocaleStorage.init(getDataFolder());
		if (ConfigStorage.getInstance().USE_MYSQL)
			SkinStorage.init(new MySQL(ConfigStorage.getInstance().MYSQL_HOST, ConfigStorage.getInstance().MYSQL_PORT,
					ConfigStorage.getInstance().MYSQL_DATABASE, ConfigStorage.getInstance().MYSQL_USERNAME,
					ConfigStorage.getInstance().MYSQL_PASSWORD));
		else {
			SkinStorage.init(getDataFolder());
			executor.scheduleWithFixedDelay(CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);
		}

		getCommand("skinsrestorer").setExecutor(new AdminCommands());
		getCommand("skin").setExecutor(new PlayerCommands());

		factory = new Factory();
		if (getServer().getPluginManager().isPluginEnabled("AutoIn")) {
			log1.sendMessage(ChatColor.GREEN + "SkinsRestorer has detected that you are using AutoIn.");
			log1.sendMessage(ChatColor.GREEN + "Check the USE_AUTOIN_SKINS option in your config!");
			autoIn = true;
		}

		if (ConfigStorage.getInstance().UPDATE_CHECK == true) {
			updater = new Updater(getDescription().getVersion());
			updater.checkUpdates();
		} else {
			log1.sendMessage(ChatColor.RED + "SkinsRestorer Updater is Disabled!");
			updater = null;
		}
		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
		String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
		this.version = version.replace(".", "");

		// Registering the factory using reflection now. I'm tired of version
		// checks xD
		try {
			Class<?> factory = Class.forName("skinsrestorer.bukkit.SkinFactory" + getVersion());
			this.factory = (Factory) factory.newInstance();
			log1.sendMessage("[SkinsRestorer] Loaded Skin Factory for " + getVersion());
		} catch (ClassNotFoundException e) {
			log1.sendMessage(ChatColor.RED + "[SkinsRestorer] The version " + getVersion()
					+ " is not supported by SkinsModule.");
			Bukkit.getPluginManager().disablePlugin(this);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		if (updater != null) {
			if (Updater.updateAvailable()) {
				log1.sendMessage(ChatColor.DARK_GREEN + "==============================================");
				log1.sendMessage(ChatColor.YELLOW + "  SkinsRestorer Updater  ");
				log1.sendMessage(ChatColor.YELLOW + " ");
				log1.sendMessage(ChatColor.GREEN + "    An update for SkinsRestorer has been found!");
				log1.sendMessage(ChatColor.AQUA + "    SkinsRestorer " + ChatColor.GREEN + "v" + Updater.getHighest());
				log1.sendMessage(
						ChatColor.AQUA + "    You are running " + ChatColor.RED + "v" + getDescription().getVersion());
				log1.sendMessage(ChatColor.YELLOW + " ");
				log1.sendMessage(ChatColor.YELLOW + "    Download at" + ChatColor.GREEN
						+ " https://www.spigotmc.org/resources/skinsrestorer.2124/");
				log1.sendMessage(ChatColor.DARK_GREEN + "==============================================");
			} else {
				log1.sendMessage(ChatColor.DARK_GREEN + "==============================================");
				log1.sendMessage(ChatColor.YELLOW + "  SkinsRestorer Updater");
				log1.sendMessage(ChatColor.YELLOW + " ");
				log1.sendMessage(ChatColor.AQUA + "    You are running " + "v" + ChatColor.GREEN
						+ getDescription().getVersion());
				log1.sendMessage(ChatColor.GREEN + "    The latest version of SkinsRestorer!");
				log1.sendMessage(ChatColor.YELLOW + " ");
				log1.sendMessage(ChatColor.DARK_GREEN + "==============================================");
			}
		}
	}

	@Override
	public void onDisable() {
		SkinStorage.getInstance().saveData();
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
		if (version.equalsIgnoreCase("v1_7_R4")) {
			SkinFactoryv1_7_R4.getFactory().applySkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R1")) {
			SkinFactoryv1_8_R1.getFactory().applySkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R2")) {
			SkinFactoryv1_8_R2.getFactory().applySkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R3")) {
			SkinFactoryv1_8_R3.getFactory().applySkin(player);
		} else if (version.equalsIgnoreCase("v1_9_R1")) {
			SkinFactoryv1_9_R1.getFactory().applySkin(player);
		} else {
			player.sendMessage(version);
		}
	}

	@Deprecated
	public void removeSkin(Player player) {
		if (version.equalsIgnoreCase("v1_7_R4")) {
			SkinFactoryv1_7_R4.getFactory().removeSkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R1")) {
			SkinFactoryv1_8_R1.getFactory().removeSkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R2")) {
			SkinFactoryv1_8_R2.getFactory().removeSkin(player);
		} else if (version.equalsIgnoreCase("v1_8_R3")) {
			SkinFactoryv1_8_R3.getFactory().removeSkin(player);
		} else if (version.equalsIgnoreCase("v1_9_R1")) {
			SkinFactoryv1_9_R1.getFactory().removeSkin(player);
		} else {
			player.sendMessage(version);
		}
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
		return version;
	}
}
