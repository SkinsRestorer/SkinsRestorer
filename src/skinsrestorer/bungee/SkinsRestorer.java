/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package skinsrestorer.bungee;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.MessageListener;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.Factory;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.Updater;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;

	public static SkinsRestorer getInstance() {
		return instance;
	}

	private Logger log;
	private Updater updater;
	private Factory factory;
	private boolean autoIn = false;

	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		getDataFolder().mkdirs();
		ConfigStorage.getInstance().init(this.getResourceAsStream("config.yml"), false);
		LocaleStorage.init();
		if (ConfigStorage.getInstance().UPDATE_CHECK == true) {
			updater = new Updater(getDescription().getVersion());
			updater.checkUpdates();
		} else {
			log.info(ChatColor.RED + "SkinsRestorer Updater is Disabled!");
			updater = null;
		}
		if (getProxy().getPluginManager().getPlugin("AutoIn") != null) {
			log.info(ChatColor.GREEN + "SkinsRestorer has detected that you are using AutoIn.");
			log.info(ChatColor.GREEN + "Check the USE_AUTOIN_SKINS option in your config!");
			autoIn = true;
		}

		if (ConfigStorage.getInstance().USE_MYSQL)
			SkinStorage./*
						*/init(/* 
								**/new MySQL(ConfigStorage./*
															* */getInstance().MYSQL_HOST,
					ConfigStorage.getInstance().MYSQL_PORT, ConfigStorage.getInstance().MYSQL_DATABASE,
					ConfigStorage.getInstance().MYSQL_USERNAME, ConfigStorage.getInstance().MYSQL_PASSWORD));
		else {
			SkinStorage.init();
			this.getProxy().getScheduler().schedule(this, CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);
		}

		try {
			Class<?> factory = Class.forName("skinsrestorer.bungee.SkinFactoryBungee");
			this.factory = (Factory) factory.newInstance();
			log.info("[SkinsRestorer] Loaded Skin Factory for Bungeecord");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			getProxy().getPluginManager().unregisterListeners(this);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		this.getProxy().getPluginManager().registerListener(this, new LoginListener());
		this.getProxy().getPluginManager().registerListener(this, new MessageListener());
		this.getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		this.getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		this.getProxy().registerChannel("SkinUpdate");

		if (updater != null) {
			if (Updater.updateAvailable()) {
				log.info(ChatColor.DARK_GREEN + "==============================================");
				log.info(ChatColor.YELLOW + "  SkinsRestorer Updater  ");
				log.info(ChatColor.YELLOW + " ");
				log.info(ChatColor.GREEN + "    An update for SkinsRestorer has been found!");
				log.info(ChatColor.AQUA + "    SkinsRestorer " + ChatColor.GREEN + "v" + Updater.getHighest());
				log.info(ChatColor.AQUA + "    You are running " + ChatColor.RED + "v" + getDescription().getVersion());
				log.info(ChatColor.YELLOW + " ");
				log.info(ChatColor.YELLOW + "    Download at" + ChatColor.GREEN
						+ " https://www.spigotmc.org/resources/skinsrestorer.2124/");
				log.info(ChatColor.DARK_GREEN + "==============================================");
			} else {
				log.info(ChatColor.DARK_GREEN + "==============================================");
				log.info(ChatColor.YELLOW + "  SkinsRestorer Updater");
				log.info(ChatColor.YELLOW + " ");
				log.info(ChatColor.AQUA + "    You are running " + "v" + ChatColor.GREEN
						+ getDescription().getVersion());
				log.info(ChatColor.GREEN + "    The latest version of SkinsRestorer!");
				log.info(ChatColor.YELLOW + " ");
				log.info(ChatColor.DARK_GREEN + "==============================================");
			}
		}
	}

	@Override
	public void onDisable() {
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

	public com.gmail.bartlomiejkmazur.autoin.api.AutoInAPI getAutoInAPI() {
		return com.gmail.bartlomiejkmazur.autoin.api.APICore.getAPI();
	}

	public Factory getFactory() {
		return factory;
	}

	public String getVersion(){
		return getDescription().getVersion();
	}
	public boolean isAutoInEnabled() {
		return autoIn;
	}
}
