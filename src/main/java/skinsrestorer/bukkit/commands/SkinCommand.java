package skinsrestorer.bukkit.commands;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SRLogger;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 24.01.2019.
 */

@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private SkinsRestorer plugin;
    private SRLogger log;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }


    @Default
    public void onDefault(CommandSender sender) {
        this.onHelp(sender, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    public void onSkinSetShort(Player p, @Single String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
    @Syntax(" [help]")
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
    @Syntax("%SyntaxSkinClearOther")
    public void onSkinClearOther(CommandSender sender, OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {

            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
                return;
            }

            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);

            // remove users defined skin from database
            plugin.getSkinStorage().removePlayerSkin(p.getName());

            if (this.setSkin(sender, p, skin, false, true)) {
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
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSender sender, OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            //Check cooldown first
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
                return;
            }

            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.contains(" ")) {
                        sender.sendMessage(Locale.ERROR_UPDATING_URL);
                        return;
                    }
                    // check if premium name
                    plugin.getMojangAPI().getUUIDMojang(skin); //TODO this might not be needed?
                    if (!plugin.getSkinStorage().forceUpdateSkinData(skin)) {
                        sender.sendMessage(Locale.ERROR_UPDATING_SKIN);
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);
                }
            } catch (SkinRequestException e) {
                // non premium = cancel
                sender.sendMessage(Locale.ERROR_UPDATING_CUSTOMSKIN);
                return;
            }

            if (this.setSkin(sender, p, skin, false, false)) {
                if (!sender.getName().equals(p.getName()))
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getName()));
                else
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN);
            }
        });
    }


    @Subcommand("set") @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player p, String[] skin) {
        if (skin.length > 0) {
            this.onSkinSetOther(p, new OnlinePlayer(p), skin[0]);
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
            }
    }

    @Subcommand("set") @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSender sender, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS) {
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

    @Subcommand("url") @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    public void onSkinSetUrl(Player p, String[] url) {
        if (url.length > 0) {
            if(C.validUrl(url[0])) {
                this.onSkinSetOther(p, new OnlinePlayer(p), url[0]);
            } else {
                p.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            }
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    private boolean setSkin(CommandSender sender, Player p, String skin) {
        return this.setSkin(sender, p, skin, true, false);
    }
    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSender sender, Player p, String skin, boolean save, boolean clear) {
        if (skin.equalsIgnoreCase("null") || !C.validUsername(skin) && !C.validUrl(skin)) {
            sender.sendMessage(Locale.INVALID_PLAYER.replace("%player", skin));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear)
            if (!sender.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        sender.sendMessage(Locale.SKIN_DISABLED);
                        return false;
                    }
            }

        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
            sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
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
                if (clear) {
                    Object props = SkinsRestorer.getInstance().getSkinStorage().createProperty("textures", "", "");
                    SkinsRestorer.getInstance().getFactory().applySkin(p, props);
                    SkinsRestorer.getInstance().getFactory().updateSkin(p);
                    return true;
                }

                sender.sendMessage(e.getReason());
            }
        }
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM && !clear) {
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
            } catch (Exception  e) {
                log.log("[ERROR] Exception: could not generate skin url:" + skin + "\nReason= "+ e.getMessage());
                sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            }
        }
        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        CooldownStorage.setCooldown(sender.getName(), Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        this.rollback(p, oldSkinName, save);
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
