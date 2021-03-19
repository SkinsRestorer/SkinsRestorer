/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;

import java.util.concurrent.TimeUnit;

@CommandAlias("skin")
@CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getSrLogger();
    }

    @Default
    @SuppressWarnings({"deprecation"})
    public void onDefault(CommandSender sender) {
        this.onHelp(sender, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    @SuppressWarnings({"unused"})
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

    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    @SuppressWarnings({"unused"})
    public void onSkinClear(ProxiedPlayer p) {
        this.onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSender sender, OnlinePlayer target) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName()))));
                return;
            }

            final ProxiedPlayer p = target.getPlayer();
            final String pName = p.getName();
            final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(pName, true);

            // remove users defined skin from database
            plugin.getSkinStorage().removePlayerSkin(pName);

            if (this.setSkin(sender, p, skin, false, true)) {
                if (sender == p)
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CLEAR_SUCCESS));
                else
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CLEAR_ISSUER.replace("%player", pName)));
            }
        });
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    @SuppressWarnings({"unused"})
    public void onSkinUpdate(ProxiedPlayer p) {
        this.onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSender sender, OnlinePlayer target) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName()))));
                return;
            }

            final ProxiedPlayer p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getName());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.startsWith(" ")) {
                        sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_URL));
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin)) {
                        sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getName(), true);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(TextComponent.fromLegacyText(e.getMessage()));
                return;
            }

            if (this.setSkin(sender, p, skin, false, false)) {
                if (sender == p)
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getName())));
                else
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SUCCESS_UPDATING_SKIN));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(ProxiedPlayer p, String[] skin) {
        if (skin.length > 0) {
            this.onSkinSetOther(p, new OnlinePlayer(p), skin[0]);
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSender sender, OnlinePlayer target, String skin) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            final ProxiedPlayer p = target.getPlayer();
            if (Config.PER_SKIN_PERMISSIONS && !sender.hasPermission("skinsrestorer.skin." + skin)) {
                if (!sender.hasPermission("skinsrestorer.ownskin") && !sender.getName().equalsIgnoreCase(p.getName()) || !skin.equalsIgnoreCase(sender.getName())) {
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }
            if (this.setSkin(sender, p, skin) && !(sender == p)) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ADMIN_SET_SKIN.replace("%player", p.getName())));
            }
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    @SuppressWarnings({"unused"})
    public void onSkinSetUrl(ProxiedPlayer p, String[] url) {
        if (url.length > 0) {
            if (C.validUrl(url[0])) {
                this.onSkinSetOther(p, new OnlinePlayer(p), url[0]);
            } else {
                p.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN));
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
        if (skin.equalsIgnoreCase("null") || !C.validMojangUsername(skin) && !C.validUrl(skin)) {
            sender.sendMessage(TextComponent.fromLegacyText(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear && !sender.hasPermission("skinsrestorer.bypassdisabled")) {
            for (String dskin : Config.DISABLED_SKINS)
                if (skin.equalsIgnoreCase(dskin)) {
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_DISABLED));
                    return false;
                }
        }

        final String senderName = sender.getName();
        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(senderName)) {
            sender.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(senderName))));
            return false;
        }

        CooldownStorage.resetCooldown(senderName);
        CooldownStorage.setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        final String pName = p.getName();
        final String oldSkinName = plugin.getSkinStorage().getPlayerSkin(pName);

        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM && !clear) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.SKINURL_DISALLOWED));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }


            try {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.MS_UPDATING_SKIN));
                String skinentry = " " + pName; // so won't overwrite premium playernames
                if (skinentry.length() > 16) {
                    skinentry = skinentry.substring(0, 16);
                } // max len of 16 char
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(pName, skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinApplierBungee().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerBungeeAPI());
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CHANGE_SUCCESS));

                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(TextComponent.fromLegacyText(e.getMessage()));
            } catch (Exception e) {
                log.log("[ERROR] could not generate skin url:" + skin + " stacktrace:");
                if (Config.DEBUG)
                    e.printStackTrace();
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN));
            }
        } else {
            //If skin is no url, its a username
            try {
                plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);

                if (save) {
                    plugin.getSkinStorage().setPlayerSkin(pName, skin);
                    plugin.getSkinApplierBungee().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerBungeeAPI());
                } else {
                    plugin.getSkinApplierBungee().applySkin(p, skin, null);
                }

                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(TextComponent.fromLegacyText(Locale.SKIN_CHANGE_SUCCESS)); //todo: should this not be sender? -> hidden skin update?? (maybe when p has no perms)
                return true;
            } catch (SkinRequestException e) {
                if (clear) {
                    Object props = plugin.getSkinStorage().createProperty("textures", "", "");
                    try {
                        plugin.getSkinStorage().setSkinData("00", props);
                        plugin.getSkinApplierBungee().applySkin(p, "00", null);
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                sender.sendMessage(TextComponent.fromLegacyText(e.getMessage()));
                // Rollback to old skin on exception
                rollback(p, oldSkinName, save);
            } catch (Exception e) {
                sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_UPDATING_SKIN));
                // Rollback to old skin on exception
                rollback(p, oldSkinName, save);
            }
        }
        // Set CoolDown to ERROR_COOLDOWN
        CooldownStorage.setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
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
