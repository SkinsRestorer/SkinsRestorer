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
package net.skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

import java.util.concurrent.TimeUnit;

//todo update deprecated source.sendMessage for velocity
@SuppressWarnings("deprecation")
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
    public void onDefault(CommandSource source) {
        onHelp(source, getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    @SuppressWarnings({"unused"})
    public void onSkinSetShort(Player p, @Single String skin) {
        onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSource commandSource, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(commandSource);
        else
            help.showHelp();
    }


    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    @SuppressWarnings({"unused"})
    public void onSkinClear(Player p) {
        onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSource source, OnlinePlayer target) {
        plugin.getService().execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
                source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
                return;
            }

            final Player p = target.getPlayer();
            final String pName = p.getUsername();
            final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(pName, true);

            // remove users defined skin from database
            plugin.getSkinStorage().removePlayerSkin(pName);

            if (setSkin(source, p, skin, false, true)) {
                if (source == p)
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_SUCCESS));
                else
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_ISSUER.replace("%player", pName)));
            }
        });
    }


    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    @SuppressWarnings({"unused"})
    public void onSkinUpdate(Player p) {
        onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSource source, OnlinePlayer target) {
        plugin.getService().execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
                source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
                return;
            }

            final Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getUsername());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.startsWith(" ")) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_URL));
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin)) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getUsername(), true);
                }
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getMessage()));
                return;
            }

            if (setSkin(source, p, skin, false, false)) {
                if (source == p)
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN));
                else
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getUsername())));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player p, String[] skin) {
        if (skin.length > 0) {
            onSkinSetOther(p, new OnlinePlayer(p), skin[0]);
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin) {
        plugin.getService().execute(() -> {
            final Player p = target.getPlayer();
            if (Config.PER_SKIN_PERMISSIONS && !source.hasPermission("skinsrestorer.skin." + skin)) {
                if (!source.hasPermission("skinsrestorer.ownskin") && !getSenderName(source).equalsIgnoreCase(p.getUsername()) || !skin.equalsIgnoreCase(getSenderName(source))) {
                    source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }

            if (setSkin(source, p, skin) && !(source == p)) {
                source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.ADMIN_SET_SKIN.replace("%player", p.getUsername())));
            }
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    @SuppressWarnings({"unused"})
    public void onSkinSetUrl(Player p, String[] url) {
        if (url.length > 0) {
            if (C.validUrl(url[0])) {
                onSkinSetOther(p, new OnlinePlayer(p), url[0]);
            } else {
                p.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.ERROR_INVALID_URLSKIN));
            }
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    private boolean setSkin(CommandSource source, Player p, String skin) {
        return setSkin(source, p, skin, true, false);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player p, String skin, boolean save, boolean clear) {
        if (skin.equalsIgnoreCase("null") || !C.validMojangUsername(skin) && !C.validUrl(skin)) {
            source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear && !source.hasPermission("skinsrestorer.bypassdisabled")) {
            for (String dskin : Config.DISABLED_SKINS)
                if (skin.equalsIgnoreCase(dskin)) {
                    source.sendMessage(plugin.deserialize(Locale.SKIN_DISABLED));
                    return false;
                }
        }

        final String senderName = getSenderName(source);
        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(senderName)) {
            source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(senderName))));
            return false;
        }

        CooldownStorage.resetCooldown(senderName);
        CooldownStorage.setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        final String pName = p.getUsername();
        final String oldSkinName = plugin.getSkinStorage().getPlayerSkin(pName);

        if (C.validUrl(skin)) {
            if (!source.hasPermission("skinsrestorer.command.set.url")
                    && !Config.SKINWITHOUTPERM
                    && !clear) { //ignore /skin clear when DefaultSkin = url
                source.sendMessage(plugin.deserialize(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                source.sendMessage(plugin.deserialize(Locale.SKINURL_DISALLOWED));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            try {
                source.sendMessage(plugin.deserialize(Locale.MS_UPDATING_SKIN));
                String skinentry = " " + pName; // so won't overwrite premium playernames
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setPlayerSkin(pName, skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinApplierVelocity().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerVelocityAPI());
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getMessage()));
            } catch (Exception e) {
                log.log("[ERROR] Exception: could not generate skin url:" + skin + "\nReason= " + e.getMessage());
                source.sendMessage(plugin.deserialize(Locale.ERROR_INVALID_URLSKIN));
            }
        } else {
            //If skin is no url, its a username
            try {
                if (save)
                    plugin.getSkinStorage().setPlayerSkin(pName, skin);

                plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);
                plugin.getSkinApplierVelocity().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerVelocityAPI());
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    p.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS));
                return true;
            } catch (SkinRequestException e) {
                //todo add clear from bukkit skincommand.java
                source.sendMessage(plugin.deserialize(e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        // Set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        CooldownStorage.setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        rollback(p, oldSkinName, save);
        return false;
    }

    private void rollback(Player p, String oldSkinName, boolean save) {
        if (save)
            plugin.getSkinStorage().setPlayerSkin(p.getUsername(), oldSkinName != null ? oldSkinName : p.getUsername());
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
