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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.ReflectionUtil;

public class AdminCommands extends Command {

	public AdminCommands() {
		super("skinsrestorer", null, new String[] { "sr" });
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(final CommandSender sender, final String[] args) {

		if (!sender.hasPermission("skinsrestorer.cmds")) {
			sender.sendMessage(C.c("&c[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION));
			return;
		}

		if (args.length == 0) {
			sender.sendMessage(Locale.ADMIN_HELP);

		} else if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
			sender.sendMessage(Locale.ADMIN_HELP);

		} else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			Locale.load();
			Config.load(SkinsRestorer.getInstance().getResourceAsStream("config.yml"));
			sender.sendMessage(Locale.RELOAD);

		} else if ((args.length == 2) && args[0].equalsIgnoreCase("drop")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < args.length; i++)
				sb.append(args[i]);

			SkinStorage.removeSkinData(sb.toString());
			sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));

		} else if (args.length > 2 && args[0].equalsIgnoreCase("set")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 2; i < args.length; i++)
				sb.append(args[i]);

			String skin = sb.toString();
			ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[1]);

			if (player == null)
				for (ProxiedPlayer pl : ProxyServer.getInstance().getPlayers()) {
					if (pl.getName().startsWith(args[1])) {
						player = pl;
						break;
					}
				}

			if (player == null) {
				sender.sendMessage(Locale.NOT_ONLINE);
				return;
			}

			ProxiedPlayer p = player;

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {

					Property props = null;

					try {
						props = (Property) MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));
					} catch (SkinRequestException e) {
						p.sendMessage(e.getReason());
						sender.sendMessage(e.getReason());
						props = (Property) SkinStorage.getSkinData(skin);

						if (props != null) {
							SkinStorage.setPlayerSkin(p.getName(), skin);
							SkinApplier.applySkin(p);
							p.sendMessage(Locale.SKIN_CHANGE_SUCCESS_DATABASE);
							sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS_DATABASE);
							return;
						}
						return;
					}

					SkinStorage.setSkinData(skin, props);
					SkinStorage.setPlayerSkin(p.getName(), skin);
					SkinApplier.applySkin(p);
					p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
				}

			});
		} else if ((args.length == 1) && args[0].equalsIgnoreCase("debug")) {
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {
					File debug = new File(SkinsRestorer.getInstance().getDataFolder(), "debug.txt");

					PrintWriter out = null;

					try {
						if (!debug.exists())
							debug.createNewFile();
						out = new PrintWriter(new FileOutputStream(debug, true), true);
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					String player1 = "Notch";
					String player2 = "Blackfire62";

					if (sender instanceof ProxiedPlayer)
						player1 = sender.getName();

					try {

						out.println("Java version: " + System.getProperty("java.version"));
						out.println("Bungee version: " + ProxyServer.getInstance().getVersion());
						out.println(
								"SkinsRestoerer version: " + SkinsRestorer.getInstance().getDescription().getVersion());
						out.println();

						String plugins = "";
						for (Plugin plugin : ProxyServer.getInstance().getPluginManager().getPlugins())
							plugins += plugin.getDescription().getName() + " (" + plugin.getDescription().getVersion()
									+ "), ";

						out.println("Plugin list: " + plugins);
						out.println();
						out.println("Property output from MojangAPI (" + player1 + ") : ");

						Property props = (Property) MojangAPI.getSkinProperty(MojangAPI.getUUID(player1));

						out.println("Name: " + props.getName());
						out.println("Value: " + props.getValue());
						out.println("Signature: " + props.getSignature());
						out.println();

						out.println("Raw data from MojangAPI (" + player2 + "): ");

						String output = (String) ReflectionUtil.invokeMethod(MojangAPI.class, null, "readURL",
								new Class<?>[] { String.class },
								"https://sessionserver.mojang.com/session/minecraft/profile/"
										+ MojangAPI.getUUID(player2) + "?unsigned=false");

						out.println(output);

						out.println("\n\n\n\n\n\n\n\n\n\n");

					} catch (Exception e) {
						out.println("=========================================");
						e.printStackTrace(out);
						out.println("=========================================");
					}

					sender.sendMessage(ChatColor.RED + "[SkinsRestorer] Debug file crated!");
					sender.sendMessage(ChatColor.RED
							+ "[SkinsRestorer] Please check the contents of the file and send the contents to developers, if you are experiencing problems!");
					sender.sendMessage(ChatColor.RED + "[SkinsRestorer] URL for error reporting: " + ChatColor.YELLOW
							+ "https://github.com/Th3Tr0LLeR/SkinsRestorer---Maro/issues");
				}
			});
			return;
		} else
			sender.sendMessage(Locale.ADMIN_HELP);
	}

}
