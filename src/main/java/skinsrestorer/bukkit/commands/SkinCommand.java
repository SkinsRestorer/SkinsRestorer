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
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

import java.util.concurrent.TimeUnit;

public class SkinCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Locale.NOT_PLAYER);
            return true;
        }

        final Player p = (Player) sender;

        if (!Config.SKINWITHOUTPERM) {
            if (p.hasPermission("skinsrestorer.playercmds") || p.isOp()) {
            } else {
                p.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION);
                return true;
            }
        }
        // Skin Clear
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            Object props = null;

            if (SkinStorage.getPlayerSkin(p.getName()).equalsIgnoreCase(p.getName())) {
                p.sendMessage(Locale.NO_SKIN_DATA);
                return true;
            }

            SkinStorage.removePlayerSkin(p.getName());
            props = SkinStorage.createProperty("textures", "", "");
            SkinsRestorer.getInstance().getFactory().applySkin(p, props);
            SkinsRestorer.getInstance().getFactory().updateSkin(p);
            p.sendMessage(Locale.SKIN_CLEAR_SUCCESS);

            return true;
        }

        // Skin Help
        if (args.length == 0 || args.length > 1) {
            p.sendMessage(Locale.SR_LINE);
            p.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
            if (p.hasPermission("skinsrestorer.cmds") || p.isOp())
                p.sendMessage(C.c("    &2/sr &7- &fDisplay Admin commands."));
            p.sendMessage(Locale.SR_LINE);
        }

        // Set Skin
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++)
                if (args.length == 1)
                    sb.append(args[i]);
                else if (args.length > 1)
                    if (i + 1 == args.length)
                        sb.append(args[i]);
                    else
                        sb.append(args[i] + " ");

            final String skin = sb.toString();

            if (Config.DISABLED_SKINS_ENABLED)
                if (!p.hasPermission("skinsrestorer.bypassdisabled") && !p.isOp()) {
                    for (String dskin : Config.DISABLED_SKINS)
                        if (skin.equalsIgnoreCase(dskin)) {
                            p.sendMessage(Locale.SKIN_DISABLED);
                            return true;
                        }
                }

            if (p.hasPermission("skinsrestorer.bypasscooldown") || p.isOp()) {

            } else {
                if (CooldownStorage.hasCooldown(p.getName())) {
                    p.sendMessage(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(p.getName())));
                    return true;
                }
            }

            CooldownStorage.resetCooldown(p.getName());
            CooldownStorage.setCooldown(p.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

            Bukkit.getScheduler().runTask(SkinsRestorer.getInstance(), new Runnable() {

                @Override
                public void run() {

                    try {
                        MojangAPI.getUUID(skin);


                        SkinStorage.setPlayerSkin(p.getName(), skin);
                        SkinsRestorer.getInstance().getFactory().applySkin(p,
                                SkinStorage.getOrCreateSkinForPlayer(p.getName()));
                        p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                        return;
                    } catch (SkinRequestException e) {
                        p.sendMessage(e.getReason());
                        return;
                    }
                }

            });
            return true;
        }
        return true;
    }

}
