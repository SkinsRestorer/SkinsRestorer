/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRConstants;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@SuppressWarnings("unused")
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
    private final SkinStorage skinStorage;
    private final PlayerStorage playerStorage;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SharedSkinApplier<Object> skinApplier;
    private final MineSkinAPI mineSkinAPI;
    private final CommandManager<SRCommandSender> commandManager;

    @RootCommand
    private void onDefault(SRCommandSender sender) {
        for (String line : commandManager.getHelpMessage("skin", sender, true)) {
            sender.sendMessage(line);
        }
    }

    @RootCommand
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSetShort(SRPlayer player, String skinName) {
        onSkinSetOther(player, skinName, player, null);
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
        // Remove the targets defined skin from database
        playerStorage.removeSkinIdOfPlayer(target.getUniqueId());

        try {
            Optional<SkinProperty> property = playerStorage.getSkinForPlayer(target.getUniqueId(), target.getName());
            skinApplier.applySkin(target.getAs(Object.class), property.orElse(SRConstants.EMPTY_SKIN));

            if (sender.getName().equals(target.getName())) {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR);
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR_OTHER, target.getName());
            }
        } catch (DataRequestException e) {
            e.printStackTrace();
            sender.sendMessage(Message.ERROR_UPDATING_SKIN); // TODO: Better error message
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
        Optional<SkinIdentifier> setSkin = playerStorage.getSkinIdOfPlayer(target.getUniqueId());

        try {
            if (setSkin.isPresent() && setSkin.get().getSkinType() == SkinType.PLAYER) {
                if (!skinStorage.updatePlayerSkinData(UUID.fromString(setSkin.get().getIdentifier())).isPresent()) {
                    sender.sendMessage(Message.ERROR_UPDATING_SKIN);
                    return;
                }
            }

            Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(target.getUniqueId(), target.getName());

            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE, null); // TODO: Figure this out

            skinApplier.applySkin(target.getAs(Object.class), skin.orElse(SRConstants.EMPTY_SKIN));

            if (sender.getName().equals(target.getName())) {
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN);
            } else {
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN_OTHER, target.getName());
            }

            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);
        } catch (DataRequestException e) {
            sender.sendMessage(getRootCause(e).getMessage());
            setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
        }
    }

    @Subcommand("set")
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSet(SRPlayer player, String skinName) {
        onSkinSetOther(player, skinName, player);
    }

    @Subcommand("set")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @Description(Message.HELP_SKIN_SET_OTHER)
    @CommandConditions("cooldown")
    private void onSkinSetOther(SRCommandSender sender, String skinName, SRPlayer target) {
        onSkinSetOther(sender, skinName, target, null);
    }

    @Subcommand("set")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @Description(Message.HELP_SKIN_SET_OTHER)
    @CommandConditions("cooldown")
    private void onSkinSetOther(SRCommandSender sender, String skinName, SRPlayer target, SkinVariant skinVariant) {
        if (!canSetSkin(sender, skinName)) {
            return;
        }

        if (isDisabledSkin(skinName) && !sender.hasPermission(PermissionRegistry.BYPASS_DISABLED)) {
            sender.sendMessage(Message.ERROR_SKIN_DISABLED);
            return;
        }

        if (!setSkin(sender, target, skinName, skinVariant)) {
            setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
            return;
        }

        if (playerEqual(sender, target)) {
            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE, (Object) null); // TODO: Figure this out
        } else {
            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE_OTHER, target.getName());
        }
    }

    @Subcommand("url")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @Description(Message.HELP_SKIN_SET_URL)
    @CommandConditions("cooldown")
    private void onSkinSetUrl(SRPlayer player, String url, SkinVariant skinVariant) {
        if (!C.validUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, url, player, skinVariant);
    }

    @Subcommand({"menu", "gui"})
    @CommandPermission(PermissionRegistry.SKINS)
    @Private
    private void onGUIShortcut(SRPlayer player) {
        commandManager.executeCommand(player, "skins");
    }

    private boolean setSkin(SRCommandSender sender, SRPlayer target, String skinInput, SkinVariant skinVariant) {
        Optional<SkinIdentifier> oldSkinId = playerStorage.getSkinIdOfPlayer(target.getUniqueId());
        if (C.validUrl(skinInput)) {
            if (!sender.hasPermission(PermissionRegistry.SKIN_SET_URL)) { // TODO: Maybe we should do this in the command itself?
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!allowedSkinUrl(skinInput)) {
                sender.sendMessage(Message.ERROR_SKINURL_DISALLOWED);
                return false;
            }

            try {
                sender.sendMessage(Message.MS_UPDATING_SKIN);

                MineSkinResponse response = mineSkinAPI.genSkin(skinInput, skinVariant);
                skinStorage.setURLSkinByResponse(skinInput, response); // "generate" and save skin forever
                playerStorage.setSkinIdOfPlayer(target.getUniqueId(), SkinIdentifier.ofURL(skinInput, response.getGeneratedVariant()));
                skinApplier.applySkin(target.getAs(Object.class), response.getProperty());

                setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

                return true;
            } catch (DataRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            } catch (Exception e) {
                logger.debug(SRLogLevel.SEVERE, String.format("Could not generate skin url: %s", skinInput), e);
                sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
            }
        } else {
            try {
                // Perform skin lookup, which causes a second url regex check, but we don't care
                Optional<InputDataResult> optional = skinStorage.findOrCreateSkinData(skinInput);

                if (!optional.isPresent()) {
                    sender.sendMessage(Message.NOT_PREMIUM); // TODO: Is this the right message?
                    return false;
                }

                playerStorage.setSkinIdOfPlayer(target.getUniqueId(), optional.get().getIdentifier());

                skinApplier.applySkin(target.getAs(Object.class), optional.get().getProperty());

                setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

                return true;
            } catch (DataRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        }

        playerStorage.setSkinIdOfPlayer(target.getUniqueId(), oldSkinId.orElse(null)); // TODO: Rethink this

        return false;
    }

    private void setCoolDown(SRCommandSender sender, Property<Integer> time) {
        if (sender instanceof SRPlayer) {
            UUID senderUUID = ((SRPlayer) sender).getUniqueId();
            cooldownStorage.setCooldown(senderUUID, settings.getProperty(time), TimeUnit.SECONDS);
        }
    }

    private boolean isDisabledSkin(String skinName) {
        return settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED)
                && settings.getProperty(CommandConfig.DISABLED_SKINS).stream().anyMatch(skinName::equalsIgnoreCase);
    }

    private boolean allowedSkinUrl(String url) {
        if (!settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED)) {
            return true;
        }

        for (String possiblyAllowedUrl : settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_LIST)) {
            if (url.startsWith(possiblyAllowedUrl)) {
                return true;
            }
        }

        return false;
    }

    private boolean canSetSkin(SRCommandSender sender, String skinName) {
        if (settings.getProperty(CommandConfig.PER_SKIN_PERMISSIONS) && !sender.hasPermission(PermissionRegistry.forSkin(skinName))) {
            if (sender.hasPermission(PermissionRegistry.OWN_SKIN) && skinName.equalsIgnoreCase(sender.getName())) {
                return true;
            } else {
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean playerEqual(SRCommandSender sender, SRPlayer player) {
        return sender instanceof SRPlayer && ((SRPlayer) sender).getUniqueId().equals(player.getUniqueId());
    }
}
