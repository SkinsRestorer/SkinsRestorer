package skinsrestorer.bukkit.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

public class ClearCommand implements CommandExecutor {

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

		// Skin Clear
		if (args.length == 0) {
			Object props = null;
			
		    if (SkinStorage.getPlayerSkin(p.getName()).equalsIgnoreCase(p.getName())){
		    	p.sendMessage(Locale.NO_SKIN);
		    	return true;
		    }
		    SkinStorage.removePlayerSkin(p.getName());
		    props = SkinStorage.createProperty("textures", "", "");
			SkinsRestorer.getInstance().getFactory().applySkin(p, props);
			SkinsRestorer.getInstance().getFactory().updateSkin(p);
			p.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
		}else{
			p.sendMessage(Locale.PLAYER_HELP);
		}

		return false;
	}

}