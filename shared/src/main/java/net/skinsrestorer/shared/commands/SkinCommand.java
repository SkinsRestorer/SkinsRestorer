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
import net.skinsrestorer.api.storage.SkinStorage;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.commands.library.annotations.SRCooldownGroup;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.PlayerStorageImpl;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ComponentHelper;
import net.skinsrestorer.shared.utils.SRConstants;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.ValidationUtil;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.processors.cooldown.CooldownGroup;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Command("skin")
@RootDescription(Message.HELP_SKIN)
@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SkinCommand {
    public static final String COOLDOWN_GROUP_ID = "skin";
    public static final CooldownGroup COOLDOWN_GROUP = CooldownGroup.named(COOLDOWN_GROUP_ID);

    private final SRPlatformAdapter adapter;
    private final SRPlugin plugin;
    private final SettingsManager settings;
    private final SkinStorage skinStorage;
    private final PlayerStorageImpl playerStorage;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SharedSkinApplier<Object> skinApplier;
    private final MineSkinAPI mineSkinAPI;
    private final SRCommandManager commandManager;
    private final RecommendationsState recommendationsState;

    @Command("")
    @CommandPermission(PermissionRegistry.SKIN)
    public void rootCommand(SRCommandSender sender) {
        if (settings.getProperty(CommandConfig.CUSTOM_HELP_ENABLED)) {
            settings.getProperty(CommandConfig.CUSTOM_HELP_MESSAGE)
                    .forEach(l -> sender.sendMessage(ComponentHelper.parseMiniMessageToJsonString(l)));
            return;
        }

        MinecraftHelp.<SRCommandSender>builder()
                .commandManager(commandManager.getCommandManager())
                .audienceProvider(ComponentHelper::commandSenderToAudience)
                .commandPrefix("/skin help")
                .messageProvider(MinecraftHelp.captionMessageProvider(
                        commandManager.getCommandManager().captionRegistry(),
                        ComponentCaptionFormatter.miniMessage()
                ))
                .descriptionDecorator((s, d) -> ComponentHelper.convertJsonToComponent(locale.getMessageRequired(s, Message.fromKey(d).orElseThrow())))
                .commandFilter(c -> c.rootComponent().name().equals("skin") && !c.commandDescription().description().isEmpty())
                .maxResultsPerPage(Integer.MAX_VALUE)
                .build()
                .queryCommands("", sender);
    }

    @Suggestions("help_queries_skin")
    public List<String> suggestHelpQueries(CommandContext<SRCommandSender> ctx, String input) {
        return this.commandManager.getCommandManager()
                .createHelpHandler()
                .queryRootIndex(ctx.sender())
                .entries()
                .stream()
                .filter(e -> e.command().rootComponent().name().equals("skin"))
                .map(CommandEntry::syntax)
                .toList();
    }

    @Command("help [query]")
    @CommandPermission(PermissionRegistry.SKIN)
    @CommandDescription(Message.HELP_SKIN)
    public void commandHelp(SRCommandSender sender, @Argument(suggestions = "help_queries_skin") @Greedy String query) {
        MinecraftHelp.<SRCommandSender>builder()
                .commandManager(commandManager.getCommandManager())
                .audienceProvider(ComponentHelper::commandSenderToAudience)
                .commandPrefix("/skin help")
                .messageProvider(MinecraftHelp.captionMessageProvider(
                        commandManager.getCommandManager().captionRegistry(),
                        ComponentCaptionFormatter.miniMessage()
                ))
                .descriptionDecorator((s, d) -> ComponentHelper.convertJsonToComponent(locale.getMessageRequired(s, Message.fromKey(d).orElseThrow())))
                .commandFilter(c -> c.rootComponent().name().equals("skin") && !c.commandDescription().description().isEmpty())
                .build()
                .queryCommands(query == null ? "" : query, sender);
    }

    @Command("<skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @CommandDescription(Message.HELP_SKIN_SET)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetShort(SRPlayer player, @Greedy String skinName) {
        onSkinSetOther(player, PlayerSelector.singleton(player), null, skinName);
    }

    @Command("<selector> <skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetShortOther(SRPlayer player, PlayerSelector selector, @Greedy String skinName) {
        onSkinSetOther(player, selector, null, skinName);
    }

    @Command("clear|reset")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR)
    @CommandDescription(Message.HELP_SKIN_CLEAR)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinClear(SRPlayer player) {
        onSkinClearOther(player, PlayerSelector.singleton(player));
    }

    @Command("clear|reset <selector>")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR_OTHER)
    @CommandDescription(Message.HELP_SKIN_CLEAR_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinClearOther(SRCommandSender sender, PlayerSelector selector) {
        for (SRPlayer target : selector.resolve(sender)) {
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
    }

    @Command("random")
    @CommandPermission(PermissionRegistry.SKIN_RANDOM)
    @CommandDescription(Message.HELP_SKIN_RANDOM)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinRandom(SRPlayer player) {
        onSkinRandomOther(player, PlayerSelector.singleton(player));
    }

    @Command("random <selector>")
    @CommandPermission(PermissionRegistry.SKIN_RANDOM_OTHER)
    @CommandDescription(Message.HELP_SKIN_RANDOM_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinRandomOther(SRCommandSender sender, PlayerSelector selector) {
        onSkinSetOther(sender, selector, SkinStorageImpl.RECOMMENDATION_PREFIX + recommendationsState.getRandomRecommendation().getSkinId());
    }

    @Command("search <searchString>")
    @CommandPermission(PermissionRegistry.SKIN_SEARCH)
    @CommandDescription(Message.HELP_SKIN_SEARCH)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSearch(SRCommandSender sender, @Greedy String searchString) {
        sender.sendMessage(Message.SKIN_SEARCH_MESSAGE, Placeholder.unparsed("search", searchString));
    }

    @Command("update|refresh")
    @CommandPermission(PermissionRegistry.SKIN_UPDATE)
    @CommandDescription(Message.HELP_SKIN_UPDATE)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinUpdate(SRPlayer player) {
        onSkinUpdateOther(player, PlayerSelector.singleton(player));
    }

    @Command("update|refresh <selector>")
    @CommandPermission(PermissionRegistry.SKIN_UPDATE_OTHER)
    @CommandDescription(Message.HELP_SKIN_UPDATE_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinUpdateOther(SRCommandSender sender, PlayerSelector selector) {
        for (SRPlayer target : selector.resolve(sender)) {
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
    }

    @Command("set|select <skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @CommandDescription(Message.HELP_SKIN_SET)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSet(SRPlayer player, @Greedy String skinName) {
        onSkinSetOther(player, PlayerSelector.singleton(player), skinName);
    }

    @Command("set|select <selector> <skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetOther(SRCommandSender sender, PlayerSelector selector, @Greedy String skinName) {
        onSkinSetOther(sender, selector, null, skinName);
    }

    @Command("set|select <selector> <skinVariant> <skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetOther(SRCommandSender sender, PlayerSelector selector, SkinVariant skinVariant, @Greedy String skinName) {
        for (SRPlayer target : selector.resolve(sender)) {
            if (!setSkin(sender, target, skinName, skinVariant, true)) {
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
    }

    @Command("url <url>")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @CommandDescription(Message.HELP_SKIN_SET_URL)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetUrlShort(SRPlayer player, @Greedy String url) {
        if (!ValidationUtil.validSkinUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, PlayerSelector.singleton(player), null, url);
    }

    @Command("url <skinVariant> <url>")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @CommandDescription(Message.HELP_SKIN_SET_URL)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetUrl(SRPlayer player, SkinVariant skinVariant, @Greedy String url) {
        if (!ValidationUtil.validSkinUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, PlayerSelector.singleton(player), skinVariant, url);
    }

    @Command("undo|revert")
    @CommandPermission(PermissionRegistry.SKIN_UNDO)
    @CommandDescription(Message.HELP_SKIN_UNDO)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinUndo(SRPlayer player) {
        onSkinUndoOther(player, PlayerSelector.singleton(player));
    }

    @Command("undo|revert <selector>")
    @CommandPermission(PermissionRegistry.SKIN_UNDO_OTHER)
    @CommandDescription(Message.HELP_SKIN_UNDO_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinUndoOther(SRCommandSender sender, PlayerSelector selector) {
        for (SRPlayer target : selector.resolve(sender)) {
            Optional<HistoryData> historyData = playerStorage.getTopOfHistory(target.getUniqueId(), 0);
            if (historyData.isEmpty()) {
                sender.sendMessage(Message.ERROR_NO_UNDO);
                return;
            }

            Optional<SkinIdentifier> currentSkin = playerStorage.getSkinIdOfPlayer(target.getUniqueId());
            if (currentSkin.isPresent() && currentSkin.get().equals(historyData.get().getSkinIdentifier())) {
                // We need a different history entry to undo
                Optional<HistoryData> historyData2 = playerStorage.getTopOfHistory(target.getUniqueId(), 1);
                if (historyData2.isEmpty()) {
                    sender.sendMessage(Message.ERROR_NO_UNDO);
                    return;
                }

                // Remove the current skin from history
                playerStorage.removeFromHistory(target.getUniqueId(), historyData.get());

                historyData = historyData2;
            }

            if (!setSkin(sender, target, historyData.get().getSkinIdentifier().getIdentifier(), historyData.get().getSkinIdentifier().getSkinVariant(), false)) {
                return;
            }

            if (senderEqual(sender, target)) {
                sender.sendMessage(Message.SUCCESS_SKIN_UNDO,
                        Placeholder.unparsed("skin", historyData.get().getSkinIdentifier().getIdentifier()),
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(historyData.get().getTimestamp(), sender.getLocale())));
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_UNDO_OTHER,
                        Placeholder.unparsed("name", target.getName()),
                        Placeholder.unparsed("skin", historyData.get().getSkinIdentifier().getIdentifier()),
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(historyData.get().getTimestamp(), sender.getLocale())));
            }
        }
    }

    @Command("menu|gui")
    @CommandPermission(PermissionRegistry.SKINS)
    private void onGUIShortcut(SRPlayer player) {
        commandManager.execute(player, "skins");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean setSkin(SRCommandSender sender, SRPlayer target, String skinInput, SkinVariant skinVariant, boolean insertHistory) {
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

            // If someone else sets your skin, it shouldn't be in your /skin undo
            if (insertHistory && senderEqual(sender, target)) {
                playerStorage.pushToHistory(target.getUniqueId(), HistoryData.of(SRHelpers.getEpochSecond(), optional.get().getIdentifier()));
            }

            return true;
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        } catch (MineSkinException e) {
            logger.debug(SRLogLevel.SEVERE, String.format("Could not generate skin url: %s", skinInput), e);
            sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
        }

        setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
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
            commandManager.setCooldown(player, COOLDOWN_GROUP, Duration.of(settings.getProperty(time), TimeUnit.SECONDS.toChronoUnit()));
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
