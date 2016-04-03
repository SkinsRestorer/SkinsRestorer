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

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.Updater;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class AdminCommands extends Command {

	public AdminCommands() {
		super("skinsrestorer", "skinsrestorer.cmds", new String[] { "sr" });
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(final CommandSender sender, final String[] args) {
		if (args.length == 0) {
			sender.sendMessage(C.c(LocaleStorage.getInstance().ADMIN_USE_SKIN_HELP));
			return;
		}
		if ((args.length == 1) && args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(C.c(LocaleStorage.getInstance().ADMIN_HELP));
			return;
		}
		if ((args.length == 2) && args[0].equalsIgnoreCase("drop")) {
			SkinStorage.getInstance().removeSkinData(args[1]);
			SkinsRestorer.getInstance().getFactory()
					.applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
			TextComponent component = new TextComponent(
					C.c(LocaleStorage.getInstance().SKIN_DATA_DROPPED.replace("%player", args[1])));
			sender.sendMessage(component);
			return;
		}
		if ((args.length == 2) && args[0].equalsIgnoreCase("update")) {
			final String name = args[1];
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					try {
						SkinStorage.getInstance().getOrCreateSkinData(name).attemptUpdate();
						SkinsRestorer.getInstance().getFactory()
								.applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
						TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().SKIN_DATA_UPDATED));
						sender.sendMessage(component);
					} catch (SkinFetchFailedException e) {
						TextComponent component = new TextComponent(
								C.c(LocaleStorage.getInstance().SKIN_FETCH_FAILED + e.getMessage()));
						sender.sendMessage(component);
					}
				}
			});
			return;
		}
		if ((args.length == 3) && args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("change")) {
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					String from = args[2];
					try {
						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(from, null);
						SkinStorage.getInstance().setSkinData(args[1], skinprofile);
						SkinsRestorer.getInstance().getFactory()
								.applySkin(SkinsRestorer.getInstance().getProxy().getPlayer(args[1]));
						TextComponent component = new TextComponent(
								C.c(LocaleStorage.getInstance().ADMIN_SET_SKIN.replace("%player", args[1])));
						sender.sendMessage(component);
					} catch (SkinFetchFailedException e) {
						TextComponent component = new TextComponent(
								C.c(LocaleStorage.getInstance().SKIN_FETCH_FAILED + e.getMessage()));
						sender.sendMessage(component);
					}
				}
			});
		} else
			sender.sendMessage(C.c(LocaleStorage.getInstance().ADMIN_USE_SKIN_HELP));
	}
	@SuppressWarnings("deprecation")
	public void infoCommand(CommandSender sender, String[] args){
		sender.sendMessage(C.c("&7=========== &9SkinsRestorer Info &7============"));
		String version = SkinsRestorer.getInstance().getVersion();
		sender.sendMessage(C.c("  \n&2&lVersion Info"));
		sender.sendMessage(C.c("   &fYour SkinsRestorer version is &9"+version));
		if (ConfigStorage.getInstance().UPDATE_CHECK&&Updater.updateAvailable()){
			sender.sendMessage(C.c("  \n&2&lUpdates Info"));
		sender.sendMessage(C.c("   &fThe latest version is &9"+Updater.getHighest()));
		}
		if (ConfigStorage.getInstance().USE_MYSQL){
		   sender.sendMessage(C.c("  \n&2&lMySQL Info"));
		   if (MySQL.isConnected()){
			   sender.sendMessage(C.c("    &aMySQL connection is OK."));
		   }else{
			   sender.sendMessage(C.c("    &cMySQL is enabled, but not connected!\n    In order to use MySQL please fill the \n    config with the required info!"));
		   }
		}
		sender.sendMessage(C.c("  \n&2&lOther Info"));
		sender.sendMessage(C.c("    &fMCAPI &7| &9"+ConfigStorage.getInstance().MCAPI_ENABLED));
		sender.sendMessage(C.c("    &fBot feature &7| &9"+ConfigStorage.getInstance().USE_BOT_FEATURE));
		sender.sendMessage(C.c("    &fAutoIn Skins &7| &9"+ConfigStorage.getInstance().USE_AUTOIN_SKINS));
		sender.sendMessage(C.c("    &fDisable /Skin Command &7| &9"+ConfigStorage.getInstance().DISABLE_SKIN_COMMAND));
		sender.sendMessage(C.c("    &fSkin Change Cooldown | &9"+ConfigStorage.getInstance().SKIN_CHANGE_COOLDOWN+" Seconds"));
	}
}
