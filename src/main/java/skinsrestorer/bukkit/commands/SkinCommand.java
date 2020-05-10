package skinsrestorer.bukkit.commands;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.interfaces.ISkinCommand;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 24.01.2019.
 */

@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private SkinsRestorer plugin;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }


    @Default
    public void onDefault(CommandSender sender) {
        this.onHelp(sender, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("<skin/url>")
    public void onSkinSetShort(Player p, @Single String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
    public void onHelp(CommandSender sender, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(sender);
        else
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
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);

            // remove users custom skin and set default skin / his skin
            plugin.getSkinStorage().removePlayerSkin(p.getName());
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
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            // User has no custom skin set, get the default skin name / his skin
            if (skin == null)
                skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);

            if (skin.contains(" ")) {
                sender.sendMessage(Locale.ERROR_UPDATING_URL);
                return;
            }

            if (!plugin.getSkinStorage().forceUpdateSkinData(skin)) {
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
    @Syntax("<skin/url>")
    public void onSkinSet(Player p, String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @Subcommand("set") @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("<target> <skin/url>")
    public void onSkinSetOther(CommandSender sender, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS && Config.USE_NEW_PERMISSIONS) {
            if (!sender.hasPermission("skinsrestorer.skin." + skin)) {
                if (!sender.getName().equals(target.getPlayer().getName()) || (!sender.hasPermission("skinsrestorer.ownskin") && !skin.equalsIgnoreCase(sender.getName()))) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            if (this.setSkin(sender, target.getPlayer(), skin)) {
                if (!sender.getName().equals(target.getPlayer().getName()))
                    sender.sendMessage(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getName()));
            }
        });
    }

    private boolean setSkin(CommandSender sender, Player p, String skin) {
        return this.setSkin(sender, p, skin, true);
    }
    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSender sender, Player p, String skin, boolean save) {
        if (skin.equalsIgnoreCase("null") || !C.validUsername(skin) && !C.validUrl(skin)) {
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


        String oldSkinName = plugin.getSkinStorage().getPlayerSkin(p.getName());
        if (C.validUsername(skin)) {
            try {
                if (save)
                    plugin.getSkinStorage().setPlayerSkin(p.getName(), skin);
                SkinsRestorer.getInstance().getFactory().applySkin(p, plugin.getSkinStorage().getOrCreateSkinForPlayer(skin));
                p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getReason());
                CooldownStorage.setCooldown(sender.getName(), Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
                this.rollback(p, oldSkinName, save); // set custom skin name back to old one if there is an exception
            }
            return false;
        }
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM) {
                sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_URL);
                CooldownStorage.resetCooldown(sender.getName());
                return false;
            }

            try {
                sender.sendMessage(Locale.MS_UPDATING_SKIN);
                String skinentry = " " + p.getName(); // so won't overwrite premium playernames
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(p.getName(), skinentry); // set player to "whitespaced" name then reload skin
                SkinsRestorer.getInstance().getFactory().applySkin(p, plugin.getSkinStorage().getSkinData(skinentry));
                p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getReason());
                CooldownStorage.setCooldown(sender.getName(), Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
                this.rollback(p, oldSkinName, save); // set custom skin name back to old one if there is an exception
            } catch (Exception  e) {
                System.out.println("[SkinsRestorer] [ERROR] could not generate skin url:" + skin);
                //e.printStackTrace(); //todo: not throw error without context
                sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
                CooldownStorage.setCooldown(sender.getName(), Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
                this.rollback(p, oldSkinName, save); // set custom skin name back to old one if there is an exception
            }
            return false;
        }
        return false;
    }
        private void rollback(Player p, String oldSkinName, boolean save) {
            if (save)
            plugin.getSkinStorage().setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
        }

    private void sendHelp(CommandSender sender) {
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);
        sender.sendMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion()));
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);
    }
}
