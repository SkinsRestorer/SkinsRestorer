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

package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinFactoryBungee;
import skinsrestorer.bungee.SkinStorage;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class AdminCommands extends Command {

	public AdminCommands() {
		super("skinsrestorer", "skinsrestorer.cmds", new String[] { "sr" });
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9Use '/skinsrestorer help' for help."));
			return;
		} if ((args.length == 1) && args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&8]&7&m-------------&r&8[ &9SkinsRestorer Admin Help &8]&7&m-------------*r&8["));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&9/skinsrestorer drop <player> &9-&a Drops player skin data."));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&9/skinsrestorer update <player> &9-&a Updates player skin data."));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
					"&9/skinsrestorer set <player> <skin name> &9-&a Sets Player's skin."));
			return;
		} if ((args.length == 2) && args[0].equalsIgnoreCase("drop")) {
			SkinStorage.getInstance().removeSkinData(args[1]);
			SkinFactoryBungee.getFactory().applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
			TextComponent component = new TextComponent("Skin data for player " + args[1] + " dropped");
			component.setColor(ChatColor.BLUE);
			sender.sendMessage(component);
			return;
		} if ((args.length == 1) && args[0].equalsIgnoreCase("savedata")) {
			SkinStorage.getInstance().saveData();
			sender.sendMessage(ChatColor.BLUE + "Skin data saved successfully.");
			return;
		} if ((args.length == 2) && args[0].equalsIgnoreCase("update")) {
			final String name = args[1];
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					try {
						SkinStorage.getInstance().getOrCreateSkinData(name).attemptUpdate();
						SkinFactoryBungee.getFactory()
								.applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
						TextComponent component = new TextComponent("Skin data updated");
						component.setColor(ChatColor.BLUE);
						sender.sendMessage(component);
					} catch (SkinFetchFailedException e) {
						TextComponent component = new TextComponent("Skin fetch failed: " + e.getMessage());
						component.setColor(ChatColor.RED);
						sender.sendMessage(component);
					}
				}
			});
			return;
		} if ((args.length == 3) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					String from = args[2];
					try {
						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
						SkinStorage.getInstance().setSkinData(args[1], skinprofile);
						SkinFactoryBungee.getFactory()
								.applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
						TextComponent component = new TextComponent(ChatColor.BLUE + "You set " + args[1] + "'s skin.");
						component.setColor(ChatColor.BLUE);
						sender.sendMessage(component);
					} catch (SkinFetchFailedException e) {
						TextComponent component = new TextComponent(
								LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_FAILED + e.getMessage());
						component.setColor(ChatColor.RED);
						sender.sendMessage(component);
					}
				}
			});
		} else
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9Use '/skinsrestorer help' for help."));
	}

}
