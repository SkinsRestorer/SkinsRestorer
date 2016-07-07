package skinsrestorer.bungee.commands;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.connection.LoginResult.Property;
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

		ProxiedPlayer p = (ProxiedPlayer) sender;

		if (!p.hasPermission("skinsrestorer.cmds")) {
			sender.sendMessage(C.c("&c[SkinsRestorer] " + SkinsRestorer.getInstance().getVersion() + "\n"
					+ Locale.PLAYER_HAS_NO_PERMISSION));
			return;
		}

		if (args.length == 0) {
			sender.sendMessage(Locale.PLAYER_HELP);
			return;
		} else if (args.length > 0) {

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++)
				if (args.length == 1)
					sb.append(args[i]);
				else if (args.length > 1)
					if (i + 1 == args.length)
						sb.append(args[i]);
					else
						sb.append(args[i] + " ");

			String skin = sb.toString();

			if (Config.DISABLED_SKINS_ENABLED) {
				for (String dskin : Config.DISABLED_SKINS) {
					if (skin.equalsIgnoreCase(dskin)) {
						p.sendMessage(Locale.SKIN_DISABLED);
						return;
					}
				}
			}

			if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.isAtCooldown(p.getUniqueId())) {
				p.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + Config.SKIN_CHANGE_COOLDOWN));
				return;
			}

			CooldownStorage.setCooldown(p.getUniqueId(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {

					Property props = null;

					try {
						props = (Property) MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));
					} catch (SkinRequestException e) {
						if (e.getReason().equals(Locale.NOT_PREMIUM))
							CooldownStorage.resetCooldown(p.getUniqueId());
						p.sendMessage(e.getReason());
						props = (Property) SkinStorage.getSkinData(skin);

						if (props != null) {
							SkinStorage.setPlayerSkin(p.getName(), skin);
							SkinApplier.applySkin(p);
							p.sendMessage(Locale.SKIN_CHANGE_SUCCESS_DATABASE);
							return;
						}
						return;
					}

					SkinStorage.setSkinData(skin, props);
					SkinStorage.setPlayerSkin(p.getName(), skin);
					SkinApplier.applySkin(p);
					p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
				}

			});
			return;
		} else {
			sender.sendMessage(Locale.PLAYER_HELP);
		}
	}
}
