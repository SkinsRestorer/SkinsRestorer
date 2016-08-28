package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;

public class ClearCommand extends Command {

	public ClearCommand() {
		super("clearskin", null);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, final String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(ChatColor.RED + "These commands are only for players");
			return;
		}

		final ProxiedPlayer p = (ProxiedPlayer) sender;

		if (!p.hasPermission("skinsrestorer.playercmds")) {
			p.sendMessage(ChatColor.RED + "[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION);
			return;
		}

		// Skin Clear
		if (args.length == 0) {
		    if (SkinStorage.getPlayerSkin(p.getName()).equalsIgnoreCase(p.getName())){
		    	p.sendMessage(Locale.NO_SKIN);
		    	return;
		    }
		    SkinStorage.removePlayerSkin(p.getName());
			SkinApplier.removeSkin(p);
			p.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
		}else{
			p.sendMessage(Locale.PLAYER_HELP);
		}

		return;
	}

}