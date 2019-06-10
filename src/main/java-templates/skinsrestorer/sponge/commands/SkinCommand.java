package skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Scheduler;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.sponge.SkinsRestorer;
import skinsrestorer.sponge.utils.SkinApplier;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 27.02.2019.
 */
@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private SkinsRestorer plugin;

    @Default
    public void onDefault(CommandSource source) {
        this.onHelp(source, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    public void onSkinSetShort(Player p, @Single String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
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
            String skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getName(), true);

            // remove users custom skin and set default skin / his skin
            SkinStorage.removePlayerSkin(p.getName());
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
    public void onSkinUpdateOther(CommandSource source, OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            Player p = target.getPlayer();
            String skin = SkinStorage.getPlayerSkin(p.getName());

            // User has no custom skin set, get the default skin name / his skin
            if (skin == null)
                skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getName(), true);

            if (!SkinStorage.forceUpdateSkinData(skin)) {
                source.sendMessage(plugin.parseMessage(Locale.ERROR_UPDATING_SKIN));
                return;
            }

            if (this.setSkin(source, p, skin, false)) {
                if (!source.getName().equals(target.getPlayer().getName()))
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", target.getPlayer().getName())));
                else
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_UPDATING_SKIN));
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
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS && Config.USE_NEW_PERMISSIONS) {
            if (!source.hasPermission("skinsrestorer.skin." + skin)) {
                source.sendMessage(plugin.parseMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                return;
            }
        }

        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            if (this.setSkin(source, target.getPlayer(), skin)) {
                if (!source.getName().equals(target.getPlayer().getName()))
                    source.sendMessage(plugin.parseMessage(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getName())));
            }
        });
    }

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    private boolean setSkin(CommandSource source, Player p, String skin) {
        return this.setSkin(source, p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player p, String skin, boolean save) {
        if (!C.validUsername(skin)) {
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
            source.sendMessage(plugin.parseMessage(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(source.getName()))));
            return false;
        }

        CooldownStorage.resetCooldown(source.getName());
        CooldownStorage.setCooldown(source.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = SkinStorage.getPlayerSkin(p.getName());
        try {
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), skin);
            plugin.getSkinApplier().applySkin(p, skin);
            p.sendMessage(plugin.parseMessage(Locale.SKIN_CHANGE_SUCCESS));
            return true;
        } catch (MojangAPI.SkinRequestException e) {
            source.sendMessage(plugin.parseMessage(e.getReason()));

            // set custom skin name back to old one if there is an exception
            if (save)
                SkinStorage.setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
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
