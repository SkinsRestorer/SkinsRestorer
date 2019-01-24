package skinsrestorer.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.bukkit.utils.PlayerArgs;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.util.concurrent.TimeUnit;

public class SkinCommand implements CommandExecutor {

    private boolean checkPerm(CommandSender p, String perm) {
        return p.hasPermission(perm) || Config.SKINWITHOUTPERM;
    }

    private void sendHelp(CommandSender p) {
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(Locale.SR_LINE);
        p.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
        if (p.hasPermission("skinsrestorer.cmds"))
            p.sendMessage(Locale.HELP_SR);
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(Locale.SR_LINE);
    }

    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
        if (args.length < 1) {
            this.sendHelp(sender);
            return true;
        }

        String cmd = args[0].toLowerCase();

        switch (cmd) {
            case "clear": {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(Locale.NOT_ONLINE);
                    return true;
                }

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.clear.other")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    Player pl = pArgs.getPlayer();
                    String skin = SkinStorage.getDefaultSkinNameIfEnabled(pl.getName(), true);

                    // remove users custom skin and set default skin / his skin
                    SkinStorage.removePlayerSkin(pl.getName());
                    if (this.setSkin(pl, skin, false)) {
                        pl.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
                    }
                });
                return true;
            }

            case "update": {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds.update")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(Locale.NOT_ONLINE);
                    return false;
                }

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.update.other")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return false;
                }

                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    Player pl = pArgs.getPlayer();
                    String skin = SkinStorage.getPlayerSkin(pl.getName());

                    // User has no custom skin set, get the default skin name / his skin
                    if (skin == null)
                        skin = SkinStorage.getDefaultSkinNameIfEnabled(pl.getName(), true);

                    if (!SkinStorage.forceUpdateSkinData(skin)) {
                        pl.sendMessage(Locale.ERROR_UPDATING_SKIN);
                        return;
                    }

                    if (this.setSkin(pl, skin, false)) {
                        pl.sendMessage(Locale.SUCCESS_UPDATING_SKIN);
                    }
                });
                return true;
            }

            case "set": {
                if (args.length < 2) {
                    if (this.checkPerm(sender, "skinsrestorer.playercmds"))
                        this.sendHelp(sender);
                    else
                        sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                if (!this.checkPerm(sender, "skinsrestorer.playercmds")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args, 2);

                if (args.length > 2 && !pArgs.foundPlayer()) {
                    sender.sendMessage(Locale.NOT_ONLINE);
                    return true;
                }

                String skin = args[1];

                if (pArgs.isOtherPlayer() && !sender.hasPermission("skinsrestorer.playercmds.other")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                if (pArgs.isOtherPlayer())
                    skin = args[2];

                final String finalSkin = skin;
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    this.setSkin(pArgs.getPlayer(), finalSkin);
                });
                return true;
            }

            // Skin <name>
            default: {
                if (!this.checkPerm(sender, "skinsrestorer.playercmds")) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                if (args.length > 1) {
                    this.sendHelp(sender);
                    return true;
                }

                final PlayerArgs pArgs = new PlayerArgs(sender, args);

                if (!pArgs.foundPlayer()) {
                    sender.sendMessage(Locale.NOT_PLAYER);
                    return true;
                }

                final String skin = args[0];
                Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
                    this.setSkin(pArgs.getPlayer(), skin);
                });
                return true;
            }
        }
    }

    private void setSkin(Player p, String skin) {
        this.setSkin(p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(Player p, String skin, boolean save) {
        if (!C.validUsername(skin)) {
            p.sendMessage(Locale.INVALID_PLAYER.replace("%player", skin));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!p.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        p.sendMessage(Locale.SKIN_DISABLED);
                        return false;
                    }
            }

        if (!p.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(p.getName())) {
            p.sendMessage(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName())));
            return false;
        }

        CooldownStorage.resetCooldown(p.getName());
        CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = SkinStorage.getPlayerSkin(p.getName());
        try {
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), skin);
            SkinsRestorer.getInstance().getFactory().applySkin(p, SkinStorage.getOrCreateSkinForPlayer(skin));
            p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
        } catch (SkinRequestException e) {
            p.sendMessage(e.getReason());

            // set custom skin name back to old one if there is an exception
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
        }
        return true;
    }
}
