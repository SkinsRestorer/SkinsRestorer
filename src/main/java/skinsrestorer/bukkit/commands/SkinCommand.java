package skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 24.01.2019.
 */

@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }


    @Subcommand("clear") @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    public void onSkinClear(Player p) {
        this.onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear") @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSender sender, OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            Player p = target.getPlayer();
            String skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getName(), true);

            // remove users custom skin and set default skin / his skin
            SkinStorage.removePlayerSkin(p.getName());
            if (this.setSkin(sender, p, skin, false)) {
                if (!sender.getName().equals(target.getPlayer().getName()))
                    sender.sendMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", target.getPlayer().getName()));
                else
                    sender.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
            }
        });
    }


    @Subcommand("update") @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    public void onSkinUpdate(Player p) {
        this.onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update") @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    public void onSkinUpdateOther(CommandSender sender, OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            Player p = target.getPlayer();
            String skin = SkinStorage.getPlayerSkin(p.getName());

            // User has no custom skin set, get the default skin name / his skin
            if (skin == null)
                skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getName(), true);

            if (!SkinStorage.forceUpdateSkinData(skin)) {
                sender.sendMessage(Locale.ERROR_UPDATING_SKIN);
                return;
            }

            if (this.setSkin(sender, p, skin, false)) {
                if (!sender.getName().equals(target.getPlayer().getName()))
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", target.getPlayer().getName()));
                else
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN);
            }
        });
    }


    @Subcommand("set") @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    public void onSkinSet(Player p, String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @Subcommand("set") @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    public void onSkinSetOther(CommandSender sender, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS && Config.USE_NEW_PERMISSIONS) {
            if (!sender.hasPermission("skinsrestorer.skin." + skin)) {
                sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                return;
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            if (this.setSkin(sender, target.getPlayer(), skin)) {
                if (!sender.getName().equals(target.getPlayer().getName()))
                    sender.sendMessage(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getName()));
            }
        });
    }


    @CatchUnknown @CommandPermission("%skinSet")
    public void onDefault(Player p, String[] args) {
        this.onSkinSetOther(p, new OnlinePlayer(p), args[0]);
    }


    private boolean setSkin(CommandSender sender, Player p, String skin) {
        return this.setSkin(sender, p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSender sender, Player p, String skin, boolean save) {
        if (!C.validUsername(skin)) {
            sender.sendMessage(Locale.INVALID_PLAYER.replace("%player", skin));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!sender.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        sender.sendMessage(Locale.SKIN_DISABLED);
                        return false;
                    }
            }

        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
            sender.sendMessage(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
            return false;
        }

        CooldownStorage.resetCooldown(sender.getName());
        CooldownStorage.setCooldown(sender.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = SkinStorage.getPlayerSkin(p.getName());
        try {
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), skin);
            SkinsRestorer.getInstance().getFactory().applySkin(p, SkinStorage.getOrCreateSkinForPlayer(skin));
            p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
            return true;
        } catch (MojangAPI.SkinRequestException e) {
            sender.sendMessage(e.getReason());

            // set custom skin name back to old one if there is an exception
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
        }
        return false;
    }
}