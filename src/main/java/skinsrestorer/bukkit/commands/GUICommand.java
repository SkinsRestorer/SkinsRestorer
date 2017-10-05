package skinsrestorer.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.bukkit.menu.SkinsGUI;
import skinsrestorer.shared.storage.Locale;

public class GUICommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "These commands are only for players");
			return true;
		}

		final Player p = (Player) sender;

		if (!p.hasPermission("skinsrestorer.playercmds.menu")) {
			p.sendMessage(ChatColor.RED + "[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION);
			return true;
		}
		SkinsGUI.getMenus().put(p.getName(), 0);
        p.openInventory(SkinsGUI.getGUI(0));
        p.sendMessage("§2Opening the Skins Menu.");
		return false;
	}
}
