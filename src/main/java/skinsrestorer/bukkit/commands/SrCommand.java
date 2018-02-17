package skinsrestorer.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.bukkit.storage.Locale;
import skinsrestorer.bukkit.storage.SkinStorage;
import skinsrestorer.bukkit.utils.MojangAPI;
import skinsrestorer.bukkit.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.util.Collection;

public class SrCommand implements CommandExecutor {

    @Override
    public boolean onCommand(final CommandSender sender, Command arg1, String arg2, String[] args) {

        if (sender.hasPermission("skinsrestorer.cmds")) {

            if (args.length == 0) {
                sender.sendMessage(Locale.SR_LINE.toString());
                sender.sendMessage(Locale.HELP_ADMIN.toString().replace("%ver%", SkinsRestorer.getInstance().getVersion()));
                sender.sendMessage(Locale.SR_LINE.toString());
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
                Player player = Bukkit.getPlayer(args[1]);

                if (player == null)
                    for (Player pl : Bukkit.getOnlinePlayers())
                        if (pl.getName().startsWith(args[1])) {
                            player = pl;
                            break;
                        }

                if (player == null) {
                    sender.sendMessage(Locale.TITLE.toString() + Locale.NOT_ONLINE);
                    return true;
                }

                final Player p = player;

                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

                    @Override
                    public void run() {
                        try {
                            MojangAPI.getUUID(skin);
                            SkinStorage.setPlayerSkin(p.getName(), skin);
                            SkinsRestorer.getInstance().getFactory().applySkin(p, SkinStorage.getOrCreateSkinForPlayer(p.getName()));
                            sender.sendMessage(Locale.TITLE.toString() + Locale.SKIN_CHANGE_SUCCESS);
                            return;
                        } catch (SkinRequestException e) {
                            sender.sendMessage(e.getReason());
                            return;
                        }
                    }

                });
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                SkinsRestorer.getInstance().reloadConfig();
                SkinsRestorer.getInstance().saveDefaultConfig();
                sender.sendMessage(Locale.TITLE.toString() + Locale.RELOAD);
            } else if (args.length == 1 && args[0].equalsIgnoreCase("config")) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §2/sr config has been removed from SkinsRestorer. Farewell!");
            } else if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                try {
                    MojangAPI.getSkinProperty(MojangAPI.getUUID("Notch"));
                    sender.sendMessage(Locale.TITLE.toString() + Locale.STATUS_OK);
                } catch (SkinRequestException e) {
                    sender.sendMessage(e.getReason());
                }
            } else if (args.length > 1 && args[0].equalsIgnoreCase("drop")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++)
                    sb.append(args[i]);

                SkinStorage.removeSkinData(sb.toString());

                sender.sendMessage(Locale.TITLE + Locale.SKIN_DATA_DROPPED.toString().replace("%player", sb.toString()));
            } else if (args.length > 0 && args[0].equalsIgnoreCase("props")) {

                Player p = null;

                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Locale.TITLE.toString() + Locale.NOT_PLAYER);
                        return true;
                    }
                    p = (Player) sender;
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
                        sender.sendMessage(Locale.TITLE.toString() + Locale.NOT_ONLINE);
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
                        sender.sendMessage(Locale.TITLE.toString() + Locale.NO_SKIN_DATA);
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

                        String decoded = Base64Coder.decodeString(value);
                        cons.sendMessage("\n§aValue Decoded: §e" + decoded);

                        sender.sendMessage("\n§e" + decoded);
                        sender.sendMessage("§cMore info in console!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sender.sendMessage(Locale.TITLE.toString() + Locale.NO_SKIN_DATA);
                    return true;
                }
                sender.sendMessage("§cMore info in console!");

            }
        } else {
            sender.sendMessage(Locale.TITLE.toString() + Locale.PLAYER_HAS_NO_PERMISSION);
            return true;
        }
        return true;
    }
}
