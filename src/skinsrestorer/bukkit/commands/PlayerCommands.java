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

import java.util.concurrent.TimeUnit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class PlayerCommands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, final String[] args) {
		if (!sender.hasPermission("skinsrestorer.playercmds")) {
			sender.sendMessage(
					C.c( LocaleStorage.getInstance().PLAYER_HAS_NO_PERMISSION));
			return true;
		}
		if (!(sender instanceof Player)) {
			sender.sendMessage("This commands are only for players");
			return true;
		}
		final Player player = (Player) sender;
		if (args.length == 0) {
			player.sendMessage(C.c( LocaleStorage.getInstance().USE_SKIN_HELP));
			return true;
		} if ((args.length == 1) && args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(C.c( LocaleStorage.getInstance().PLAYER_HELP));
			return true;
		} if ((args.length == 1) && args[0].equalsIgnoreCase("clear")) {
			if (SkinStorage.getInstance().isSkinDataForced(player.getName())) {
				SkinStorage.getInstance().removeSkinData(player.getName());
				SkinsRestorerAPI.removeSkinBukkit(player);
				player.sendMessage(C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED));
			} else {
				SkinsRestorerAPI.removeSkinBukkit(player);
				player.sendMessage(C.c(
						LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED));
			}
			return true;
		} if ((args.length == 2) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			if (CooldownStorage.getInstance().isAtCooldown(player.getUniqueId())) {
				player.sendMessage(
						C.c( LocaleStorage.getInstance().PLAYER_SKIN_COOLDOWN
								.replace("%s", "" + ConfigStorage.getInstance().SKIN_CHANGE_COOLDOWN)));
				return true;
			}
			CooldownStorage.getInstance().setCooldown(player.getUniqueId(),
					ConfigStorage.getInstance().SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);
			SkinsRestorer.executor.execute(new Runnable() {
				@Override
				public void run() {
					String from = args[1];
					try {
						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
						SkinStorage.getInstance().setSkinData(player.getName(), skinprofile);
						skinprofile.attemptUpdate();
						SkinsRestorerAPI.applySkinBukkit(player);
						player.sendMessage(C.c(
								LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS));
					} catch (SkinFetchFailedException e) {
						player.sendMessage(C.c(
								LocaleStorage.getInstance().SKIN_FETCH_FAILED) + e.getMessage());
					}
				}
			});
			return true;
		} else
			player.sendMessage(C.c( LocaleStorage.getInstance().USE_SKIN_HELP));
		return false;
	}
}
