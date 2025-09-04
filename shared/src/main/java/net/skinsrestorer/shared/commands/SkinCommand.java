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
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.*;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.commands.library.annotations.SRCooldownGroup;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.storage.PlayerStorageImpl;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.model.player.FavouriteData;
import net.skinsrestorer.shared.storage.model.player.HistoryData;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.subjects.permissions.SkinPermissionManager;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.ValidationUtil;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.help.result.CommandEntry;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.processors.cooldown.CooldownGroup;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
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
    private final SkinStorageImpl skinStorage;
    private final PlayerStorageImpl playerStorage;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SharedSkinApplier<Object> skinApplier;
    private final MineSkinAPI mineSkinAPI;
    private final SRCommandManager commandManager;
    private final RecommendationsState recommendationsState;
    private final SkinPermissionManager permissionManager;
    private final MetricsCounter metricsCounter;

    @Command("")
    @CommandPermission(PermissionRegistry.SKIN)
    public void rootCommand(SRCommandSender sender) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_ROOT_HELP);
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
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_HELP);
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
                .build()
                .queryCommands(query == null ? "" : query, sender);
    }

    @Command("<skinName>")
    @CommandPermission(PermissionRegistry.SKIN_SET)
    @CommandDescription(Message.HELP_SKIN_SET)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetShort(SRPlayer player, @Quoted String skinName) {
        onSkinSetOther(player, skinName, PlayerSelector.singleton(player), null);
    }

    @Command("<skinName> <selector>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetShortOther(SRPlayer player, @Quoted String skinName, PlayerSelector selector) {
        onSkinSetOther(player, skinName, selector, null);
    }

    @Command("clear|reset")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR)
    @CommandDescription(Message.HELP_SKIN_CLEAR)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinClear(SRPlayer player) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_CLEAR);
        onSkinClearOther(player, PlayerSelector.singleton(player));
    }

    @Command("clear|reset <selector>")
    @CommandPermission(PermissionRegistry.SKIN_CLEAR_OTHER)
    @CommandDescription(Message.HELP_SKIN_CLEAR_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinClearOther(SRCommandSender sender, PlayerSelector selector) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_CLEAR);
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            // Remove the targets defined skin from database
            playerStorage.removeSkinIdOfPlayer(target);

            try {
                if (targetPlayer.isPresent()) {
                    Optional<SkinProperty> property = playerStorage.getSkinForPlayer(target, targetPlayer.get().getName());
                    skinApplier.applySkin(targetPlayer.get().getAs(Object.class), property.orElse(PropertyUtils.EMPTY_SKIN));
                }

                if (senderEqual(sender, target)) {
                    sender.sendMessage(Message.SUCCESS_SKIN_CLEAR);
                } else {
                    sender.sendMessage(Message.SUCCESS_SKIN_CLEAR_OTHER, Placeholder.unparsed("name", targetName));
                }
                setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);
            } catch (DataRequestException e) {
                logger.severe("Error while clearing skin", e);
                sender.sendMessage(Message.ERROR_UPDATING_SKIN); // TODO: Better error message
                setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
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
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_RANDOM);
        Optional<RecommenationResponse.SkinInfo> randomRecommendation = recommendationsState.getRandomRecommendation();
        if (randomRecommendation.isEmpty()) {
            logger.warning("No random skins available, skipping");
            return;
        }

        onSkinSetOther(sender, SkinStorageImpl.RECOMMENDATION_PREFIX + randomRecommendation.get().getSkinId(), selector);
    }

    @Command("search <searchString>")
    @CommandPermission(PermissionRegistry.SKIN_SEARCH)
    @CommandDescription(Message.HELP_SKIN_SEARCH)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSearch(SRCommandSender sender, @Greedy String searchString) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_SEARCH);
        sender.sendMessage(Message.SKIN_SEARCH_MESSAGE, Placeholder.unparsed("search", searchString));
    }

    @Command("edit")
    @CommandPermission(PermissionRegistry.SKIN_EDIT)
    @CommandDescription(Message.HELP_SKIN_EDIT)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinEdit(SRPlayer player) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_EDIT);
        player.sendMessage(Message.SKIN_EDIT_MESSAGE,
                Placeholder.parsed("url", "https://minecraft.novaskin.me/?skin=%s".formatted(
                        PropertyUtils.getSkinTextureUrl(adapter.getSkinProperty(player).orElse(HardcodedSkins.STEVE.getProperty())))));
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
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_UPDATE);
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            try {
                Optional<SkinIdentifier> currentSkin = targetPlayer.isPresent() ? playerStorage.getSkinIdForPlayer(target, targetPlayer.get().getName())
                        : playerStorage.getSkinIdOfPlayer(target);

                if (currentSkin.isPresent()) {
                    var skin = currentSkin.get();
                    if (skin.getSkinType() == SkinType.PLAYER
                            && skinStorage.updatePlayerSkinData(skin.getPlayerUniqueId()).isEmpty()) {

                        sender.sendMessage(Message.ERROR_UPDATING_SKIN);
                        return;
                    }
                }

                if (targetPlayer.isPresent()) {
                    Optional<SkinProperty> newSkin = currentSkin.isEmpty() ?
                            Optional.empty() : playerStorage.getSkinForPlayer(target, targetPlayer.get().getName());

                    skinApplier.applySkin(targetPlayer.get().getAs(Object.class), newSkin.orElse(PropertyUtils.EMPTY_SKIN));
                }

                if (senderEqual(sender, target)) {
                    sender.sendMessage(Message.SUCCESS_UPDATING_SKIN);
                } else {
                    sender.sendMessage(Message.SUCCESS_UPDATING_SKIN_OTHER, Placeholder.unparsed("name", targetName));
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
    private void onSkinSet(SRPlayer player, @Quoted String skinName) {
        onSkinSetOther(player, skinName, PlayerSelector.singleton(player));
    }

    @Command("set|select <skinName> <selector>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetOther(SRCommandSender sender, @Quoted String skinName, PlayerSelector selector) {
        onSkinSetOther(sender, skinName, selector, null);
    }

    @Command("set|select <skinName> <selector> <skinVariant>")
    @CommandPermission(PermissionRegistry.SKIN_SET_OTHER)
    @CommandDescription(Message.HELP_SKIN_SET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetOther(SRCommandSender sender, @Quoted String skinName, PlayerSelector selector, SkinVariant skinVariant) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_SET);
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            if (!setSkin(sender, target, skinName, skinVariant, true)) {
                return;
            }

            if (senderEqual(sender, target)) {
                sender.sendMessage(Message.SUCCESS_SKIN_CHANGE,
                        Placeholder.unparsed("skin", skinName));
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_CHANGE_OTHER,
                        Placeholder.unparsed("name", targetName),
                        Placeholder.unparsed("skin", skinName));
            }
        }
    }

    @Command("url <url> [skinVariant]")
    @CommandPermission(PermissionRegistry.SKIN_SET_URL)
    @CommandDescription(Message.HELP_SKIN_SET_URL)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinSetUrl(SRPlayer player, @Quoted String url, @Nullable SkinVariant skinVariant) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_URL);
        if (!ValidationUtil.validSkinUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, url, PlayerSelector.singleton(player), skinVariant);
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
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_UNDO);
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            Optional<HistoryData> historyData = playerStorage.getTopOfHistory(target, 0);
            if (historyData.isEmpty()) {
                sender.sendMessage(Message.ERROR_NO_UNDO);
                return;
            }

            Optional<SkinIdentifier> currentSkin = playerStorage.getSkinIdOfPlayer(target);
            if (currentSkin.isPresent() && currentSkin.get().equals(historyData.get().getSkinIdentifier())) {
                // We need a different history entry to undo
                Optional<HistoryData> historyData2 = playerStorage.getTopOfHistory(target, 1);
                if (historyData2.isEmpty()) {
                    sender.sendMessage(Message.ERROR_NO_UNDO);
                    return;
                }

                // Remove the current skin from history
                playerStorage.removeFromHistory(target, historyData.get());

                historyData = historyData2;
            }

            if (!setSkin(sender, target, historyData.get().getSkinIdentifier().getIdentifier(), historyData.get().getSkinIdentifier().getSkinVariant(), false)) {
                return;
            }

            if (senderEqual(sender, target)) {
                sender.sendMessage(Message.SUCCESS_SKIN_UNDO,
                        Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(historyData.get().getSkinIdentifier()))),
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, historyData.get().getTimestamp(), sender.getLocale())));
            } else {
                sender.sendMessage(Message.SUCCESS_SKIN_UNDO_OTHER,
                        Placeholder.unparsed("name", targetName),
                        Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(historyData.get().getSkinIdentifier()))),
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, historyData.get().getTimestamp(), sender.getLocale())));
            }
        }
    }

    @Command("history")
    @CommandPermission(PermissionRegistry.SKIN_UNDO)
    @CommandDescription(Message.HELP_SKIN_UNDO)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinHistory(SRPlayer player) {
        onSkinHistoryOther(player, PlayerSelector.singleton(player));
    }

    @Command("history <selector>")
    @CommandPermission(PermissionRegistry.SKIN_UNDO_OTHER)
    @CommandDescription(Message.HELP_SKIN_UNDO_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinHistoryOther(SRCommandSender sender, PlayerSelector selector) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_HISTORY);
        for (UUID target : selector.resolve(sender)) {
            List<HistoryData> historyDataList = playerStorage.getHistoryEntries(target, 0, Integer.MAX_VALUE);
            if (historyDataList.isEmpty()) {
                sender.sendMessage(Message.ERROR_NO_HISTORY);
                return;
            }

            sender.sendMessage(Message.DIVIDER);
            for (HistoryData historyData : historyDataList) {
                sender.sendMessage(Message.SUCCESS_HISTORY_LINE,
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, historyData.getTimestamp(), sender.getLocale())),
                        Placeholder.parsed("skin_id", historyData.getSkinIdentifier().getIdentifier()),
                        Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(historyData.getSkinIdentifier()))));
            }
            sender.sendMessage(Message.DIVIDER);
        }
    }

    @Command("favourite")
    @CommandPermission(PermissionRegistry.SKIN_FAVOURITE)
    @CommandDescription(Message.HELP_SKIN_FAVOURITE)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinFavourite(SRPlayer player) {
        onSkinFavouriteOther(player, PlayerSelector.singleton(player));
    }

    @Command("favourite <selector>")
    @CommandPermission(PermissionRegistry.SKIN_FAVOURITE_OTHER)
    @CommandDescription(Message.HELP_SKIN_FAVOURITE_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinFavouriteOther(SRCommandSender sender, PlayerSelector selector) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_FAVOURITE);
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            Optional<SkinIdentifier> currentSkin = playerStorage.getSkinIdOfPlayer(target);
            if (currentSkin.isEmpty()) {
                sender.sendMessage(Message.ERROR_NO_SKIN_TO_FAVOURITE);
                return;
            }

            Optional<FavouriteData> favouriteData = playerStorage.getFavouriteData(target, currentSkin.get());
            if (favouriteData.isPresent()) {
                playerStorage.removeFavourite(target, favouriteData.get().getSkinIdentifier());
                if (senderEqual(sender, target)) {
                    sender.sendMessage(Message.SUCCESS_SKIN_UNFAVOURITE,
                            Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(currentSkin.get()))),
                            Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, favouriteData.get().getTimestamp(), sender.getLocale())));
                } else {
                    sender.sendMessage(Message.SUCCESS_SKIN_UNFAVOURITE_OTHER,
                            Placeholder.unparsed("name", targetName),
                            Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(currentSkin.get()))),
                            Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, favouriteData.get().getTimestamp(), sender.getLocale())));
                }
            } else {
                playerStorage.addFavourite(target, FavouriteData.of(SRHelpers.getEpochSecond(), currentSkin.get()));
                if (senderEqual(sender, target)) {
                    sender.sendMessage(Message.SUCCESS_SKIN_FAVOURITE,
                            Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(currentSkin.get()))));
                } else {
                    sender.sendMessage(Message.SUCCESS_SKIN_FAVOURITE_OTHER,
                            Placeholder.unparsed("name", targetName),
                            Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(currentSkin.get()))));
                }
            }
        }
    }

    @Command("favourites")
    @CommandPermission(PermissionRegistry.SKIN_FAVOURITE)
    @CommandDescription(Message.HELP_SKIN_FAVOURITE)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinFavourites(SRPlayer player) {
        onSkinFavouritesOther(player, PlayerSelector.singleton(player));
    }

    @Command("favourites <selector>")
    @CommandPermission(PermissionRegistry.SKIN_FAVOURITE_OTHER)
    @CommandDescription(Message.HELP_SKIN_FAVOURITE_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkinFavouritesOther(SRCommandSender sender, PlayerSelector selector) {
        metricsCounter.increment(MetricsCounter.CommandType.SKIN_FAVOURITES);
        for (UUID target : selector.resolve(sender)) {
            List<FavouriteData> favouriteDataList = playerStorage.getFavouriteEntries(target, 0, Integer.MAX_VALUE);
            if (favouriteDataList.isEmpty()) {
                sender.sendMessage(Message.ERROR_NO_HISTORY);
                return;
            }

            sender.sendMessage(Message.DIVIDER);
            for (FavouriteData favouriteData : favouriteDataList) {
                sender.sendMessage(Message.SUCCESS_HISTORY_LINE,
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, favouriteData.getTimestamp(), sender.getLocale())),
                        Placeholder.parsed("skin_id", favouriteData.getSkinIdentifier().getIdentifier()),
                        Placeholder.component("skin", ComponentHelper.convertJsonToComponent(skinStorage.resolveSkinName(favouriteData.getSkinIdentifier()))));
            }
            sender.sendMessage(Message.DIVIDER);
        }
    }

    @Command("menu|gui")
    @CommandPermission(PermissionRegistry.SKINS)
    private void onGUIShortcut(SRPlayer player) {
        commandManager.execute(player, "skins");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean setSkin(SRCommandSender sender, UUID target, String skinInput, SkinVariant skinVariant, boolean insertHistory) {
        Optional<Message> noPermissionMessage = permissionManager.canSetSkin(sender, skinInput);
        if (noPermissionMessage.isPresent()) {
            sender.sendMessage(noPermissionMessage.get());
            return false;
        }

        try {
            if (ValidationUtil.validSkinUrl(skinInput)) {
                sender.sendMessage(Message.MS_UPLOADING_SKIN);
            }

            // Perform skin lookup, which causes a second url regex check, but we don't care
            Optional<InputDataResult> optional = skinStorage.findOrCreateSkinData(skinInput, skinVariant);

            if (optional.isEmpty()) {
                sender.sendMessage(Message.NOT_PREMIUM); // TODO: Is this the right message?
                return false;
            }

            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            playerStorage.setSkinIdOfPlayer(target, optional.get().getIdentifier());
            targetPlayer.ifPresent(player -> skinApplier.applySkin(player.getAs(Object.class), optional.get().getProperty()));

            setCoolDown(sender, CommandConfig.SKIN_CHANGE_COOLDOWN);

            // If someone else sets your skin, it shouldn't be in your /skin undo
            if (insertHistory && targetPlayer.isPresent() && senderEqual(sender, target)) {
                playerStorage.pushToHistory(target, HistoryData.of(SRHelpers.getEpochSecond(), optional.get().getIdentifier()));
            }

            return true;
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        } catch (MineSkinException e) {
            logger.debug(SRLogLevel.SEVERE, "Could not generate skin url: %s".formatted(skinInput), e);
            sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
        }

        setCoolDown(sender, CommandConfig.SKIN_ERROR_COOLDOWN);
        return false;
    }

    private void setCoolDown(SRCommandSender sender, Property<Integer> time) {
        if (sender instanceof SRPlayer player) {
            commandManager.setCooldown(player, COOLDOWN_GROUP, Duration.of(settings.getProperty(time), TimeUnit.SECONDS.toChronoUnit()));
        }
    }

    private boolean senderEqual(SRCommandSender sender, UUID other) {
        if (sender instanceof SRPlayer player) {
            // Player == Player
            return player.getUniqueId().equals(other);
        } else {
            // Console != Player
            return false;
        }
    }
}
