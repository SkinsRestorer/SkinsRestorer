package skinsrestorer.bukkit.commands;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SrCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, Command arg1, String arg2, String[] args) {

		if (!sender.hasPermission("skinsrestorer.cmds") && !sender.isOp()) {
			sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
			return true;
		}

		if (args.length == 0)
			sender.sendMessage(Locale.ADMIN_HELP);

		else if (args.length > 2 && args[0].equalsIgnoreCase("set")) {
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
			Player player = Bukkit.getPlayer(args[1]);

			if (player == null)
				for (Player pl : Bukkit.getOnlinePlayers()) {
					if (pl.getName().startsWith(args[1])) {
						player = pl;
						break;
					}
				}

			if (player == null) {
				sender.sendMessage(Locale.NOT_ONLINE);
				return true;
			}

			final Player p = player;

			Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

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
					SkinsRestorer.getInstance().getFactory().updateSkin(p);
					sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
					return;
				}

			});
		} else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			Locale.load();
			Config.load(SkinsRestorer.getInstance().getResource("config.yml"));
			sender.sendMessage(Locale.RELOAD);
		} else if (args.length > 1 && args[0].equalsIgnoreCase("drop")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < args.length; i++)
				sb.append(args[i]);

			SkinStorage.removeSkinData(sb.toString());

			sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));
		} else if (args.length > 1 && args[0].equalsIgnoreCase("remove")) {
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < args.length; i++)
				sb.append(args[i]);

			SkinStorage.removePlayerSkin(sb.toString());

			sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));
		} else if ((args.length > 0) && args[0].equalsIgnoreCase("props")) {

			Player p = null;

			if (args.length == 1) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(Locale.NOT_PLAYER);
					return true;
				}
				p = ((Player) sender);
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

				p = Bukkit.getPlayer(name);

				if (p == null) {
					sender.sendMessage(Locale.NOT_ONLINE);
					return true;
				}
			}
			try {
				Object ep = ReflectionUtil.invokeMethod(p, "getHandle");
				Object profile = ReflectionUtil.invokeMethod(ep, "getProfile");
				Object propmap = ReflectionUtil.invokeMethod(profile, "getProperties");

				Collection<?> props = (Collection<?>) ReflectionUtil.invokeMethod(propmap.getClass(), propmap, "get",
						new Class[] { Object.class }, "textures");

				if (props == null || props.isEmpty()) {
					sender.sendMessage(Locale.NO_SKIN_DATA);
					return true;
				}

				for (Object prop : props) {

					String name = (String) ReflectionUtil.invokeMethod(prop, "getName");
					String value = (String) ReflectionUtil.invokeMethod(prop, "getValue");
					String signature = (String) ReflectionUtil.invokeMethod(prop, "getSignature");

					ConsoleCommandSender cons = Bukkit.getConsoleSender();

					cons.sendMessage(C.c("\n&aName: &8" + name));
					cons.sendMessage(C.c("\n&aValue : &8" + value));
					cons.sendMessage(C.c("\n&aSignature : &8" + signature));

					String decoded = Base64Coder.decodeString(value);
					cons.sendMessage(C.c("\n&aValue Decoded: &e" + decoded));

					sender.sendMessage(C.c("\n&e" + decoded));
					sender.sendMessage(C.c("&cMore info in console!"));
				}
			} catch (Exception e) {
				e.printStackTrace();
				sender.sendMessage(Locale.NO_SKIN_DATA);
				return true;
			}
			sender.sendMessage(C.c("&cMore info in console!"));

		}

		return true;
	}

}
