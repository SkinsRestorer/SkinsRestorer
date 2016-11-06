package skinsrestorer.bukkit.commands;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

public class SkinCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "These commands are only for players");
			return true;
		}

		final Player p = (Player) sender;

		if (!p.hasPermission("skinsrestorer.playercmds")) {
			p.sendMessage(ChatColor.RED + "[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION);
			return true;
		}

		// Skin Help
		if (args.length == 0 || args.length > 1)
			p.sendMessage(Locale.PLAYER_HELP);

		// Set Skin
		else if (args.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++)
				if (args.length == 1)
					sb.append(args[i]);
				else if (args.length > 1)
					if (i + 1 == args.length)
						sb.append(args[i]);
					else
						sb.append(args[i] + " ");

			final String skin = sb.toString();

			if (Config.DISABLED_SKINS_ENABLED) {
				for (String dskin : Config.DISABLED_SKINS) {
					if (skin.equalsIgnoreCase(dskin)) {
						p.sendMessage(Locale.SKIN_DISABLED);
						return true;
					}
				}
			}

			if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
				p.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + Config.SKIN_CHANGE_COOLDOWN));
				return true;
			}

			CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

			Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {

					if (SkinStorage.getSkinData(skin) == null)
						try {
							MojangAPI.getUUID(skin);
						} catch (SkinRequestException e) {
							p.sendMessage(e.getReason());
							return;
						}

					SkinStorage.setPlayerSkin(p.getName(), skin);
					SkinsRestorer.getInstance().getFactory().updateSkin(p);
					p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
				}

			});
			return true;
		}

		return true;
	}

}
