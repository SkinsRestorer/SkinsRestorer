package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.util.concurrent.TimeUnit;

public class PlayerCommands extends Command {

    public PlayerCommands() {
        super("skin", null);
    }

    //Method called for the commands help.
    public void help(ProxiedPlayer p) {
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(new TextComponent(Locale.SR_LINE));
        p.sendMessage(new TextComponent(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion())));
        if (p.hasPermission("skinsrestorer.cmds"))
            p.sendMessage(new TextComponent(Locale.HELP_SR));
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(new TextComponent(Locale.SR_LINE));
    }

    public void execute(CommandSender sender, final String[] args) {

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(Locale.NOT_PLAYER));
            return;
        }

        final ProxiedPlayer p = (ProxiedPlayer) sender;

        // Skin Help
        if (args.length == 0 || args.length > 2) {
            if (!Config.SKINWITHOUTPERM) {
                if (p.hasPermission("skinsrestorer.playercmds")) {
                    help(p);
                } else {
                    p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                }
            } else {
                help(p);
            }
        }

        //Skin Clear and Skin (name)
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("clear")) {
                CooldownStorage.resetCooldown(p.getName());
                CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    try {
                        SkinStorage.removePlayerSkin(p.getName());
                        SkinStorage.setPlayerSkin(p.getName(), p.getName());
                        SkinApplier.applySkin(p);
                        p.sendMessage(new TextComponent(Locale.SKIN_CLEAR_SUCCESS));
                        return;
                    } catch (Exception e) {
                        return;
                    }
                });
            }
            else {
                StringBuilder sb = new StringBuilder();
                sb.append(args[0]);

                //skin <skin>
                final String skin = sb.toString();

                if (Config.DISABLED_SKINS_ENABLED)
                    if (!p.hasPermission("skinsrestorer.bypassdisabled")) {
                        for (String dskin : Config.DISABLED_SKINS)
                            if (skin.equalsIgnoreCase(dskin)) {
                                p.sendMessage(new TextComponent(Locale.SKIN_DISABLED));
                                return;
                            }
                    }

                if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
                    p.sendMessage(new TextComponent(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName()))));
                    return;
                }
                CooldownStorage.resetCooldown(p.getName());
                CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    try {
                        MojangAPI.getUUID(skin);
                        SkinStorage.setPlayerSkin(p.getName(), skin);
                        SkinApplier.applySkin(p);
                        p.sendMessage(new TextComponent(Locale.SKIN_CHANGE_SUCCESS));
                        return;
                    } catch (SkinRequestException e) {
                        p.sendMessage(new TextComponent(e.getReason()));
                        return;
                    }
                });
                return;
            }
        }

        //skin set
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {

                StringBuilder sb = new StringBuilder();
                sb.append(args[1]);

                final String skin = sb.toString();

                if (Config.DISABLED_SKINS_ENABLED)
                    if (!p.hasPermission("skinsrestorer.bypassdisabled")) {
                        for (String dskin : Config.DISABLED_SKINS)
                            if (skin.equalsIgnoreCase(dskin)) {
                                p.sendMessage(new TextComponent(Locale.SKIN_DISABLED));
                                return;
                            }
                    }

                if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
                    p.sendMessage(new TextComponent(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName()))));
                    return;
                }
                CooldownStorage.resetCooldown(p.getName());
                CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    try {
                        MojangAPI.getUUID(skin);
                        SkinStorage.setPlayerSkin(p.getName(), skin);
                        SkinApplier.applySkin(p);
                        p.sendMessage(new TextComponent(Locale.SKIN_CHANGE_SUCCESS));
                        return;
                    } catch (SkinRequestException e) {
                        p.sendMessage(new TextComponent(e.getReason()));
                        return;
                    }
                });
                return;
            } else {
                help(p);
                return;
            }
        }
    }
}
