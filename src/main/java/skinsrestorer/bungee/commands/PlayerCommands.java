package skinsrestorer.bungee.commands;

import javafx.scene.control.Skin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
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
    private void help(ProxiedPlayer p) {
        if (p.hasPermission("skinsrestorer.playercmds") || Config.SKINWITHOUTPERM) {
            if (!Locale.SR_LINE.isEmpty())
                p.sendMessage(new TextComponent(Locale.SR_LINE));
            p.sendMessage(new TextComponent(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion())));
            if (p.hasPermission("skinsrestorer.cmds"))
                p.sendMessage(new TextComponent(Locale.HELP_SR));
            if (!Locale.SR_LINE.isEmpty())
                p.sendMessage(new TextComponent(Locale.SR_LINE));
        } else {
            p.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
        }
    }

    public void execute(CommandSender sender, final String[] args) {

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(Locale.NOT_PLAYER));
            return;
        }

        final ProxiedPlayer p = (ProxiedPlayer) sender;

        // Skin Help
        if (args.length == 0 || args.length > 2) {
            help(p);
        }

        //Skin Clear and Skin (name)
        if (args.length == 1) {
            if (!p.hasPermission("skinsrestorer.playercmds") && !Config.SKINWITHOUTPERM) {
                p.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                return;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                CooldownStorage.resetCooldown(p.getName());
                CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

                // Todo: Make sure to check if DefaultSkins are enabled and set the correct skin
                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    SkinStorage.removePlayerSkin(p.getName());
                    SkinStorage.setPlayerSkin(p.getName(), p.getName());
                    SkinApplier.applySkin(p);
                    p.sendMessage(new TextComponent(Locale.SKIN_CLEAR_SUCCESS));
                });
            } else {

                //skin <skin>
                final String skin = args[0];

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

                String oldSkinName = SkinStorage.getPlayerSkin(p.getName());
                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    //try {
                        // MojangAPI.getUUID(skin);  WTF is this <.<
                        SkinStorage.setPlayerSkin(p.getName(), skin);
                        SkinApplier.applySkin(p);
                        p.sendMessage(new TextComponent(Locale.SKIN_CHANGE_SUCCESS));
                    //} catch (SkinRequestException e) {
                    //    p.sendMessage(new TextComponent(e.getReason()));
                    //}
                });
                return;
            }
        }

        //skin set
        if (args.length == 2) {
            if (!p.hasPermission("skinsrestorer.playercmds") && !Config.SKINWITHOUTPERM) {
                p.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                return;
            }

            if (args[0].equalsIgnoreCase("set")) {

                final String skin = args[1];

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
                    } catch (SkinRequestException e) {
                        p.sendMessage(new TextComponent(e.getReason()));
                    }
                });
            } else {
                help(p);
            }
        }
    }
}
