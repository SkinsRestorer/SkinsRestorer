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

package skinsrestorer.bukkit.commands;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class PlayerCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, final String[] args) {
		if (!sender.hasPermission("skinsrestorer.playercmds")) {
			sender.sendMessage(ChatColor.RED+LocaleStorage.getInstance().PLAYER_HAS_NO_PERMISSION);
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage("This commands are only for players");
			return true;
		}
		final Player player = (Player) sender;
		if (args.length == 0){
			player.sendMessage(ChatColor.BLUE+LocaleStorage.getInstance().USE_SKIN_HELP);
			return true;
		}else
		if ((args.length == 1) && args[0].equalsIgnoreCase("help")){
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8]&7&m-------------&r&8[ &9SkinsRestorer Help &8]&7&m-------------*r&8["));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9/skin set <skinname> &9-&a Sets your skin. &7&o//requires relog"));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9/skin clear &9-&a Clears your skin &7&o//requires relog"));
			return true;
		}else
		if ((args.length == 1) && args[0].equalsIgnoreCase("clear")) {
			if (SkinStorage.getInstance().isSkinDataForced(player.getName())) {
				SkinStorage.getInstance().removeSkinData(player.getName());
		    	SkinsRestorer.getInstance().applySkin(player);
				player.sendMessage(ChatColor.BLUE+LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED);
			}
			return true;
		} else
		if ((args.length == 2) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			/*if (CooldownStorage.getInstance().isAtCooldown(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED+LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_COOLDOWN);
				return true;
			}*/
			SkinsRestorer.executor.execute(
				new Runnable() {
					@Override
					public void run() {
						String from = args[1];
						try {
							SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
							SkinStorage.getInstance().setSkinData(player.getName(), skinprofile);
							skinprofile.attemptUpdate();
					    	SkinsRestorer.getInstance().applySkin(player);
							player.sendMessage(ChatColor.BLUE+LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS);
						} catch (SkinFetchFailedException e) {
							player.sendMessage(ChatColor.RED+LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_FAILED+e.getMessage());
						}
					}
				}
			);
			return true;
		}else
			player.sendMessage(ChatColor.BLUE+LocaleStorage.getInstance().USE_SKIN_HELP);
		return false;
	}
}
