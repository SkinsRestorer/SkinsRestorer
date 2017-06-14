package skinsrestorer.bungee.commands;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

public class PlayerCommands extends Command {

	public PlayerCommands() {
		super("skin", null);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, final String[] args) {

		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(Locale.NOT_PLAYER);
			return;
		}

		final ProxiedPlayer p = (ProxiedPlayer) sender;

		if (!Config.SKINWITHOUTPERM){
		if (p.hasPermission("skinsrestorer.playercmds")) {
		}else{
			sender.sendMessage(C.c("&c[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION));
			return;
		}
		}
		
		// Skin Help
		if (args.length == 0 || args.length > 1) {
			p.sendMessage(Locale.SR_LINE);
			p.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
			if (p.hasPermission("skinsrestorer.cmds"))
				p.sendMessage(C.c("    &2/sr &7- &fDisplay Admin commands."));
			p.sendMessage(Locale.SR_LINE);
			return;
		}

		if (args.length > 0) {

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

			if (Config.DISABLED_SKINS_ENABLED)
				for (String dskin : Config.DISABLED_SKINS)
					if (skin.equalsIgnoreCase(dskin)) {
						p.sendMessage(Locale.SKIN_DISABLED);
						return;
					}

			if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
				p.sendMessage(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName())));
				return;
			}

			CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {

				
						try {
							MojangAPI.getUUID(skin);
						SkinStorage.setPlayerSkin(p.getName(), skin);
					SkinApplier.applySkin(p);
					p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
						} catch (SkinRequestException e) {
							p.sendMessage(e.getReason());
							return;
						}
				}

			});
			return;
		} else {
			if (!Locale.SR_LINE.isEmpty())
				sender.sendMessage(Locale.SR_LINE);
			sender.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
			if (sender.hasPermission("skinsrestorer.cmds"))
				sender.sendMessage(
						ChatColor.translateAlternateColorCodes('&', "    &2/sr &7- &fDisplay Admin commands."));
			if (!Locale.SR_LINE.isEmpty())
				sender.sendMessage(Locale.SR_LINE);
		}

	}
}
