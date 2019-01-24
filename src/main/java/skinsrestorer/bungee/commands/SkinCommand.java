package skinsrestorer.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.bungee.utils.PlayerArgs;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.util.concurrent.TimeUnit;

public class SkinCommand extends Command {

    public SkinCommand() {
        super("skin", null);
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        return sender.hasPermission(perm) || Config.SKINWITHOUTPERM;
    }

    //Method called for the commands help.
    private void help(CommandSender p) {
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
        // Skin Help
        if (args.length < 1) {
            this.help(sender);
            return;
        }

        String cmd = args[0].toLowerCase();

        switch (cmd) {
            case "clear": {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds.clear")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(new TextComponent(Locale.NOT_ONLINE));
                    return;
                }

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.clear.other")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    ProxiedPlayer pl = pArgs.getPlayer();
                    String skin = SkinStorage.getDefaultSkinNameIfEnabled(pl.getName(), true);

                    // remove users custom skin and set default skin / his skin
                    SkinStorage.removePlayerSkin(pl.getName());
                    if (this.setSkin(pl, skin, false)) {
                        pl.sendMessage(new TextComponent(Locale.SKIN_CLEAR_SUCCESS));
                    }
                });
                return;
            }

            case "update": {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds.update")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(new TextComponent(Locale.NOT_ONLINE));
                    return;
                }

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.update.other")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    ProxiedPlayer pl = pArgs.getPlayer();
                    String skin = SkinStorage.getPlayerSkin(pl.getName());

                    // User has no custom skin set, get the default skin name / his skin
                    if (skin == null)
                        skin = SkinStorage.getDefaultSkinNameIfEnabled(pl.getName(), true);

                    if (!SkinStorage.forceUpdateSkinData(skin)) {
                        pl.sendMessage(new TextComponent(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                    if (this.setSkin(pl, skin, false)) {
                        pl.sendMessage(new TextComponent(Locale.SUCCESS_UPDATING_SKIN));
                    }
                });

                return;
            }

            case "set": {
                if (args.length < 2) {
                    if (this.checkPerm(sender, "skinsrestorer.playercmds"))
                        this.help(sender);
                    else
                        sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                if (!this.checkPerm(sender, "skinsrestorer.playercmds")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args, 2);

                if (args.length > 2 && !pArgs.foundPlayer()) {
                    sender.sendMessage(new TextComponent(Locale.NOT_ONLINE));
                    return;
                }

                String skin = args[1];

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.other")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                if (pArgs.isOtherPlayer())
                    skin = args[2];

                final String finalSkin = skin;

                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    this.setSkin(pArgs.getPlayer(), finalSkin);
                });
                return;
            }

            // Skin <name>
            default: {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds")) {
                    sender.sendMessage(new TextComponent(Locale.PLAYER_HAS_NO_PERMISSION));
                    return;
                }

                if (args.length > 1) {
                    this.help(sender);
                    return;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(new TextComponent(Locale.NOT_PLAYER));
                    return;
                }

                final String skin = args[0];
                ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
                    this.setSkin(pArgs.getPlayer(), skin);
                });
                return;
            }
        }
    }

    private void setSkin(ProxiedPlayer p, String skin) {
        this.setSkin(p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(ProxiedPlayer p, String skin, boolean save) {
        if (!C.validUsername(skin)) {
            p.sendMessage(new TextComponent(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!p.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        p.sendMessage(new TextComponent(Locale.SKIN_DISABLED));
                        return false;
                    }
            }

        if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
            p.sendMessage(new TextComponent(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName()))));
            return false;
        }

        CooldownStorage.resetCooldown(p.getName());
        CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = SkinStorage.getPlayerSkin(p.getName());
        try {
            MojangAPI.getUUID(skin);
            if (save) {
                SkinStorage.setPlayerSkin(p.getName(), skin);
                SkinApplier.applySkin(p);
            } else {
                SkinApplier.applySkin(p, skin, null);
            }
            p.sendMessage(new TextComponent(Locale.SKIN_CHANGE_SUCCESS));
            return true;
        } catch (SkinRequestException e) {
            p.sendMessage(new TextComponent(e.getReason()));

            // set custom skin name back to old one if there is an exception
            this.rollback(p, oldSkinName, save);
        } catch (Exception e) {
            e.printStackTrace();

            // set custom skin name back to old one if there is an exception
            this.rollback(p, oldSkinName, save);
        }
        return false;
    }

    private void rollback(ProxiedPlayer p, String oldSkinName, boolean save) {
        if (save)
            SkinStorage.setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
    }
}
