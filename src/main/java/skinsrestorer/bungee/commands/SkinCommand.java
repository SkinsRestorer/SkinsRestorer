package skinsrestorer.bungee.commands;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SRLogger;

import java.util.concurrent.TimeUnit;

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
    public void onSkinSetShort(ProxiedPlayer p, @Single String skin) {
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
    public void onSkinClear(ProxiedPlayer p) {
        this.onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear") @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSender sender, OnlinePlayer target) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            ProxiedPlayer p = target.getPlayer();
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);

            //command cooldown = cancel
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName()))));
                return;
            }

            // remove users custom skin and set default skin / his skin
            plugin.getSkinStorage().removePlayerSkin(p.getName());
            if (this.setSkin(sender, p, skin, false, true)) {
                if (!sender.getName().equals(target.getPlayer().getName()))
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CLEAR_ISSUER.replace("%player", target.getPlayer().getName())));
                else
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CLEAR_SUCCESS));
            }
        });
    }


    @Subcommand("update") @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    public void onSkinUpdate(ProxiedPlayer p) {
        this.onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update") @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSender sender, OnlinePlayer target) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            //Check cooldown first
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName()))));
                return;
            }

            ProxiedPlayer p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.contains(" ")) {
                        sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_URL));
                        return;
                    }
                    // check if premium name
                    plugin.getMojangAPI().getUUIDMojang(skin);
                    if (!plugin.getSkinStorage().forceUpdateSkinData(skin)) {
                        sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);
                }
            } catch (SkinRequestException e) {
                // non premium = cancel
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_CUSTOMSKIN));
                return;
            }

            if (this.setSkin(sender, p, skin, false, false)) {
                if (!sender.getName().equals(p.getName()))
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getName())));
                else
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SUCCESS_UPDATING_SKIN));
            }
        });
    }


    @Subcommand("set") @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(ProxiedPlayer p, String[] skin) {
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
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }
        }

        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (this.setSkin(sender, target.getPlayer(), skin)) {
                if (!sender.getName().equals(target.getPlayer().getName())) {
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getName())));
                }
            }
        });
    }

    @Subcommand("url") @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    public void onSkinSetUrl(ProxiedPlayer p, String[] url) {
        if (url.length > 0) {
            if(C.validUrl(url[0])) {
                this.onSkinSetOther(p, new OnlinePlayer(p), url[0]);
            } else {
                p.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN_2));
            }
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    private boolean setSkin(CommandSender sender, ProxiedPlayer p, String skin) {
        return this.setSkin(sender, p, skin, true, false);
    }
    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSender sender, ProxiedPlayer p, String skin, boolean save, boolean clear) {
        if (skin.equalsIgnoreCase("null") || !C.validUsername(skin) && !C.validUrl(skin)) {
            if (C.matchesRegex(skin)) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN_2));
            } else {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.INVALID_PLAYER.replace("%player", skin)));
            }

            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED)
            if (!sender.hasPermission("skinsrestorer.bypassdisabled") && !clear) {
                for (String dskin : Config.DISABLED_SKINS)
                    if (skin.equalsIgnoreCase(dskin)) {
                        sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_DISABLED));
                        return false;
                    }
            }

        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
            sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName()))));
            return false;
        }

        CooldownStorage.resetCooldown(sender.getName());
        CooldownStorage.setCooldown(sender.getName(), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = plugin.getSkinStorage().getPlayerSkin(p.getName());
        if (C.validUsername(skin)) {
            try {
                plugin.getSkinStorage().getOrCreateSkinForPlayer(skin);
                if (save) {
                    plugin.getSkinStorage().setPlayerSkin(p.getName(), skin);
                    plugin.getSkinApplier().applySkin(p);
                } else {
                    plugin.getSkinApplier().applySkin(p, skin, null);
                }
                p.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CHANGE_SUCCESS)); //todo: should this not be sender? -> hidden skin update?? (maybe when p has no perms)
                return true;
            } catch (SkinRequestException e) {
                if (clear) {
                    //plugin.getSkinStorage()

                    Object props = plugin.getSkinStorage().createProperty("textures", "", "");
                    try {
                        plugin.getSkinStorage().setSkinData("00", props);
                        plugin.getSkinApplier().applySkin(p, "00", null);
                    } catch (Exception ignored) {
                    }
                    return true;
                }

                sender.sendMessage(TextComponent.fromLegacyText(e.getReason()));
            } catch (Exception e) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_SKIN));
            }
        }
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM && !clear) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                CooldownStorage.resetCooldown(sender.getName());
                return false;
            }
            try {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.MS_UPDATING_SKIN));
                String skinentry = " "+p.getName(); // so won't overwrite premium playernames
                if (skinentry.length() > 16) { skinentry = skinentry.substring(0, 16); } // max len of 16 char
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(p.getName(), skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinApplier().applySkin(p);
                p.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(TextComponent.fromLegacyText(e.getReason()));
            } catch (Exception e) {
                log.log("[ERROR] could not generate skin url:" + skin + " stacktrace:");
                if (Config.DEBUG)
                e.printStackTrace();
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN_2));
            }
        }
        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        CooldownStorage.setCooldown(sender.getName(), Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        this.rollback(p, oldSkinName, save);
        return false;
    }

    private void rollback(ProxiedPlayer p, String oldSkinName, boolean save) {
        if (save)
            plugin.getSkinStorage().setPlayerSkin(p.getName(), oldSkinName != null ? oldSkinName : p.getName());
    }

    private void sendHelp(CommandSender sender) {
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(TextComponent.fromLegacyText(Locale.SR_LINE));
        sender.sendMessage(TextComponent.fromLegacyText(Locale.HELP_PLAYER.replace("%ver%", SkinsRestorer.getInstance().getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(TextComponent.fromLegacyText(Locale.SR_LINE));
    }
}
