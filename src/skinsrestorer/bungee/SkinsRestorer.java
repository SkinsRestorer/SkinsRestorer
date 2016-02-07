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

import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.bungee.listeners.Updater;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;

public class SkinsRestorer extends Plugin {

	private static SkinsRestorer instance;
	public static SkinsRestorer getInstance() {
		return instance;
	}

	private Logger log;
	private Updater updater;
	private boolean autoIn = false;
	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		if (ConfigStorage.getInstance().UPDATE_CHECK == true){
			updater = new Updater(this);
	        updater.checkUpdates();
		}else{
			log.info(ChatColor.RED+"SkinsRestorer Updater is Disabled!");
			updater = null;
		}
		if (getProxy().getPluginManager().getPlugin("AutoIn")!=null){
			log.info(ChatColor.GREEN+"SkinsRestorer has detected that you are using AutoIn.");
			log.info(ChatColor.GREEN+"Check the USE_AUTOIN_SKINS option in your config!");
			autoIn = true;
		}
		ConfigStorage.init(getDataFolder());
		LocaleStorage.init(getDataFolder());
		SkinStorage.init(getDataFolder());
		new SkinFactoryBungee();
		this.getProxy().getPluginManager().registerListener(this, new LoginListener());
		this.getProxy().getPluginManager().registerCommand(this, new AdminCommands());
		this.getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
		this.getProxy().registerChannel("SkinUpdate");
		this.getProxy().getScheduler().schedule(this, CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);
		if (updater!=null){
	        if (Updater.updateAvailable())
	        {
	          log.info(ChatColor.DARK_GREEN+"==============================================");
	          log.info(ChatColor.YELLOW+"  SkinsRestorer Updater  ");
	          log.info(ChatColor.YELLOW+" ");
	          log.info(ChatColor.GREEN+"    An update for SkinsRestorer has been found!");
	          log.info(ChatColor.AQUA+"    SkinsRestorer " +ChatColor.GREEN+"v"+ Updater.getHighest());
	          log.info(ChatColor.AQUA+"    You are running " +ChatColor.RED+"v"+  getDescription().getVersion());
	          log.info(ChatColor.YELLOW+" ");
	          log.info(ChatColor.YELLOW+"    Download at"+ChatColor.GREEN+" https://www.spigotmc.org/resources/skinsrestorer.2124/");
	          log.info(ChatColor.DARK_GREEN+"==============================================");
	        }else{
	        	log.info(ChatColor.DARK_GREEN+"==============================================");
	        	log.info(ChatColor.YELLOW+"  SkinsRestorer Updater");
	        	log.info(ChatColor.YELLOW+" ");
	        	log.info(ChatColor.AQUA+"    You are running " +"v"+ ChatColor.GREEN+ getDescription().getVersion());
	        	log.info(ChatColor.GREEN+"    The latest version of SkinsRestorer!");
	        	log.info(ChatColor.YELLOW+" ");
	        	log.info(ChatColor.DARK_GREEN+"==============================================");
	        }
			}
	}

	@Override
	public void onDisable() {
		SkinStorage.getInstance().saveData();
		instance = null;
	}
	   @Deprecated
	   public void setSkin(final String playerName, final String skinName) throws SkinFetchFailedException{
						SkinsRestorerAPI.setSkin(playerName, skinName);
						}
	   @Deprecated
	   public boolean hasSkin(String playerName){
		   return SkinsRestorerAPI.hasSkin(playerName);
	   }
	   
	   public com.gmail.bartlomiejkmazur.autoin.api.AutoInAPI getAutoInAPI(){
		   return com.gmail.bartlomiejkmazur.autoin.api.APICore.getAPI();
	   }
	   
	   public boolean isAutoInEnabled(){
		   return autoIn;
	   }
}
