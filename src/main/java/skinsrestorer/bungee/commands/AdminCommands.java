package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.bungee.storage.SkinStorage;
import skinsrestorer.bungee.utils.MojangAPI;
import skinsrestorer.bungee.utils.MojangAPI.SkinRequestException;

import java.io.File;
import java.io.IOException;

public class AdminCommands extends Command {

    public AdminCommands() {
        super("skinsrestorer", null, new String[]{"sr"});
    }

    @SuppressWarnings("deprecation")
    public void execute(final CommandSender sender, final String[] args) {

        if (sender.hasPermission("skinsrestorer.cmds")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                File file = new File(SkinsRestorer.getInstance().getDataFolder(), "config.yml");
                try {
                    ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sender.sendMessage(Locale.RELOAD);
            }

            else if (args.length == 2 && args[0].equalsIgnoreCase("drop")) {
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
                sb.append(args[2]);

                final String skin = sb.toString();
                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[1]);

                if (player == null)
                    for (ProxiedPlayer pl : ProxyServer.getInstance().getPlayers())
                        if (pl.getName().startsWith(args[1])) {
                            player = pl;
                            break;
                        }

                if (player == null) {
                    sender.sendMessage(Locale.NOT_ONLINE);
                    return;
                }

                final ProxiedPlayer p = player;

                SkinsRestorer.getInstance().getProxy().getScheduler().runAsync(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            MojangAPI.getUUID(skin);
                            SkinStorage.setPlayerSkin(p.getName(), skin);
                            SkinApplier.applySkin(p);
                            sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                            return;
                        } catch (SkinRequestException e) {
                            sender.sendMessage(e.getReason());
                            return;
                        }
                    }

                });

            } else if (args.length == 1 && args[0].equalsIgnoreCase("config")) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §2/sr config has been removed from SkinsRestorer. Farewell!");
                return;

            } else if (args.length > 0 && args[0].equalsIgnoreCase("props")) {

                ProxiedPlayer p = null;

                if (args.length == 1) {
                    if (!(sender instanceof ProxiedPlayer)) {
                        sender.sendMessage(Locale.NOT_PLAYER);
                        return;
                    }
                    p = (ProxiedPlayer) sender;
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

                /**
                 * There is no viable way to make this work without BungeeCord-Proxy.
                 * This will be removed from the file in a few releases

                 InitialHandler h = (InitialHandler) p.getPendingConnection();
                 Property prop = h.getLoginProfile().getProperties()[0];

                 if (prop == null) {
                 sender.sendMessage(Locale.NO_SKIN_DATA);
                 return;
                 }

                 CommandSender cons = ProxyServer.getInstance().getConsole();

                 cons.sendMessage("\n§aName: §8" + prop.getName());
                 cons.sendMessage("\n§aValue : §8" + prop.getValue());
                 cons.sendMessage("\n§aSignature : §8" + prop.getSignature());

                 String decoded = Base64Coder.decodeString(prop.getValue());
                 cons.sendMessage("\n§aValue Decoded: §e" + decoded);

                 sender.sendMessage("\n§e" + decoded);

                 sender.sendMessage("§cMore info in console!");
                 **/
            } else {
                if (!Locale.SR_LINE.isEmpty())
                    sender.sendMessage(Locale.SR_LINE);
                sender.sendMessage(Locale.HELP_ADMIN.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
                if (!Locale.SR_LINE.isEmpty())
                    sender.sendMessage(Locale.SR_LINE);
            }
        } else {
            sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
            return;

        }
    }
}
