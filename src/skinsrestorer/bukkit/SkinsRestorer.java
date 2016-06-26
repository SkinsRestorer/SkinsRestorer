package skinsrestorer.bukkit;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import skinsrestorer.bukkit.commands.Commands;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.bukkit.metrics.Metrics;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.SkinsPacketHandler;
import skinsrestorer.shared.utils.Updater;

public class SkinsRestorer extends JavaPlugin implements Listener {

	private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private boolean autoIn = false;
	private static SkinsRestorer instance;
	private Logger log;
	private ConsoleCommandSender coloredLog = null;
	private Updater updater;
	private MySQL mysql;
	private boolean bungeeEnabled;

	public static SkinsRestorer getInstance() {
		return instance;
	}

	public static ScheduledExecutorService getExecutor() {
		return executor;
	}

	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		coloredLog = Bukkit.getConsoleSender();
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
								Property textures = new Property(in.readUTF(), in.readUTF(), in.readUTF());

								Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(player);
								Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
								GameProfile profile = (GameProfile) ReflectionUtil.invokeMethod(ep.getClass(), ep,
										"getProfile");

								profile.getProperties().get(textures.getName()).clear();
								profile.getProperties().put(textures.getName(), textures);
							} catch (Exception e) {
							}
							SkinsPacketHandler.updateSkin(player);
						}
					});
				}
			});
			return;
		}

		getDataFolder().mkdirs();
		ConfigStorage.getInstance().init(this.getResource("config.yml"), false);
		LocaleStorage.init();
		if (ConfigStorage.getInstance().USE_MYSQL)
			SkinStorage.init(mysql = new MySQL(ConfigStorage.getInstance().MYSQL_HOST,
					ConfigStorage.getInstance().MYSQL_PORT, ConfigStorage.getInstance().MYSQL_DATABASE,
					ConfigStorage.getInstance().MYSQL_USERNAME, ConfigStorage.getInstance().MYSQL_PASSWORD));
		else
			SkinStorage.init();

		executor.scheduleWithFixedDelay(CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);

		getCommand("skinsrestorer").setExecutor(new Commands());
		getCommand("skin").setExecutor(new Commands());

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

		for (Player p : Bukkit.getOnlinePlayers())
			SkinsPacketHandler.inject(p);

		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);

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
		if (!bungeeEnabled)
			for (Player p : Bukkit.getOnlinePlayers())
				SkinsPacketHandler.uninject(p);
		instance = null;
	}

	public boolean isAutoInEnabled() {
		return autoIn;
	}

	public String getVersion() {
		return this.getDescription().getVersion();
	}

	public MySQL getMySQL() {
		return mysql;
	}

	public ConsoleCommandSender getColoredLog() {
		return coloredLog;
	}
}
