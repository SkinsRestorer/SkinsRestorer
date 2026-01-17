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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.commands.library.CommandHelpService;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.commands.library.annotations.SRCooldownGroup;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.config.CommandConfig;
import net.skinsrestorer.shared.connections.RecommendationsState;
import net.skinsrestorer.shared.connections.responses.RecommenationResponse;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.storage.PlayerStorageImpl;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.subjects.permissions.SkinPermissionManager;
import net.skinsrestorer.shared.utils.ValidationUtil;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.processors.cooldown.CooldownGroup;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Command("skull")
@RootDescription(Message.HELP_SKULL)
@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SkullCommand {
    public static final String COOLDOWN_GROUP_ID = "skull";
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
    private final CommandHelpService helpService;

    @Command("")
    @CommandPermission(PermissionRegistry.SKULL)
    public void rootCommand(SRCommandSender sender) {
        helpService.sendRootHelp(sender, "skull");
    }

    @Suggestions("help_queries_skull")
    public List<String> suggestHelpQueries(CommandContext<SRCommandSender> ctx, String input) {
        return helpService.suggestHelpQueries(ctx.sender(), "skull");
    }

    @Command("help [query]")
    @CommandPermission(PermissionRegistry.SKULL)
    @CommandDescription(Message.HELP_SKULL)
    public void commandHelp(SRCommandSender sender, @Argument(suggestions = "help_queries_skull") @Greedy String query) {
        helpService.sendQueryHelp(sender, "skull", query);
    }

    @Command("<skinName>")
    @CommandPermission(PermissionRegistry.SKULL_GET)
    @CommandDescription(Message.HELP_SKULL_GET)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGetShort(SRPlayer player, @Quoted String skinName) {
        onSkullGetOther(player, skinName, PlayerSelector.singleton(player), null);
    }

    @Command("<skinName> <selector>")
    @CommandPermission(PermissionRegistry.SKULL_GET_OTHER)
    @CommandDescription(Message.HELP_SKULL_GET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGetShortOther(SRPlayer player, @Quoted String skinName, PlayerSelector selector) {
        onSkullGetOther(player, skinName, selector, null);
    }

    @Command("random")
    @CommandPermission(PermissionRegistry.SKULL_RANDOM)
    @CommandDescription(Message.HELP_SKULL_RANDOM)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullRandom(SRPlayer player) {
        onSkullRandomOther(player, PlayerSelector.singleton(player));
    }

    @Command("random <selector>")
    @CommandPermission(PermissionRegistry.SKULL_RANDOM_OTHER)
    @CommandDescription(Message.HELP_SKULL_RANDOM_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullRandomOther(SRCommandSender sender, PlayerSelector selector) {
        Optional<RecommenationResponse.SkinInfo> randomRecommendation = recommendationsState.getRandomRecommendation();
        if (randomRecommendation.isEmpty()) {
            logger.warning("No random skins available, skipping");
            return;
        }

        onSkullGetOther(sender, SkinStorageImpl.RECOMMENDATION_PREFIX + randomRecommendation.get().getSkinId(), selector);
    }

    @Command("get|give <skinName>")
    @CommandPermission(PermissionRegistry.SKULL_GET)
    @CommandDescription(Message.HELP_SKULL_GET)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGet(SRPlayer player, @Quoted String skinName) {
        onSkullGetOther(player, skinName, PlayerSelector.singleton(player));
    }

    @Command("get|give <skinName> <selector>")
    @CommandPermission(PermissionRegistry.SKULL_GET_OTHER)
    @CommandDescription(Message.HELP_SKULL_GET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGetOther(SRCommandSender sender, @Quoted String skinName, PlayerSelector selector) {
        onSkullGetOther(sender, skinName, selector, null);
    }

    @Command("get|give <skinName> <selector> <skinVariant>")
    @CommandPermission(PermissionRegistry.SKULL_GET_OTHER)
    @CommandDescription(Message.HELP_SKULL_GET_OTHER)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGetOther(SRCommandSender sender, @Quoted String skinName, PlayerSelector selector, SkinVariant skinVariant) {
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            String targetName = targetPlayer.map(SRPlayer::getName).orElseGet(target::toString);

            var givenSkull = giveSkull(sender, target, skinName, skinVariant);
            if (givenSkull.isEmpty()) {
                return;
            }

            sender.sendMessage(senderEqual(sender, target) ? Message.SUCCESS_SKULL_GET : Message.SUCCESS_SKULL_GET_OTHER,
                    Placeholder.unparsed("name", targetName),
                    Placeholder.unparsed("skin", skinName),
                    Placeholder.component("skin_head", AdvancedConfig.emptyIfPlayerHeadChatObjectsDisabled(settings, Component.object(ObjectContents.playerHead()
                            .name(targetName)
                            .profileProperty(PlayerHeadObjectContents.property(SkinProperty.TEXTURES_NAME, givenSkull.get().getValue(), givenSkull.get().getSignature()))
                            .build()))));
        }
    }

    @Command("url <url> [skinVariant]")
    @CommandPermission(PermissionRegistry.SKULL_GET_URL)
    @CommandDescription(Message.HELP_SKULL_GET_URL)
    @SRCooldownGroup(COOLDOWN_GROUP_ID)
    private void onSkullGetUrl(SRPlayer player, @Quoted String url, @Nullable SkinVariant skinVariant) {
        if (!ValidationUtil.validSkinUrl(url)) {
            player.sendMessage(Message.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkullGetOther(player, url, PlayerSelector.singleton(player), skinVariant);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private Optional<SkinProperty> giveSkull(SRCommandSender sender, UUID target, String skinInput, SkinVariant skinVariant) {
        Optional<Message> noPermissionMessage = permissionManager.canSetSkin(sender, skinInput);
        if (noPermissionMessage.isPresent()) {
            sender.sendMessage(noPermissionMessage.get());
            return Optional.empty();
        }

        try {
            if (ValidationUtil.validSkinUrl(skinInput)) {
                sender.sendMessage(Message.MS_UPLOADING_SKIN);
            }

            // Perform skin lookup, which causes a second url regex check, but we don't care
            Optional<InputDataResult> optional = skinStorage.findOrCreateSkinData(skinInput, skinVariant);

            if (optional.isEmpty()) {
                sender.sendMessage(Message.NOT_PREMIUM); // TODO: Is this the right message?
                return Optional.empty();
            }

            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            if (targetPlayer.isEmpty()) {
                // TODO: Send message
                return Optional.empty();
            }

            SkinIdentifier skinIdentifier = optional.get().getIdentifier();
            String itemName = switch (skinIdentifier.getSkinType()) {
                case PLAYER, LEGACY, CUSTOM -> skinInput;
                case URL -> "Custom skull";
            };
            adapter.giveSkullItem(targetPlayer.get(), new SRServerPluginMessage.GiveSkullChannelPayload(
                    ComponentHelper.convertPlainToJson(itemName),
                    PropertyUtils.getSkinTextureHash(optional.get().getProperty())
            ));

            setCoolDown(sender, CommandConfig.SKULL_GET_COOLDOWN);

            return Optional.of(optional.get().getProperty());
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        } catch (MineSkinException e) {
            logger.debug(SRLogLevel.SEVERE, "Could not generate skin url: %s".formatted(skinInput), e);
            sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
        }

        setCoolDown(sender, CommandConfig.SKULL_ERROR_COOLDOWN);
        return Optional.empty();
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
