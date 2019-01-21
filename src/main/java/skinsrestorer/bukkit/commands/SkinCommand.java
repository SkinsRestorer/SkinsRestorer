package skinsrestorer.bukkit.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.util.concurrent.TimeUnit;

public class SkinCommand implements CommandExecutor {

    private boolean checkPerm(Player p, String perm) {
        return p.hasPermission(perm) || Config.SKINWITHOUTPERM;
    }

    private void sendHelp(Player p) {
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(Locale.SR_LINE);
        p.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
        if (p.hasPermission("skinsrestorer.cmds"))
            p.sendMessage(Locale.HELP_SR);
        if (!Locale.SR_LINE.isEmpty())
            p.sendMessage(Locale.SR_LINE);
    }

    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Locale.NOT_PLAYER);
            return true;
        }

        final Player p = (Player) sender;

        if (args.length < 1) {
            this.sendHelp(p);
            return true;
        }

        String cmd = args[0].toLowerCase();

        switch (cmd) {
            case "clear": {
                if (!this.checkPerm(p, "skinsrestorer.playercmds")) {
                    p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                String skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getName(), true);

                // If skin change was successfull, remove custom skin from storage
                if (this.setSkin(p, skin, false)) {
                    SkinStorage.removePlayerSkin(p.getName());

                    p.sendMessage(Locale.SKIN_CLEAR_SUCCESS);

                    return true;
                }
            }

            case "set": {
                if (args.length < 2) {
                    if (this.checkPerm(p, "skinsrestorer.playercmds"))
                        this.sendHelp(p);
                    else
                        p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                if (!this.checkPerm(p, "skinsrestorer.playercmds")) {
                    p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                final String skin = args[1];
                this.setSkin(p, skin);
                return true;
            }

            // Skin <name>
            default: {
                if (!this.checkPerm(p, "skinsrestorer.playercmds")) {
                    p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                    return true;
                }

                final String skin = args[0];
                this.setSkin(p, skin);
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
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
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
        });
        return true;
    }
}
