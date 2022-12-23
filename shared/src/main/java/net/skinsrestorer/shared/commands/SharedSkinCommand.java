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

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@RequiredArgsConstructor
public abstract class SharedSkinCommand extends BaseCommand {
    protected final ISRPlugin plugin;
    private final SettingsManager settings;
    private final CooldownStorage cooldownStorage;
    private final SkinStorage skinStorage;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;

    @SuppressWarnings("deprecation")
    protected void onDefault(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        onHelp(sender, getCurrentCommandManager().generateCommandHelp());
    }

    protected void onSkinSetShort(ISRPlayer player, String skin) {
        if (!CommandUtil.isAllowedToExecute(player, settings)) return;

        onSkinSetOther(player, player, skin, null);
    }

    protected void onHelp(ISRCommandSender sender, CommandHelp help) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        if (settings.getProperty(Config.ENABLE_CUSTOM_HELP)) {
            sendHelp(sender);
        } else {
            help.showHelp();
        }
    }

    protected void onSkinClear(ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(player, settings)) return;

        onSkinClearOther(player, player);
    }

    public void onSkinClearOther(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            String senderName = sender.getName();
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(senderName)) {
                sender.sendMessage(Message.SKIN_COOLDOWN, cooldownStorage.getCooldownSeconds(senderName));
                return;
            }

            String playerName = target.getName();

            // remove users defined skin from database
            skinStorage.removeSkinOfPlayer(playerName);

            try {
                IProperty property = skinStorage.getDefaultSkinForPlayer(playerName).getLeft();
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper(), property);
            } catch (NotPremiumException e) {
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper(),
                        SkinsRestorerAPI.getApi().createPlatformProperty(IProperty.TEXTURES_NAME, "", ""));
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }

            if (sender == target) {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR);
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR_OTHER, playerName);
            }
        });
    }

    protected void onSkinSearch(ISRCommandSender sender, String searchString) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        sender.sendMessage(Message.SKIN_SEARCH_MESSAGE, searchString);
    }

    protected void onSkinUpdate(ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(player, settings)) return;

        onSkinUpdateOther(player, player);
    }

    public void onSkinUpdateOther(ISRCommandSender sender, ISRPlayer player) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            final String senderName = sender.getName();
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(senderName)) {
                sender.sendMessage(Message.SKIN_COOLDOWN, cooldownStorage.getCooldownSeconds(senderName));
                return;
            }

            final String playerName = player.getName();
            Optional<String> skin = skinStorage.getSkinNameOfPlayer(playerName);

            try {
                if (skin.isPresent()) {
                    // Filter skinUrl
                    if (skin.get().startsWith(" ")) {
                        sender.sendMessage(Message.ERROR_UPDATING_URL);
                        return;
                    }

                    if (!skinStorage.updateSkinData(skin.get())) {
                        sender.sendMessage(Message.ERROR_UPDATING_SKIN);
                        return;
                    }
                } else {
                    // get DefaultSkin
                    skin = Optional.of(skinStorage.getDefaultSkinName(playerName, true).getLeft());
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
                return;
            }

            if (setSkin(sender, player, skin.get(), false, null)) {
                if (sender == player)
                    sender.sendMessage(Message.SUCCESS_UPDATING_SKIN);
                else
                    sender.sendMessage(Message.SUCCESS_UPDATING_SKIN_OTHER, playerName);
            }
        });
    }

    protected void onSkinSet(ISRPlayer player, String[] skin) {
        if (!CommandUtil.isAllowedToExecute(player, settings)) return;

        if (skin.length == 0)
            throw new InvalidCommandArgument(true);

        onSkinSetOther(player, player, skin[0], null);
    }

    public void onSkinSetOther(ISRCommandSender sender, ISRPlayer player, String skin, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            if (settings.getProperty(Config.PER_SKIN_PERMISSIONS) && !sender.hasPermission("skinsrestorer.skin." + skin)) {
                if (!sender.hasPermission("skinsrestorer.ownskin") && (!sender.equalsPlayer(player) || !skin.equalsIgnoreCase(sender.getName()))) {
                    sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
                    return;
                }
            }

            if (setSkin(sender, player, skin, true, skinVariant) && !sender.equalsPlayer(player))
                sender.sendMessage(Message.SUCCESS_SKIN_CHANGE_OTHER, player.getName());
        });
    }

    protected void onSkinSetUrl(ISRPlayer player, String url, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(player, settings)) return;

        if (!C.validUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, player, url, skinVariant);
    }

    protected void sendHelp(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        String srLine = locale.getMessage(sender, Message.SR_LINE);
        if (!srLine.isEmpty())
            sender.sendMessage(srLine);

        sender.sendMessage(Message.CUSTOM_HELP_IF_ENABLED, plugin.getVersion());

        if (!srLine.isEmpty())
            sender.sendMessage(srLine);
    }

    protected boolean setSkin(ISRCommandSender sender, ISRPlayer player, String skin, boolean restoreOnFailure, SkinVariant skinVariant) {
        // Escape "null" skin, this did cause crash in the past for some waterfall instances
        // TODO: resolve this in a different way
        if (skin.equalsIgnoreCase("null")) {
            sender.sendMessage(Message.INVALID_PLAYER, skin);
            return false;
        }

        if (settings.getProperty(Config.DISABLED_SKINS_ENABLED) && !sender.hasPermission("skinsrestorer.bypassdisabled")
                && settings.getProperty(Config.DISABLED_SKINS).stream().anyMatch(skin::equalsIgnoreCase)) {
            sender.sendMessage(Message.ERROR_SKIN_DISABLED);
            return false;
        }

        String senderName = sender.getName();
        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(senderName)) {
            sender.sendMessage(Message.SKIN_COOLDOWN, String.valueOf(cooldownStorage.getCooldownSeconds(senderName)));
            return false;
        }

        String playerName = player.getName();
        String oldSkinName = restoreOnFailure ? skinStorage.getSkinNameOfPlayer(playerName).orElse(playerName) : null;
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url")
                    && !settings.getProperty(Config.SKIN_WITHOUT_PERM)) { // Ignore /skin clear when defaultSkin = url
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!C.allowedSkinUrl(settings, skin)) {
                sender.sendMessage(Message.ERROR_SKINURL_DISALLOWED);
                return false;
            }

            // Apply cooldown to sender
            cooldownStorage.setCooldown(senderName, settings.getProperty(Config.SKIN_CHANGE_COOLDOWN), TimeUnit.SECONDS);

            try {
                sender.sendMessage(Message.MS_UPDATING_SKIN);
                String skinName = " " + playerName; // so won't overwrite premium player names
                if (skinName.length() > 16) // max len of 16 char
                    skinName = skinName.substring(0, 16);

                IProperty generatedSkin = SkinsRestorerAPI.getApi().genSkinUrl(skin, skinVariant);
                SkinsRestorerAPI.getApi().setSkinData(skinName, generatedSkin,
                        System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000)); // "generate" and save skin for 100 years
                SkinsRestorerAPI.getApi().setSkinName(playerName, skinName); // set player to "whitespaced" name then reload skin
                SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), generatedSkin);

                String success = locale.getMessage(player, Message.SUCCESS_SKIN_CHANGE);
                if (!success.isEmpty() && !success.equals(locale.getMessage(player, Message.PREFIX)))
                    player.sendMessage(Message.SUCCESS_SKIN_CHANGE, "skinUrl");

                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            } catch (Exception e) {
                logger.debug(SRLogLevel.SEVERE, "Could not generate skin url: " + skin, e);
                sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
            }
        } else {
            // If skin is not an url, it's a username
            // Apply cooldown to sender
            cooldownStorage.setCooldown(senderName, settings.getProperty(Config.SKIN_CHANGE_COOLDOWN), TimeUnit.SECONDS);
            try {
                if (restoreOnFailure) {
                    SkinsRestorerAPI.getApi().setSkinName(playerName, skin);
                }

                SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), skin);

                String success = locale.getMessage(player, Message.SUCCESS_SKIN_CHANGE);
                if (!success.isEmpty() && !success.equals(locale.getMessage(player, Message.PREFIX)))
                    player.sendMessage(Message.SUCCESS_SKIN_CHANGE, skin); // TODO: should this not be sender? -> hidden skin set?

                return true;
            } catch (SkinRequestException | NotPremiumException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        }

        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        cooldownStorage.setCooldown(senderName, settings.getProperty(Config.SKIN_ERROR_COOLDOWN), TimeUnit.SECONDS);
        if (restoreOnFailure) {
            SkinsRestorerAPI.getApi().setSkinName(playerName, oldSkinName);
        }
        return false;
    }
}
