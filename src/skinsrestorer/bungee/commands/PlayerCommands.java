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

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class PlayerCommands extends Command {

	public PlayerCommands() {
		super("skin", "skinsrestorer.playercmds");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, final String[] args) {
		if (ConfigStorage.getInstance().DISABLE_SKIN_COMMAND) {
			sender.sendMessage(C.c(LocaleStorage.getInstance().UNKNOWN_COMMAND));
			return;
		}
		if (!(sender instanceof ProxiedPlayer)) {
			TextComponent component = new TextComponent("This commands are only for players");
			sender.sendMessage(component);
			return;
		}
		final ProxiedPlayer player = (ProxiedPlayer) sender;
		if (args.length == 0) {
			TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().USE_SKIN_HELP));
			sender.sendMessage(component);
			return;
		} else if ((args.length == 1) && args[0].equalsIgnoreCase("help")) {
			for (String s : LocaleStorage.getInstance().PLAYER_HELP) {
				sender.sendMessage(C.c(s));
			}
			return;
		}
		if ((args.length == 1) && args[0].equalsIgnoreCase("clear")) {
			SkinStorage.getInstance().removePlayerSkin(player.getName());
			TextComponent component = new TextComponent(
					C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED));
			player.sendMessage(component);
			SkinsRestorer.getInstance().getFactory().removeSkin(player);
			return;
		}
		if ((args.length == 2) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			if (!player.hasPermission("skinsrestorer.bypasscooldown")) {
				if (CooldownStorage.getInstance().isAtCooldown(player.getUniqueId())) {
					TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().PLAYER_SKIN_COOLDOWN
							.replace("%s", "" + ConfigStorage.getInstance().SKIN_CHANGE_COOLDOWN)));
					player.sendMessage(component);
					return;
				}
				CooldownStorage.getInstance().setCooldown(player.getUniqueId(),
						ConfigStorage.getInstance().SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);
			}
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {

					String from = args[1];
					if (!player.hasPermission("skinsrestorer.disabledskins")) {
						for (String disabledSkin : ConfigStorage.getInstance().DISABLED_SKINS) {
							if (args[1].equalsIgnoreCase(disabledSkin)) {
								player.sendMessage(LocaleStorage.getInstance().DISABLED_SKIN);
								return;
							}
						}
					}
					try {
						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
						SkinStorage.getInstance().setSkinData(skinprofile);
						SkinStorage.getInstance().setPlayerSkin(player.getName(), skinprofile.getName());
						SkinsRestorer.getInstance().getFactory().applySkin(player);
						TextComponent component = new TextComponent(
								C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS));
						player.sendMessage(component);
					} catch (SkinFetchFailedException e) {
						SkinProfile skinprofile = SkinStorage.getInstance().getSkinData(from);
						if (skinprofile != null) {
							SkinStorage.getInstance().setSkinData(skinprofile);
							SkinStorage.getInstance().setPlayerSkin(player.getName(), skinprofile.getName());
							SkinsRestorer.getInstance().getFactory().applySkin(player);
							TextComponent component = new TextComponent(
									C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS_DATABASE));
							player.sendMessage(component);
						} else {
							TextComponent component = new TextComponent(
									C.c(LocaleStorage.getInstance().SKIN_FETCH_FAILED + e.getMessage()));
							player.sendMessage(component);
							CooldownStorage.getInstance().resetCooldown(player.getUniqueId());
						}
					}
				}
			});
		} else {
			TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().USE_SKIN_HELP));
			sender.sendMessage(component);
		}
	}
}
