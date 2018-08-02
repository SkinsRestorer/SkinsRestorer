package skinsrestorer.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.ServiceChecker;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SrCommand implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, Command arg1, String arg2, String[] args) {

        if (!sender.hasPermission("skinsrestorer.cmds")) {
            sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
            return true;
        }
        if (args.length == 0) {
            if (!Locale.SR_LINE.isEmpty())
                sender.sendMessage(Locale.SR_LINE);
            sender.sendMessage(Locale.HELP_ADMIN.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
            if (!Locale.SR_LINE.isEmpty())
                sender.sendMessage(Locale.SR_LINE);
        } else if (args.length > 2 && args[0].equalsIgnoreCase("set")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++)
                if (args.length == 3)
                    sb.append(args[i]);
                else if (i + 1 == args.length)
                    sb.append(args[i]);
                else
                    sb.append(args[i]).append(" ");

            final String skin = sb.toString();
            Player player = Bukkit.getPlayer(args[1]);

            if (player == null)
                for (Player pl : Bukkit.getOnlinePlayers())
                    if (pl.getName().startsWith(args[1])) {
                        player = pl;
                        break;
                    }

            if (player == null) {
                sender.sendMessage(Locale.NOT_ONLINE);
                return true;
            }

            final Player p = player;

            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                try {
                    MojangAPI.getUUID(skin);
                    SkinStorage.setPlayerSkin(p.getName(), skin);
                    SkinsRestorer.getInstance().getFactory().applySkin(p, SkinStorage.getOrCreateSkinForPlayer(p.getName()));
                    sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                } catch (SkinRequestException e) {
                    sender.sendMessage(e.getReason());
                }
            });
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            Locale.load();
            Config.load(SkinsRestorer.getInstance().getResource("config.yml"));
            sender.sendMessage(Locale.RELOAD);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("config")) {
            sender.sendMessage("§e[§2SkinsRestorer§e] §2/sr config has been removed from SkinsRestorer. Farewell!");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("status")) {

            sender.sendMessage("Checking needed services for SR to work properly...");

            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                ServiceChecker checker = new ServiceChecker();
                checker.checkServices();

                ServiceChecker.ServiceCheckResponse response = checker.getResponse();
                List<String> results = response.getResults();

                for (String result : results) {
                    sender.sendMessage(result);
                }
                sender.sendMessage("Working UUID API count: " + response.getWorkingUUID());
                sender.sendMessage("Working Profile API count: " + response.getWorkingProfile());
                if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                    sender.sendMessage("The plugin is currently in a working state.");
                else
                    sender.sendMessage("Plugin currently can't fetch new skins. You might check out our discord at https://discordapp.com/invite/012gnzKK9EortH0v2?utm_source=Discord%20Widget&utm_medium=Connect");
                sender.sendMessage("Finished checking services..");
            });

        } else if (args.length > 1 && args[0].equalsIgnoreCase("drop")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++)
                sb.append(args[i]);

            SkinStorage.removeSkinData(sb.toString());

            sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));

        } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
            String nick = args[1];
            Player player = Bukkit.getPlayer(nick);

            if (player == null) {
                sender.sendMessage(Locale.NOT_ONLINE);
                return true;
            }

            final Player p = player;

            // Todo: Make sure to check if DefaultSkins are enabled and set the correct skin
            Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                Object props;

                SkinStorage.removePlayerSkin(p.getName());
                props = SkinStorage.createProperty("textures", "", "");
                SkinsRestorer.getInstance().getFactory().applySkin(p, props);
                SkinsRestorer.getInstance().getFactory().updateSkin(p);
                p.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
                sender.sendMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", p.getName()));
            });
        } else if (args[0].equalsIgnoreCase("props")) {

            Player p;

            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Locale.NOT_PLAYER);
                    return true;
                }
                p = (Player) sender;
            } else {
                StringBuilder name = new StringBuilder();
                for (int i = 1; i < args.length; i++)
                    if (args.length == 2)
                        name.append(args[i]);
                    else if (i + 1 == args.length)
                        name.append(args[i]);
                    else
                        name.append(args[i]).append(" ");

                p = Bukkit.getPlayer(name.toString());

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
                        new Class[]{Object.class}, "textures");

                if (props == null || props.isEmpty()) {
                    sender.sendMessage(Locale.NO_SKIN_DATA);
                    return true;
                }

                for (Object prop : props) {

                    String name = (String) ReflectionUtil.invokeMethod(prop, "getName");
                    String value = (String) ReflectionUtil.invokeMethod(prop, "getValue");
                    String signature = (String) ReflectionUtil.invokeMethod(prop, "getSignature");

                    ConsoleCommandSender cons = Bukkit.getConsoleSender();

                    cons.sendMessage("\n§aName: §8" + name);
                    cons.sendMessage("\n§aValue : §8" + value);
                    cons.sendMessage("\n§aSignature : §8" + signature);

                    byte[] decoded = Base64.getDecoder().decode(value);
                    cons.sendMessage("\n§aValue Decoded: §e" + Arrays.toString(decoded));

                    sender.sendMessage("\n§e" + Arrays.toString(decoded));
                    sender.sendMessage("§cMore info in console!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(Locale.NO_SKIN_DATA);
                return true;
            }
            sender.sendMessage("§cMore info in console!");

        }
        return true;
    }
}
