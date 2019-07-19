package skinsrestorer.velocity.command;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.velocity.SkinsRestorer;
import skinsrestorer.velocity.utils.SkinApplier;

import java.util.concurrent.TimeUnit;

/**
 * Created by McLive on 17.02.2019.
 */
@SuppressWarnings("deprecation")
@CommandAlias("skin") @CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
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
    public void onHelp(CommandSource commandSource, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(commandSource);
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
        plugin.getService().execute(() -> {
            Player p = target.getPlayer();
            String skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getUsername(), true);

            // remove users custom skin and set default skin / his skin
            SkinStorage.removePlayerSkin(p.getUsername());
            if (this.setSkin(source, p, skin, false)) {
                if (!getSenderName(source).equals(target.getPlayer().getUsername()))
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_ISSUER.replace("%player", target.getPlayer().getUsername())));
                else
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_SUCCESS));
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
        plugin.getService().execute(() -> {
            Player p = target.getPlayer();
            String skin = SkinStorage.getPlayerSkin(p.getUsername());

            // User has no custom skin set, get the default skin name / his skin
            if (skin == null)
                skin = SkinStorage.getDefaultSkinNameIfEnabled(p.getUsername(), true);

            if (!SkinStorage.forceUpdateSkinData(skin)) {
                source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_SKIN));
                return;
            }

            if (this.setSkin(source, p, skin, false)) {
                if (!getSenderName(source).equals(target.getPlayer().getUsername()))
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", target.getPlayer().getUsername())));
                else
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN));
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
                source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                return;
            }
        }

        plugin.getService().execute(() -> {
            if (this.setSkin(source, target.getPlayer(), skin)) {
                if (!getSenderName(source).equals(target.getPlayer().getUsername())) {
                    source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getUsername())));
                }
            }
        });
    }




    private final SkinsRestorer plugin;

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
            source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!source.hasPermission("skinsrestorer.bypassdisabled")) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        source.sendMessage(plugin.deserialize(Locale.SKIN_DISABLED));
                        return false;
                    }
            }

        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
            source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN_NEW.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
            return false;
        }

        CooldownStorage.resetCooldown(getSenderName(source));
        CooldownStorage.setCooldown(getSenderName(source), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = SkinStorage.getPlayerSkin(p.getUsername());
        plugin.getService().execute(() -> {
            try {
                MojangAPI.getUUID(skin);
                if (save) {
                    SkinStorage.setPlayerSkin(p.getUsername(), skin);
                    SkinApplier.applySkin(p, p.getUsername());
                } else {
                    SkinApplier.applySkin(p, skin);
                }
                p.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS));
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getReason()));

                // set custom skin name back to old one if there is an exception
                this.rollback(p, oldSkinName, save);
            } catch (Exception e) {
                e.printStackTrace();

                // set custom skin name back to old one if there is an exception
                this.rollback(p, oldSkinName, save);
            }
        });
        return true;
    }

    private void rollback(Player p, String oldSkinName, boolean save) {
        if (save)
            SkinStorage.setPlayerSkin(p.getUsername(), oldSkinName != null ? oldSkinName : p.getUsername());
    }

    private boolean checkPerm(Player p, String perm) {
        return p.hasPermission(perm) || Config.SKINWITHOUTPERM;
    }

    private void sendHelp(CommandSource commandSource) {
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
        commandSource.sendMessage(plugin.deserialize(Locale.HELP_PLAYER.replace("%ver%", plugin.getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
    }

    private String getSenderName(CommandSource source) {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }
}
