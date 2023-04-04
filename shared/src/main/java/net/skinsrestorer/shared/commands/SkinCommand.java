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
import ch.jalu.configme.properties.Property;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.PermissionRegistry;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.utils.C;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@SuppressWarnings("unused")
@PublicVisibility
@CommandNames("skin")
@Description(Message.HELP_SKIN)
@CommandPermission(value = PermissionRegistry.SKIN)
@CommandConditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SkinCommand {
    private final SRPlatformAdapter<?> adapter;
    private final SRPlugin plugin;
    private final SettingsManager settings;
    private final CooldownStorage cooldownStorage;
    private final SkinStorageImpl skinStorage;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SharedSkinApplier<Object> skinApplier;
    private final MineSkinAPI mineSkinAPI;
    private final CommandManager<SRCommandSender> commandManager;

    @RootCommand
    private void onDefault(SRCommandSender sender) {
        for (String line : commandManager.getHelp("skin", sender)) {
            sender.sendMessage(line);
        }
    }

    @RootCommand
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSetShort(SRPlayer player, String skin) {
        onSkinSetOther(player, player, skin, null);
    }

    @Subcommand("clear")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR)
    @Description(Message.HELP_SKIN_CLEAR)
    @CommandConditions("cooldown")
    private void onSkinClear(SRPlayer player) {
        onSkinClearOther(player, player);
    }

    @Subcommand("clear")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR_OTHER)
    @Description(Message.HELP_SKIN_CLEAR_OTHER)
    @CommandConditions("cooldown")
    private void onSkinClearOther(SRCommandSender sender, SRPlayer target) {
        String playerName = target.getName();

        // remove users defined skin from database
        skinStorage.removeSkinNameOfPlayer(playerName);

        try {
            Optional<SkinProperty> property = skinStorage.getDefaultSkinForPlayer(playerName);
            skinApplier.applySkin(target.getAs(Object.class), property.orElse(SkinProperty.of("", "")));
        } catch (DataRequestException e) {
            sender.sendMessage(getRootCause(e).getMessage());
        }

        if (sender.getName().equals(target.getName())) {
            sender.sendMessage(Message.SUCCESS_SKIN_CLEAR);
        } else {
            sender.sendMessage(Message.SUCCESS_SKIN_CLEAR_OTHER, playerName);
        }
    }

    @Subcommand("search")
    @CommandPermission(PermissionRegistry.SKIN_SEARCH)
    @Description(Message.HELP_SKIN_SEARCH)
    @CommandConditions("cooldown")
    private void onSkinSearch(SRCommandSender sender, String searchString) {
        sender.sendMessage(Message.SKIN_SEARCH_MESSAGE, searchString);
    }

    @Subcommand("update")
    @CommandPermission(PermissionRegistry.SKIN_UPDATE)
    @Description(Message.HELP_SKIN_UPDATE)
    @CommandConditions("cooldown")
    private void onSkinUpdate(SRPlayer player) {
        onSkinUpdateOther(player, player);
    }

    @Subcommand("update")
    @CommandPermission(PermissionRegistry.SKIN_UPDATE_OTHER)
    @Description(Message.HELP_SKIN_UPDATE_OTHER)
    @CommandConditions("cooldown")
    private void onSkinUpdateOther(SRCommandSender sender, SRPlayer target) {
        String playerName = target.getName();
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
                skin = skinStorage.getDefaultSkinNameForPlayer(playerName);
            }
        } catch (DataRequestException e) {
            sender.sendMessage(getRootCause(e).getMessage());
            return;
        }

        if (setSkin(sender, target, skin.orElse(playerName), false, null)) {
            if (sender.getName().equals(target.getName()))
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN);
            else
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN_OTHER, playerName);
        }
    }

    @Subcommand("set")
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSet(SRPlayer player, String skin) {
        onSkinSetOther(player, player, skin, null);
    }

    @Subcommand("set")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @Description(Message.HELP_SKIN_SET_OTHER)
    @CommandConditions("cooldown")
    private void onSkinSetOther(SRCommandSender sender, SRPlayer target, String skin, SkinVariant skinVariant) {
        if (settings.getProperty(CommandConfig.PER_SKIN_PERMISSIONS) && !sender.hasPermission(PermissionRegistry.forSkin(skin))) {
            if (!sender.hasPermission(PermissionRegistry.OWN_SKIN) && (!playerEqual(sender, target) || !skin.equalsIgnoreCase(sender.getName()))) {
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
                return;
            }
        }

        if (setSkin(sender, target, skin, true, skinVariant) && !playerEqual(sender, target)) {
            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE_OTHER, target.getName());
        }
    }

    @Subcommand("url")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @Description(Message.HELP_SKIN_SET_OTHER_URL) // TODO: rename to HELP_SKIN_SET_URL
    @CommandConditions("cooldown")
    private void onSkinSetUrl(SRPlayer player, String url, SkinVariant skinVariant) {
        if (!C.validUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, player, url, skinVariant);
    }

    @Subcommand({"menu", "gui"})
    @CommandPermission(PermissionRegistry.SKINS)
    @Private
    private void onGUIShortcut(SRPlayer player) {
        commandManager.getExecutor().execute(player, "skins");
    }

    private void sendHelp(SRCommandSender sender) {
        String srLine = locale.getMessage(sender, Message.SR_LINE);
        if (!srLine.isEmpty()) {
            sender.sendMessage(srLine);
        }

        sender.sendMessage(Message.CUSTOM_HELP_IF_ENABLED, plugin.getVersion());

        if (!srLine.isEmpty()) {
            sender.sendMessage(srLine);
        }
    }

    private boolean setSkin(SRCommandSender sender, SRPlayer player, String skinName, boolean saveSkin, SkinVariant skinVariant) {
        // Escape "null" skin, this did cause crash in the past for some waterfall instances
        // TODO: resolve this in a different way
        if (skinName.equalsIgnoreCase("null")) {
            sender.sendMessage(Message.INVALID_PLAYER, skinName);
            return false;
        }

        if (settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED) && !sender.hasPermission(PermissionRegistry.BYPASS_DISABLED)
                && settings.getProperty(CommandConfig.DISABLED_SKINS).stream().anyMatch(skinName::equalsIgnoreCase)) {
            sender.sendMessage(Message.ERROR_SKIN_DISABLED);
            return false;
        }

        String playerName = player.getName();
        String oldSkinName = saveSkin ? skinStorage.getSkinNameOfPlayer(playerName).orElse(playerName) : null;
        if (C.validUrl(skinName)) {
            if (!sender.hasPermission(PermissionRegistry.SKIN_SET_URL) // TODO: Maybe we should do this in the command itself?
                    && !settings.getProperty(CommandConfig.SKIN_WITHOUT_PERM)) { // Ignore /skin clear when defaultSkin = url
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!allowedSkinUrl(skinName)) {
                sender.sendMessage(Message.ERROR_SKINURL_DISALLOWED);
                return false;
            }

            // Apply cooldown to sender
            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

            try {
                sender.sendMessage(Message.MS_UPDATING_SKIN);
                String skinUrlName = " " + playerName; // so won't overwrite premium player names
                if (skinUrlName.length() > 16) // max len of 16 char
                    skinUrlName = skinUrlName.substring(0, 16);

                SkinProperty generatedSkin = mineSkinAPI.genSkin(skinName, skinVariant);
                skinStorage.setSkinData(skinUrlName, generatedSkin, 0); // "generate" and save skin forever
                skinStorage.setSkinNameOfPlayer(playerName, skinUrlName); // set player to "whitespaced" name then reload skin
                skinApplier.applySkin(player.getAs(Object.class), generatedSkin);

                String success = locale.getMessage(player, Message.SUCCESS_SKIN_CHANGE);
                if (!success.isEmpty() && !success.equals(locale.getMessage(player, Message.PREFIX_FORMAT))) {
                    player.sendMessage(Message.SUCCESS_SKIN_CHANGE, "skinUrl");
                }

                return true;
            } catch (DataRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            } catch (Exception e) {
                logger.debug(SRLogLevel.SEVERE, String.format("Could not generate skin url: %s", skinName), e);
                sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
            }
        } else {
            // If skin is not an url, it's a username
            // Apply cooldown to sender
            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

            try {
                if (saveSkin) {
                    skinStorage.setSkinNameOfPlayer(playerName, skinName);
                }

                skinApplier.applySkin(player.getAs(Object.class), skinName);

                String success = locale.getMessage(player, Message.SUCCESS_SKIN_CHANGE);
                if (!success.isEmpty() && !success.equals(locale.getMessage(player, Message.PREFIX_FORMAT))) {
                    player.sendMessage(Message.SUCCESS_SKIN_CHANGE, skinName); // TODO: should this not be sender? -> hidden skin set?
                }

                return true;
            } catch (DataRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        }

        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
        if (saveSkin) {
            skinStorage.setSkinNameOfPlayer(playerName, oldSkinName);
        }
        return false;
    }

    private void setCoolDown(SRCommandSender sender, Property<Integer> time) {
        if (sender instanceof SRPlayer) {
            UUID senderUUID = ((SRPlayer) sender).getUniqueId();
            cooldownStorage.setCooldown(senderUUID, settings.getProperty(time), TimeUnit.SECONDS);
        }
    }

    private boolean allowedSkinUrl(String url) {
        if (settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED)) {
            for (String possiblyAllowedUrl : settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_LIST)) {
                if (url.startsWith(possiblyAllowedUrl)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean playerEqual(SRCommandSender sender, SRPlayer player) {
        return sender instanceof SRPlayer && ((SRPlayer) sender).getUniqueId().equals(player.getUniqueId());
    }
}
