package skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;
import skinsrestorer.sponge.SkinsRestorer;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 27.02.2019.
 */
@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private SkinsRestorer plugin;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onDefault(CommandSource source) {
        this.onHelp(source, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    public void onSkinSetShort(Player p, @Single String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSource source, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(source);
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
    public void onSkinClearOther(CommandSource source, OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);

            // remove users custom skin and set default skin / his skin
            plugin.getSkinStorage().removePlayerSkin(p.getName());
            if (this.setSkin(source, p, skin, false)) {
                if (!source.getName().equals(target.getPlayer().getName()))
                    source.sendMessage(plugin.parseMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", target.getPlayer().getName())));
                else
                    source.sendMessage(plugin.parseMessage(Locale.SKIN_CLEAR_SUCCESS));
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
    public void onSkinUpdateOther(CommandSource source, OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            //Check cooldown first
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(source.getName())) {
                source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(source.getName()))));
                return;
            }

            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.contains(" ")) {
                        source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_URL));
                        return;
                    }
                    // check if premium name
                    plugin.getMojangAPI().getUUIDMojang(skin);
                    if (!plugin.getSkinStorage().forceUpdateSkinData(skin)) {
                        source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);
                }
            } catch (SkinRequestException e) {
                // non premium = cancel
                source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_CUSTOMSKIN));
                return;
            }

            if (this.setSkin(source, p, skin, false)) {
                if (!source.getName().equals(p.getName()))
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getName())));
                else
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN));
            }
        });
    }

    @Subcommand("set") @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player p, String[] skin) {
        if(skin.length > 0){
            this.onSkinSetOther(p, new OnlinePlayer(p), skin[0]);
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    @Subcommand("set") @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS) {
            if (!source.hasPermission("skinsrestorer.skin." + skin)) {
                if (!source.getName().equals(target.getPlayer().getName()) || (!source.hasPermission("skinsrestorer.ownskin") && !skin.equalsIgnoreCase(source.getName()))) {
                    source.sendMessage(plugin.parseMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }
        }

        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            if (this.setSkin(source, target.getPlayer(), skin)) {
                if (!source.getName().equals(target.getPlayer().getName()))
                    source.sendMessage(plugin.parseMessage(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getName())));
            }
        });
    }

    @Subcommand("url") @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    public void onSkinSetUrl(Player p, String[] url) {
        if (url.length > 0) {
            if (C.validUrl(String.valueOf(url))) {
                this.onSkinSetOther(p, new OnlinePlayer(p), String.valueOf(url));
            } else {
                p.sendMessage(plugin.parseMessage(Locale.ERROR_INVALID_URLSKIN));
            }
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    private boolean setSkin(CommandSource source, Player p, String skin) {
        return this.setSkin(source, p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player p, String skin, boolean save) {
        if (!C.validUsername(skin) && !C.validUrl(skin)) {
            source.sendMessage(plugin.parseMessage(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!source.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        source.sendMessage(plugin.parseMessage(Locale.SKIN_DISABLED));
                        return false;
                    }
            }

        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(source.getName())) {
            source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(source.getName()))));
            return false;
        }

        CooldownStorage.resetCooldown(source.getName());
        CooldownStorage.setCooldown(source.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = plugin.getSkinStorage().getPlayerSkin(p.getName());

        if (C.validUsername(skin)) {
            try {
                if (save)
                    plugin.getSkinStorage().setPlayerSkin(p.getName(), skin);
                plugin.getSkinApplier().applySkin(p, skin);
                p.sendMessage(plugin.parseMessage(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getReason()));
                // set custom skin name back to old one if there is an exception
                if (save)
                    plugin.getSkinStorage().setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
            }
            return false;
        }
        if (C.validUrl(skin)) {
            if (!source.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM) {
                source.sendMessage(plugin.parseMessage(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                return false;
            }

            try {
                source.sendMessage(plugin.parseMessage(Locale.MS_UPDATING_SKIN));
                String skinentry = " "+p.getName(); // so won't overwrite premium playernames
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(p.getName(), skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinApplier().applySkin(p, skinentry);
                p.sendMessage(plugin.parseMessage(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getReason()));
                // set custom skin name back to old one if there is an exception
                plugin.getSkinStorage().setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
            }
            return false;
        }
        return false;
    }

    private void sendHelp(CommandSource source) {
        if (!Locale.SR_LINE.isEmpty())
            source.sendMessage(plugin.parseMessage(Locale.SR_LINE));
        source.sendMessage(plugin.parseMessage(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            source.sendMessage(plugin.parseMessage(Locale.SR_LINE));
    }
}
