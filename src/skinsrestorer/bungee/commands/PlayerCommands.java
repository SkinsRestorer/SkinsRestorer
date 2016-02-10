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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinFactoryBungee;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class PlayerCommands extends Command {

	public PlayerCommands() {
		super("skin", "skinsrestorer.playercmds");
	}
	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, final String[] args) {
		if (!(sender instanceof ProxiedPlayer)) {
			TextComponent component = new TextComponent("This commands are only for players");
			sender.sendMessage(component);
			return;
		}
		final ProxiedPlayer player = (ProxiedPlayer) sender;
		if (args.length == 0){
			TextComponent component = new TextComponent(LocaleStorage.getInstance().USE_SKIN_HELP);
			component.setColor(ChatColor.BLUE);
		    sender.sendMessage(component);
			return;
		}else
		if ((args.length == 1) && args[0].equalsIgnoreCase("help")){
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8]&7&m-------------&r&8[ &9SkinsRestorer Help &8]&7&m-------------*r&8["));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9/skin set <skinname> &9-&a Sets your skin."));
			sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&9/skin clear &9-&a Clears your skin."));
			return;
		}else
		if ((args.length == 1) && args[0].equalsIgnoreCase("clear")) {
			if (SkinStorage.getInstance().isSkinDataForced(player.getName())) {
				SkinStorage.getInstance().removeSkinData(player.getName());
				SkinFactoryBungee.getFactory().removeSkin(player);
				TextComponent component = new TextComponent(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED);
				component.setColor(ChatColor.BLUE);
				player.sendMessage(component);
			}
		} else
			if ((args.length == 2) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			/*if (CooldownStorage.getInstance().isAtCooldown(player.getUniqueId())) {
				TextComponent component = new TextComponent(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_COOLDOWN);
				component.setColor(ChatColor.RED);
				player.sendMessage(component);
				return;
			}
			CooldownStorage.getInstance().setCooldown(player.getUniqueId(), 10, TimeUnit.MINUTES);*/
				ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				    @Override
				    public void run() {

						String from = args[1];
						try {
							SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
							SkinStorage.getInstance().setSkinData(player.getName(), skinprofile);
							SkinFactoryBungee.getFactory().applySkin(player);
							TextComponent component = new TextComponent(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS);
							component.setColor(ChatColor.BLUE);
							player.sendMessage(component);
						} catch (SkinFetchFailedException e) {
							TextComponent component = new TextComponent(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_FAILED+e.getMessage());
							component.setColor(ChatColor.RED);
							player.sendMessage(component);
							CooldownStorage.getInstance().resetCooldown(player.getUniqueId());
						}
					}
				});
		}else{
			TextComponent component = new TextComponent(LocaleStorage.getInstance().USE_SKIN_HELP);
			component.setColor(ChatColor.BLUE);
		    sender.sendMessage(component);
		}
	}
	}
