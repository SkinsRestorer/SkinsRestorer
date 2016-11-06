package skinsrestorer.bungee.commands;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

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

		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			Locale.load();
			Config.load(SkinsRestorer.getInstance().getResourceAsStream("config.yml"));
			sender.sendMessage(Locale.RELOAD);

		} else if ((args.length == 2) && args[0].equalsIgnoreCase("drop")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 2; i < args.length; i++)
				if (args.length == 3)
					sb.append(args[i]);
				else if (args.length > 3)
					if (i + 1 == args.length)
						sb.append(args[i]);
					else
						sb.append(args[i] + " ");

			SkinStorage.removeSkinData(sb.toString());
			sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));

		} else if (args.length > 2 && args[0].equalsIgnoreCase("set")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 2; i < args.length; i++)
				if (args.length == 3)
					sb.append(args[i]);
				else if (args.length > 3)
					if (i + 1 == args.length)
						sb.append(args[i]);
					else
						sb.append(args[i] + " ");

			final String skin = sb.toString();
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

			final ProxiedPlayer p = player;

			SkinsRestorer.getInstance().getExecutor().submit(new Runnable() {

				@Override
				public void run() {

					if (SkinStorage.getSkinData(skin) == null)
						try {
							MojangAPI.getUUID(skin);
						} catch (SkinRequestException e) {
							sender.sendMessage(e.getReason());
							return;
						}

					SkinStorage.setPlayerSkin(p.getName(), skin);
					SkinApplier.applySkin(p);
					sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
				}

			});
		} else if ((args.length > 0) && args[0].equalsIgnoreCase("props")) {

			ProxiedPlayer p = null;

			if (args.length == 1) {
				if (!(sender instanceof ProxiedPlayer)) {
					sender.sendMessage(Locale.NOT_PLAYER);
					return;
				}
				p = ((ProxiedPlayer) sender);
			} else if (args.length > 1) {
				String name = "";
				for (int i = 1; i < args.length; i++)
					if (args.length == 2)
						name += args[i];
					else if (args.length > 2)
						if (i + 1 == args.length)
							name += args[i];
						else
							name += args[i] + " ";

				p = ProxyServer.getInstance().getPlayer(name);

				if (p == null) {
					sender.sendMessage(Locale.NOT_ONLINE);
					return;
				}
			}

			InitialHandler h = (InitialHandler) p.getPendingConnection();
			Property prop = h.getLoginProfile().getProperties()[0];

			if (prop == null) {
				sender.sendMessage(Locale.NO_SKIN_DATA);
				return;
			}

			String decoded = Base64Coder.decodeString(prop.getValue());

			CommandSender cons = ProxyServer.getInstance().getConsole();

			cons.sendMessage(C.c("\n&aName: &8" + prop.getName()));
			cons.sendMessage(C.c("\n&aValue Decoded: &e" + decoded));
			cons.sendMessage(C.c("\n&aValue : &8" + prop.getValue()));
			cons.sendMessage(C.c("\n&aSignature : &8" + prop.getSignature()));

			sender.sendMessage(C.c("\n&e" + decoded));

			sender.sendMessage(C.c("&cMore info in console!"));

		} else
			sender.sendMessage(Locale.ADMIN_HELP);
	}

}
