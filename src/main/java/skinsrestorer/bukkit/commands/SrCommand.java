package skinsrestorer.bukkit.commands;

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

import java.util.Collection;
import java.util.List;

public class SrCommand implements CommandExecutor {

    public boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(final CommandSender sender, Command arg1, String arg2, String[] args) {

        if (sender.hasPermission("skinsrestorer.cmds")) {


            if (args.length == 0) {
                sender.sendMessage(Locale.SR_LINE);
                sender.sendMessage(Locale.HELP_ADMIN.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
                sender.sendMessage(Locale.SR_LINE);
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
                    sender.sendMessage(Locale.NOT_ONLINE);
                    return true;
                }

                final Player p = player;

                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

                    @Override
                    public void run() {


                        try {
                            MojangAPI.getUUID(skin);


                            SkinStorage.setPlayerSkin(p.getName(), skin);
                            SkinsRestorer.getInstance().getFactory().applySkin(p,
                                    SkinStorage.getOrCreateSkinForPlayer(p.getName()));
                            sender.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                            return;
                        } catch (SkinRequestException e) {
                            sender.sendMessage(e.getReason());
                            return;
                        }
                    }

                });
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload"))
                reloadConfig(sender, Locale.RELOAD);
            else if (args.length == 1 && args[0].equalsIgnoreCase("config"))
                sender.sendMessage(Locale.HELP_CONFIG);
            else if (args.length >= 2 && args[0].equalsIgnoreCase("defaultSkins")) {
                if (args[1].equalsIgnoreCase("true")) {
                    Config.DEFAULT_SKINS_ENABLED = true;
                    Config.set("DefaultSkins.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&2Default skins has been enabled.");
                } else if (args[1].equalsIgnoreCase("false")) {
                    Config.DEFAULT_SKINS_ENABLED = false;
                    Config.set("DefaultSkins.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&4Default skins has been disabled.");
                } else if (args[1].equalsIgnoreCase("add")) {
                    String skin = args[2];
                    List<String> skins = Config.DEFAULT_SKINS;
                    skins.add(skin);
                    Config.set("DefaultSkins.Names", skins);
                    reloadConfig(sender, "&2Added &f" + skin + " &2to the default skins list");
                }

            } else if (args.length >= 2 && args[0].equalsIgnoreCase("disabledSkins")) {
                if (args[1].equalsIgnoreCase("true")) {
                    Config.DISABLED_SKINS_ENABLED = true;
                    Config.set("DisabledSkins.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&2Disabled skins has been enabled.");
                } else if (args[1].equalsIgnoreCase("false")) {
                    Config.DISABLED_SKINS_ENABLED = false;
                    Config.set("DisabledSkins.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&4Disabled skins has been disabled.");
                } else if (args[1].equalsIgnoreCase("add")) {
                    String skin = args[2];
                    List<String> skins = Config.DISABLED_SKINS;
                    skins.add(skin);
                    Config.set("DisabledSkins.Names", skins);
                    reloadConfig(sender, "&2Added &f" + skin + " &2to the disabled skins list");
                }

            } else if (args.length == 2 && args[0].equalsIgnoreCase("joinSkins")) {
                if (args[1].equalsIgnoreCase("true")) {
                    Config.DISABLE_ONJOIN_SKINS = true;
                    Config.set("DisableOnJoinSkins", String.valueOf(args[1]));
                    reloadConfig(sender, "&4Players will not have skins on join.");
                } else if (args[1].equalsIgnoreCase("false")) {
                    Config.DISABLE_ONJOIN_SKINS = false;
                    Config.set("DisableOnJoinSkins", String.valueOf(args[1]));
                    ;
                    reloadConfig(sender, "&2Players will have skins on join.");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("updater")) {
                if (args[1].equalsIgnoreCase("true")) {
                    Config.UPDATER_ENABLED = true;
                    Config.set("Updater.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&2The updater has been enabled.");
                } else if (args[1].equalsIgnoreCase("false")) {
                    Config.UPDATER_ENABLED = false;
                    Config.set("Updater.Enabled", String.valueOf(args[1]));
                    reloadConfig(sender, "&4The updater has been disabled.");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("skinwithoutperm")) {
                if (args[1].equalsIgnoreCase("true")) {
                    Config.SKINWITHOUTPERM = true;
                    Config.set("SkinWithoutPerm", String.valueOf(args[1]));
                    reloadConfig(sender, "&2Skins will not require permissions.");
                } else if (args[1].equalsIgnoreCase("false")) {
                    Config.SKINWITHOUTPERM = false;
                    Config.set("SkinWithoutPerm", String.valueOf(args[1]));
                    reloadConfig(sender, "&2Skins will require permissions.");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("skinCooldown")) {
                if (isStringInt(args[1])) {
                    Config.SKIN_CHANGE_COOLDOWN = Integer.valueOf(args[1]);
                    Config.set("SkinChangeCooldown", Integer.valueOf(args[1]));
                    reloadConfig(sender, "&2The skin change cooldown has been set to &f" + Integer.valueOf(args[1]) + "&seconds(s)");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("SkinExpiresAfter")) {
                if (isStringInt(args[1])) {
                    Config.SKIN_EXPIRES_AFTER = Integer.valueOf(args[1]);
                    Config.set("SkinExpiresAfter", Integer.valueOf(args[1]));
                    reloadConfig(sender, "&2The skin cache time is now &f" + Integer.valueOf(args[1]) + "&2minute(s)");
                }

            } else if (args.length == 1 && args[0].equalsIgnoreCase("status"))
                try {
                    MojangAPI.getSkinProperty(MojangAPI.getUUID("Notch"));
                    sender.sendMessage(Locale.STATUS_OK);
                } catch (SkinRequestException e) {
                    sender.sendMessage(e.getReason());
                }

            else if (args.length > 1 && args[0].equalsIgnoreCase("drop")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++)
                    sb.append(args[i]);

                SkinStorage.removeSkinData(sb.toString());

                sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", sb.toString()));
            } else if (args.length > 0 && args[0].equalsIgnoreCase("props")) {

                Player p = null;

                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(Locale.NOT_PLAYER);
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

                        String decoded = Base64Coder.decodeString(value);
                        cons.sendMessage("\n§aValue Decoded: §e" + decoded);

                        sender.sendMessage("\n§e" + decoded);
                        sender.sendMessage("§cMore info in console!");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sender.sendMessage(Locale.NO_SKIN_DATA);
                    return true;
                }
                sender.sendMessage("§cMore info in console!");

            }
        } else {
            sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
            return true;

        }
        return true;
    }

    public void reloadConfig(CommandSender sender, String msg) {
        Locale.load();
        Config.load(SkinsRestorer.getInstance().getResource("config.yml"));
        sender.sendMessage(C.c(msg));
    }
}
