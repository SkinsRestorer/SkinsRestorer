/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.commands;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.properties.Property;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ComponentHelper;
import net.skinsrestorer.shared.utils.SRConstants;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@CommandNames("skin")
@Description(Message.HELP_SKIN)
@CommandPermission(value = PermissionRegistry.SKIN)
@CommandConditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SkinCommand {
    private final SRPlatformAdapter<?, ?> adapter;
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
    private final RecommendationsState recommendationsState;

    @RootCommand
    private void onDefault(SRCommandSender sender) {
        if (settings.getProperty(CommandConfig.CUSTOM_HELP_ENABLED)) {
            for (String line : settings.getProperty(CommandConfig.CUSTOM_HELP_MESSAGE)) {
                sender.sendMessage(ComponentHelper.parseMiniMessageToJsonString(line));
            }
            return;
        }

        commandManager.getHelpMessage("skin", sender).forEach(sender::sendMessage);
    }

    @Subcommand("help")
    @Description(Message.HELP_SKIN_HELP)
    @CommandPermission(PermissionRegistry.SKIN_SET)
    private void onSkinHelp(SRCommandSender sender) {
        onDefault(sender);
    }

    @RootCommand
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSetShort(SRPlayer player, String skinName) {
        onSkinSetOther(player, skinName, player, null);
    }

    @Subcommand({"clear", "reset"})
    @CommandPermission(PermissionRegistry.SKIN_CLEAR)
    @Description(Message.HELP_SKIN_CLEAR)
    @CommandConditions("cooldown")
    private void onSkinClear(SRPlayer player) {
        onSkinClearOther(player, player);
    }

    @Subcommand({"clear", "reset"})
    @CommandPermission(PermissionRegistry.SKIN_CLEAR_OTHER)
    @Description(Message.HELP_SKIN_CLEAR_OTHER)
    @CommandConditions("cooldown")
    private void onSkinClearOther(SRCommandSender sender, SRPlayer target) {
        // Remove the targets defined skin from database
        playerStorage.removeSkinIdOfPlayer(target.getUniqueId());

        try {
            Optional<SkinProperty> property = playerStorage.getSkinForPlayer(target.getUniqueId(), target.getName());
            skinApplier.applySkin(target.getAs(Object.class), property.orElse(SRConstants.EMPTY_SKIN));

            if (senderEqual(sender, target)) {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR);
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_CLEAR_OTHER, Placeholder.unparsed("name", target.getName()));
            }
        } catch (DataRequestException e) {
            logger.severe("Error while clearing skin", e);
            sender.sendMessage(Message.ERROR_UPDATING_SKIN); // TODO: Better error message
        }
    }

    @Subcommand("random")
    @CommandPermission(PermissionRegistry.SKIN_RANDOM)
    @Description(Message.HELP_SKIN_RANDOM)
    @CommandConditions("cooldown")
    private void onSkinRandom(SRPlayer player) {
        onSkinRandomOther(player, player);
    }

    @Subcommand("random")
    @CommandPermission(PermissionRegistry.SKIN_RANDOM_OTHER)
    @Description(Message.HELP_SKIN_RANDOM_OTHER)
    @CommandConditions("cooldown")
    private void onSkinRandomOther(SRCommandSender sender, SRPlayer target) {
        onSkinSetOther(sender, SkinStorageImpl.RECOMMENDATION_PREFIX + recommendationsState.getRandomRecommendation().getSkinId(), target);
    }

    @Subcommand("search")
    @CommandPermission(PermissionRegistry.SKIN_SEARCH)
    @Description(Message.HELP_SKIN_SEARCH)
    @CommandConditions("cooldown")
    private void onSkinSearch(SRCommandSender sender, String searchString) {
        sender.sendMessage(Message.SKIN_SEARCH_MESSAGE, Placeholder.unparsed("search", searchString));
    }

    @Subcommand({"update", "refresh"})
    @CommandPermission(PermissionRegistry.SKIN_UPDATE)
    @Description(Message.HELP_SKIN_UPDATE)
    @CommandConditions("cooldown")
    private void onSkinUpdate(SRPlayer player) {
        onSkinUpdateOther(player, player);
    }

    @Subcommand({"update", "refresh"})
    @CommandPermission(PermissionRegistry.SKIN_UPDATE_OTHER)
    @Description(Message.HELP_SKIN_UPDATE_OTHER)
    @CommandConditions("cooldown")
    private void onSkinUpdateOther(SRCommandSender sender, SRPlayer target) {
        try {
            Optional<SkinIdentifier> currentSkin = playerStorage.getSkinIdForPlayer(target.getUniqueId(), target.getName());
            if (currentSkin.isPresent() && currentSkin.get().getSkinType() == SkinType.PLAYER) {
                if (skinStorage.updatePlayerSkinData(UUID.fromString(currentSkin.get().getIdentifier())).isEmpty()) {
                    sender.sendMessage(Message.ERROR_UPDATING_SKIN);
                    return;
                }
            }

            Optional<SkinProperty> newSkin = currentSkin.isEmpty() ?
                    Optional.empty() : playerStorage.getSkinForPlayer(target.getUniqueId(), target.getName());

            skinApplier.applySkin(target.getAs(Object.class), newSkin.orElse(SRConstants.EMPTY_SKIN));

            if (senderEqual(sender, target)) {
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN);
            } else {
                sender.sendMessage(Message.SUCCESS_UPDATING_SKIN_OTHER, Placeholder.unparsed("name", target.getName()));
            }

            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
            setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
        }
    }

    @Subcommand({"set", "select"})
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @Description(Message.HELP_SKIN_SET)
    @CommandConditions("cooldown")
    private void onSkinSet(SRPlayer player, String skinName) {
        onSkinSetOther(player, skinName, player);
    }

    @Subcommand({"set", "select"})
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @Description(Message.HELP_SKIN_SET_OTHER)
    @CommandConditions("cooldown")
    private void onSkinSetOther(SRCommandSender sender, String skinName, SRPlayer target) {
        onSkinSetOther(sender, skinName, target, null);
    }

    @Subcommand({"set", "select"})
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @Description(Message.HELP_SKIN_SET_OTHER)
    @CommandConditions("cooldown")
    private void onSkinSetOther(SRCommandSender sender, String skinName, SRPlayer target, SkinVariant skinVariant) {
        if (!setSkin(sender, target, skinName, skinVariant)) {
            setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
            return;
        }

        if (senderEqual(sender, target)) {
            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE,
                    Placeholder.unparsed("skin", skinName));
        } else {
            sender.sendMessage(Message.SUCCESS_SKIN_CHANGE_OTHER,
                    Placeholder.unparsed("name", target.getName()),
                    Placeholder.unparsed("skin", skinName));
        }
    }

    @Subcommand("url")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @Description(Message.HELP_SKIN_SET_URL)
    @CommandConditions("cooldown")
    private void onSkinSetUrlShort(SRPlayer player, String url) {
        if (!ValidationUtil.validSkinUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, url, player, null);
    }

    @Subcommand("url")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @Description(Message.HELP_SKIN_SET_URL)
    @CommandConditions("cooldown")
    private void onSkinSetUrl(SRPlayer player, String url, SkinVariant skinVariant) {
        if (!ValidationUtil.validSkinUrl(url)) {
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
        if (!canSetSkin(sender, skinInput)) {
            return false;
        }

        try {
            // Perform skin lookup, which causes a second url regex check, but we don't care
            Optional<InputDataResult> optional = skinStorage.findOrCreateSkinData(skinInput, skinVariant);

            if (optional.isEmpty()) {
                sender.sendMessage(Message.NOT_PREMIUM); // TODO: Is this the right message?
                return false;
            }

            playerStorage.setSkinIdOfPlayer(target.getUniqueId(), optional.get().getIdentifier());
            skinApplier.applySkin(target.getAs(Object.class), optional.get().getProperty());

            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

            return true;
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        } catch (MineSkinException e) {
            logger.debug(SRLogLevel.SEVERE, String.format("Could not generate skin url: %s", skinInput), e);
            sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
        }

        return false;
    }

    private boolean canSetSkin(SRCommandSender sender, String skinInput) {
        if (settings.getProperty(CommandConfig.PER_SKIN_PERMISSIONS)
                && !sender.hasPermission(PermissionRegistry.forSkin(skinInput.toLowerCase(Locale.ROOT)))
                && (!sender.hasPermission(PermissionRegistry.OWN_SKIN)
                || !(sender instanceof SRPlayer player)
                || !skinInput.equalsIgnoreCase(player.getName()))) {
            sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_SKIN);
            return false;
        }

        if (isDisabledSkin(skinInput) && !sender.hasPermission(PermissionRegistry.BYPASS_DISABLED)) {
            sender.sendMessage(Message.ERROR_SKIN_DISABLED);
            return false;
        }

        if (ValidationUtil.validSkinUrl(skinInput)) {
            if (!sender.hasPermission(PermissionRegistry.SKIN_SET_URL)) {
                sender.sendMessage(Message.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!allowedSkinUrl(skinInput)) {
                sender.sendMessage(Message.ERROR_SKINURL_DISALLOWED);
                return false;
            }

            sender.sendMessage(Message.MS_UPLOADING_SKIN);
        }

        return true;
    }

    private void setCoolDown(SRCommandSender sender, Property<Integer> time) {
        if (sender instanceof SRPlayer player) {
            UUID senderUUID = player.getUniqueId();
            cooldownStorage.setCooldown(senderUUID, settings.getProperty(time), TimeUnit.SECONDS);
        }
    }

    private boolean isDisabledSkin(String skinName) {
        return settings.getProperty(CommandConfig.DISABLED_SKINS_ENABLED)
                && settings.getProperty(CommandConfig.DISABLED_SKINS).stream().anyMatch(skinName::equalsIgnoreCase);
    }

    private boolean allowedSkinUrl(String url) {
        return !settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_ENABLED)
                || settings.getProperty(CommandConfig.RESTRICT_SKIN_URLS_LIST)
                .stream()
                .anyMatch(url::startsWith);
    }

    private boolean senderEqual(SRCommandSender sender, SRCommandSender other) {
        if (sender instanceof SRPlayer player && other instanceof SRPlayer otherPlayer) {
            // Player == Player
            return player.getUniqueId().equals(otherPlayer.getUniqueId());
        } else {
            // Console == Console
            return !(sender instanceof SRPlayer) && !(other instanceof SRPlayer);
        }
    }
}
