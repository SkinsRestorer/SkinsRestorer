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

package skinsrestorer.bukkit;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import skinsrestorer.bukkit.commands.AdminCommands;
import skinsrestorer.bukkit.commands.PlayerCommands;
import skinsrestorer.bukkit.listeners.LoginListener;
import skinsrestorer.bukkit.listeners.Updater;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class SkinsRestorer extends JavaPlugin implements Listener {

	public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	static SkinsRestorer instance;
	public static SkinsRestorer getInstance() {
		return instance;
	}

	static Logger log;
    public ConsoleCommandSender log1 = null;
	private skinsrestorer.bukkit.listeners.Updater updater;

	private String version;
	public void logInfo(String message) {
		log.info(message);
	}

	@Override
	public void onEnable() {
		instance = this;
		log = getLogger();
		log1 = Bukkit.getConsoleSender();
		
		ConfigStorage.init(getDataFolder());
		LocaleStorage.init(getDataFolder());
		SkinStorage.init(getDataFolder());
		System.currentTimeMillis();
        updater = new Updater(this);
        
        updater.checkUpdates();

        if (Updater.updateAvailable())
        {
          log1.sendMessage(ChatColor.DARK_GREEN+"==============================================");
          log1.sendMessage(ChatColor.YELLOW+"  SkinsRestorer Updater  ");
          log1.sendMessage(ChatColor.YELLOW+" ");
          log1.sendMessage(ChatColor.GREEN+"    An update for SkinsRestorer has been found!");
          log1.sendMessage(ChatColor.AQUA+"    SkinsRestorer " +ChatColor.GREEN+"v"+ Updater.getHighest());
          log1.sendMessage(ChatColor.AQUA+"    You are running " +ChatColor.RED+"v"+  getDescription().getVersion());
          log1.sendMessage(ChatColor.YELLOW+" ");
          log1.sendMessage(ChatColor.YELLOW+"    Download at"+ChatColor.GREEN+" https://www.spigotmc.org/resources/skinsrestorer.2124/");
          log1.sendMessage(ChatColor.DARK_GREEN+"==============================================");
          
        }
        else
        {
        	log1.sendMessage(ChatColor.DARK_GREEN+"==============================================");
        	log1.sendMessage(ChatColor.YELLOW+"  SkinsRestorer Updater");
        	log1.sendMessage(ChatColor.YELLOW+" ");
        	log1.sendMessage(ChatColor.AQUA+"    You are running " +"v"+ ChatColor.GREEN+ getDescription().getVersion());
        	log1.sendMessage(ChatColor.GREEN+"    The latest version of SkinsRestorer!");
        	log1.sendMessage(ChatColor.YELLOW+" ");
        	log1.sendMessage(ChatColor.DARK_GREEN+"==============================================");
        }
		getCommand("skinsrestorer").setExecutor(new AdminCommands());
		getCommand("skin").setExecutor(new PlayerCommands());
		Bukkit.getPluginManager().registerEvents(new LoginListener(), this);
		String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
		this.version = version;
		executor.scheduleWithFixedDelay(CooldownStorage.cleanupCooldowns, 0, 1, TimeUnit.MINUTES);
		}
	@Override
	public void onDisable() {
		SkinStorage.getInstance().saveData();
		executor.shutdown();
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
   
   
   public boolean is1_7(){
	   if (version.contains("1_7")){
		   return true;
	   }
	   return false;
   }
   public boolean is1_8(){
	   if (version.contains("1_8")){
		   return true;
	   }
	   return false;
   }
}
