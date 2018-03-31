package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;

import java.util.Arrays;
import java.util.Base64;

public class AdminCommands extends Command {

    public AdminCommands() {
        super("skinsrestorer", null, "sr");
    }

    @SuppressWarnings("deprecation")
    public void execute(final CommandSender sender, final String[] args) {

        if (sender.hasPermission("skinsrestorer.cmds")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                Locale.load();
                Config.load(SkinsRestorer.getInstance().getResourceAsStream("config.yml"));
                sender.sendMessage(new TextComponent(Locale.RELOAD));
            } else if (args.length == 2 && args[0].equalsIgnoreCase("drop")) {
                String nick = args[1];

                SkinStorage.removeSkinData(nick);
                sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", nick));

            } else if (args.length == 2 && args[0].equalsIgnoreCase("clear")) {
                // Todo: Add /sr clear <player> to admin help and implement that command in bukkit
                String nick = args[1];
                ProxiedPlayer player = ProxyServer.getInstance().getPlayer(nick);

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

                // Todo: Make sure to check if DefaultSkins are enabled and set the correct skin
                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    SkinStorage.removePlayerSkin(p.getName());
                    SkinStorage.setPlayerSkin(p.getName(), p.getName());
                    SkinApplier.applySkin(p);
                    p.sendMessage(new TextComponent(Locale.SKIN_CLEAR_SUCCESS));
                });

            } else if (args.length > 2 && args[0].equalsIgnoreCase("set")) {

                final String skin = args[2];
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

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    try {
                        MojangAPI.getUUID(skin);
                        SkinStorage.setPlayerSkin(p.getName(), skin);
                        SkinApplier.applySkin(p);
                        sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                    } catch (MojangAPI.SkinRequestException e) {
                        sender.sendMessage(e.getReason());
                    }
                });

            } else if (args.length == 1 && args[0].equalsIgnoreCase("config")) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §2/sr config has been removed from SkinsRestorer. Farewell!");

            } else if (args.length > 0 && args[0].equalsIgnoreCase("props")) {
                // sr props <player>
                ProxiedPlayer p;

                if (args.length == 1) {
                    if (!(sender instanceof ProxiedPlayer)) {
                        sender.sendMessage(Locale.NOT_PLAYER);
                    }
                }

                if (args.length == 2) {
                    String nick = args[1];

                    p = ProxyServer.getInstance().getPlayer(nick);
                    if (p == null) {
                        sender.sendMessage(Locale.NOT_ONLINE);
                        return;
                    }

                    InitialHandler h = (InitialHandler) p.getPendingConnection();
                    LoginResult.Property prop = h.getLoginProfile().getProperties()[0];

                    if (prop == null) {
                        sender.sendMessage(Locale.NO_SKIN_DATA);
                        return;
                    }

                    CommandSender cons = ProxyServer.getInstance().getConsole();

                    cons.sendMessage("\n§aName: §8" + prop.getName());
                    cons.sendMessage("\n§aValue : §8" + prop.getValue());
                    cons.sendMessage("\n§aSignature : §8" + prop.getSignature());

                    byte[] decoded = Base64.getDecoder().decode(prop.getValue());
                    cons.sendMessage("\n§aValue Decoded: §e" + Arrays.toString(decoded));

                    sender.sendMessage("\n§e" + Arrays.toString(decoded));

                    sender.sendMessage("§cMore info in console!");
                }
            } else {
                if (!Locale.SR_LINE.isEmpty())
                    sender.sendMessage(Locale.SR_LINE);
                sender.sendMessage(Locale.HELP_ADMIN.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
                if (!Locale.SR_LINE.isEmpty())
                    sender.sendMessage(Locale.SR_LINE);
            }
        } else {
            sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
        }
    }
}
