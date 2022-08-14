/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
 *
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
 */
package net.skinsrestorer.shared.commands;

import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogLevel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static co.aikar.commands.CommandManager.getCurrentCommandManager;

public interface ISkinCommand {
    @SuppressWarnings("deprecation")
    default void onDefault(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        onHelp(sender, getCurrentCommandManager().generateCommandHelp());
    }

    default void onSkinSetShort(ISRPlayer player, String skin) {
        if (!CommandUtil.isAllowedToExecute(player)) return;

        onSkinSetOther(player, player, skin, null);
    }

    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        if (Config.ENABLE_CUSTOM_HELP) sendHelp(sender);
        else help.showHelp();
    }

    default void onSkinClear(ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(player)) return;

        onSkinClearOther(player, player);
    }

    default void onSkinClearOther(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            String senderName = sender.getName();
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && plugin.getCooldownStorage().hasCooldown(senderName)) {
                sender.sendMessage(String.format(Locale.SKIN_COOLDOWN, plugin.getCooldownStorage().getCooldownSeconds(senderName)));
                return;
            }

            String playerName = target.getName();

            // remove users defined skin from database
            plugin.getSkinStorage().removeSkinOfPlayer(playerName);

            try {
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper());
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }

            if (sender == target) {
                sender.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
            } else {
                sender.sendMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", playerName));
            }
        });
    }

    default void onSkinSearch(ISRCommandSender sender, String searchString) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        sender.sendMessage(Locale.SKIN_SEARCH_MESSAGE.replace("%SearchString%", searchString));
    }

    default void onSkinUpdate(ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(player)) return;

        onSkinUpdateOther(player, player);
    }

    default void onSkinUpdateOther(ISRCommandSender sender, ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            final String senderName = sender.getName();
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && plugin.getCooldownStorage().hasCooldown(senderName)) {
                sender.sendMessage(String.format(Locale.SKIN_COOLDOWN, plugin.getCooldownStorage().getCooldownSeconds(senderName)));
                return;
            }

            final String playerName = player.getName();
            Optional<String> skin = plugin.getSkinStorage().getSkinNameOfPlayer(playerName);

            try {
                if (skin.isPresent()) {
                    // Filter skinUrl
                    if (skin.get().startsWith(" ")) {
                        sender.sendMessage(Locale.ERROR_UPDATING_URL);
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin.get())) {
                        sender.sendMessage(Locale.ERROR_UPDATING_SKIN);
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = Optional.of(plugin.getSkinStorage().getDefaultSkinName(playerName, true).getLeft());
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
                return;
            }

            if (setSkin(sender, player, skin.get(), false, null)) {
                if (sender == player)
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN);
                else
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", playerName));
            }
        });
    }

    default void onSkinSet(ISRPlayer player, String[] skin) {
        if (!CommandUtil.isAllowedToExecute(player)) return;

        if (skin.length == 0)
            throw new InvalidCommandArgument(true);

        onSkinSetOther(player, player, skin[0], null);
    }

    default void onSkinSetOther(ISRCommandSender sender, ISRPlayer player, String skin, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            if (Config.PER_SKIN_PERMISSIONS && !sender.hasPermission("skinsrestorer.skin." + skin)) {
                if (!sender.hasPermission("skinsrestorer.ownskin") && (!sender.equalsPlayer(player) || !skin.equalsIgnoreCase(sender.getName()))) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                    return;
                }
            }

            if (setSkin(sender, player, skin, true, skinVariant) && !sender.equalsPlayer(player))
                sender.sendMessage(Locale.ADMIN_SET_SKIN.replace("%player", player.getName()));
        });
    }

    default void onSkinSetUrl(ISRPlayer player, String url, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(player)) return;

        if (!C.validUrl(url)) {
            player.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, player, url, skinVariant);
    }

    default void sendHelp(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);

        sender.sendMessage(Locale.CUSTOM_HELP_IF_ENABLED.replace("%ver%", getPlugin().getVersion()));

        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);
    }

    default boolean setSkin(ISRCommandSender sender, ISRPlayer player, String skin, boolean restoreOnFailure, SkinVariant skinVariant) {
        ISRPlugin plugin = getPlugin();

        // Escape "null" skin, this did cause crash in the past for some waterfall instances
        // TODO: resolve this in a different way
        if (skin.equalsIgnoreCase("null")) {
            sender.sendMessage(Locale.INVALID_PLAYER.replace("%player", skin));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !sender.hasPermission("skinsrestorer.bypassdisabled")
                && Config.DISABLED_SKINS.stream().anyMatch(skin::equalsIgnoreCase)) {
            sender.sendMessage(Locale.SKIN_DISABLED);
            return false;
        }

        String senderName = sender.getName();
        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && plugin.getCooldownStorage().hasCooldown(senderName)) {
            sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", String.valueOf(plugin.getCooldownStorage().getCooldownSeconds(senderName))));
            return false;
        }

        String playerName = player.getName();
        String oldSkinName = restoreOnFailure ? plugin.getSkinStorage().getSkinNameOfPlayer(playerName).orElse(playerName) : null;
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url")
                    && !Config.SKIN_WITHOUT_PERM) { // Ignore /skin clear when defaultSkin = url
                sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                sender.sendMessage(Locale.SKINURL_DISALLOWED);
                return false;
            }

            // Apply cooldown to sender
            plugin.getCooldownStorage().setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

            try {
                sender.sendMessage(Locale.MS_UPDATING_SKIN);
                String skinName = " " + playerName; // so won't overwrite premium player names
                if (skinName.length() > 16) // max len of 16 char
                    skinName = skinName.substring(0, 16);

                IProperty generatedSkin = SkinsRestorerAPI.getApi().genSkinUrl(skin, skinVariant);
                SkinsRestorerAPI.getApi().setSkinData(skinName, generatedSkin,
                        Instant.now().plus(100, ChronoUnit.YEARS).toEpochMilli()); // "generate" and save skin for 100 years
                SkinsRestorerAPI.getApi().setSkinName(playerName, skinName); // set player to "whitespaced" name then reload skin
                SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), generatedSkin);

                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", "skinUrl"));

                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            } catch (Exception e) {
                plugin.getSrLogger().debug(SRLogLevel.SEVERE, "Could not generate skin url: " + skin, e);
                sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            }
        } else {
            // If skin is not an url, it's a username
            // Apply cooldown to sender
            plugin.getCooldownStorage().setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);
            try {
                if (restoreOnFailure) {
                    SkinsRestorerAPI.getApi().setSkinName(playerName, skin);
                }

                SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), skin);

                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", skin)); // TODO: should this not be sender? -> hidden skin set?

                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }
        }

        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        plugin.getCooldownStorage().setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        if (restoreOnFailure) {
            SkinsRestorerAPI.getApi().setSkinName(playerName, oldSkinName);
        }
        return false;
    }

    ISRPlugin getPlugin();
}
