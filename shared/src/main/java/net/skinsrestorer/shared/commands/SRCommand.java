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
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.model.MojangProfileTextureMeta;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.shared.commands.library.CommandHelpService;
import net.skinsrestorer.shared.commands.library.PlayerSelector;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.commands.library.SRCommandService;
import net.skinsrestorer.shared.commands.library.annotations.CommandDescription;
import net.skinsrestorer.shared.commands.library.annotations.CommandPermission;
import net.skinsrestorer.shared.commands.library.annotations.ConsoleOnly;
import net.skinsrestorer.shared.commands.library.annotations.RootDescription;
import net.skinsrestorer.shared.config.StorageConfig;
import net.skinsrestorer.shared.connections.DumpService;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.HardcodedSkins;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.storage.adapter.AdapterReference;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;
import net.skinsrestorer.shared.storage.model.skin.CustomSkinData;
import net.skinsrestorer.shared.storage.model.skin.PlayerSkinData;
import net.skinsrestorer.shared.storage.model.skin.URLIndexData;
import net.skinsrestorer.shared.storage.model.skin.URLSkinData;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.ComponentHelper;
import net.skinsrestorer.shared.subjects.messages.ComponentString;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.Permission;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ExpiringSet;
import net.skinsrestorer.shared.utils.SRHelpers;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Command("sr|skinsrestorer")
@RootDescription(Message.HELP_SR)
@SuppressWarnings("unused")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand {
    private final ExpiringSet<UUID> quotesHelpCache = new ExpiringSet<>(5, TimeUnit.MINUTES);
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final ServiceCheckerService serviceCheckerService;
    private final PlayerStorage playerStorage;
    private final CacheStorage cacheStorage;
    private final SkinStorageImpl skinStorage;
    private final AdapterReference adapterReference;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final DumpService dumpService;
    private final SkinsRestorer skinsRestorer;
    private final MineSkinAPI mineSkinAPI;
    private final SkinApplier<Object> skinApplier;
    private final Injector injector;
    private final SkinsRestorerLocale locale;
    private final SRCommandManager commandManager;
    private final CommandHelpService helpService;
    private final SRCommandService srCommandService;

    @Command("")
    @CommandPermission(PermissionRegistry.SR)
    public void rootCommand(SRCommandSender sender) {
        helpService.sendRootHelp(sender, "sr");
    }

    @Suggestions("help_queries_sr")
    public List<String> suggestHelpQueries(CommandContext<SRCommandSender> ctx, String input) {
        return helpService.suggestHelpQueries(ctx.sender(), "sr");
    }

    @Command("help [query]")
    @CommandPermission(PermissionRegistry.SR)
    @CommandDescription(Message.HELP_SR)
    public void commandHelp(SRCommandSender sender, @Argument(suggestions = "help_queries_sr") @Greedy String query) {
        helpService.sendQueryHelp(sender, "sr", query);
    }

    @Command("reload")
    @CommandPermission(PermissionRegistry.SR_RELOAD)
    @CommandDescription(Message.HELP_SR_RELOAD)
    private void onReload(SRCommandSender sender) {
        plugin.loadConfig();
        try {
            plugin.loadLocales();
        } catch (IOException e) {
            logger.severe("Failed to load locales", e);
        }

        try {
            plugin.loadStorage();
        } catch (InitializeException e) {
            logger.severe("Failed to load storage", e);
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_RELOAD);
    }

    @Command("docs permissions")
    @CommandPermission(PermissionRegistry.SR)
    private void onDocsPermissions(SRCommandSender sender) {
        for (PermissionRegistry permission : PermissionRegistry.values()) {
            sender.sendMessage(ComponentHelper.convertComponentToJson(Component.text("| `%s` | %s |".formatted(
                    permission.getPermission().getPermissionString(),
                    ComponentHelper.convertComponentToPlain(ComponentHelper.convertJsonToComponent(locale.getMessageRequired(sender, permission.getDescription())))
            ))));
        }

        for (PermissionGroup group : PermissionGroup.VALUES) {
            sender.sendMessage(ComponentHelper.convertComponentToJson(Component.text("| `%s` / `%s` | %s | %s |".formatted(
                    group.getBasePermission().getPermissionString(),
                    group.getWildcard().getPermissionString(),
                    ComponentHelper.convertComponentToPlain(ComponentHelper.convertJsonToComponent(locale.getMessageRequired(sender, group.getDescription()))),
                    Stream.concat(Arrays.stream(group.getParents()).map(PermissionGroup::getBasePermission), Arrays.stream(group.getPermissions()).map(PermissionRegistry::getPermission))
                            .map(Permission::getPermissionString).map("`%s`"::formatted).collect(Collectors.joining(", "))
            ))));
        }
    }

    @Command("status")
    @CommandPermission(PermissionRegistry.SR_STATUS)
    @CommandDescription(Message.HELP_SR_STATUS)
    private void onStatus(SRCommandSender sender) {
        srCommandService.executeStatus(sender);
    }

    @Command("drop|remove player <target>")
    @CommandPermission(PermissionRegistry.SR_DROP)
    @CommandDescription(Message.HELP_SR_DROP)
    private void onDropPlayer(SRCommandSender sender, String target) {
        try {
            Optional<UUID> targetId = cacheStorage.getUUID(target, false);

            if (targetId.isEmpty()) {
                sender.sendMessage(Message.ADMINCOMMAND_DROP_PLAYER_NOT_FOUND, Placeholder.unparsed("player", target));
                return;
            }

            playerStorage.removeSkinIdOfPlayer(targetId.get());
        } catch (DataRequestException e) {
            sender.sendMessage(Message.ADMINCOMMAND_DROP_UUID_ERROR);
            return;
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_DROP, Placeholder.unparsed("type", "player"), Placeholder.unparsed("target", target));
    }

    @Command("drop|remove skin <target>")
    @CommandPermission(PermissionRegistry.SR_DROP)
    @CommandDescription(Message.HELP_SR_DROP)
    private void onDropSkin(SRCommandSender sender, String target) {
        Optional<InputDataResult> optional = skinStorage.findSkinData(target);

        if (optional.isEmpty()) {
            sender.sendMessage(Message.ADMINCOMMAND_DROP_SKIN_NOT_FOUND, Placeholder.unparsed("skin", target));
            return;
        }

        InputDataResult result = optional.get();

        skinStorage.removeSkinData(result.getIdentifier());

        sender.sendMessage(Message.SUCCESS_ADMIN_DROP, Placeholder.unparsed("type", "skin"), Placeholder.unparsed("target", target));
    }

    @Command("info|props|lookup player <input>")
    @CommandPermission(PermissionRegistry.SR_INFO)
    @CommandDescription(Message.HELP_SR_INFO)
    private void onInfoPlayer(SRCommandSender sender, String input) {
        sender.sendMessage(Message.ADMINCOMMAND_INFO_CHECKING);
        sender.sendMessage(Message.DIVIDER);
        getPlayerInfoMessage(input).accept(sender);
        sender.sendMessage(Message.DIVIDER);
    }

    @Command("info|props|lookup skin <input>")
    @CommandPermission(PermissionRegistry.SR_INFO)
    @CommandDescription(Message.HELP_SR_INFO)
    private void onInfoSkin(SRCommandSender sender, String input) {
        try {
            Consumer<SRCommandSender> message = getSkinInfoMessage(input);

            sender.sendMessage(Message.ADMINCOMMAND_INFO_CHECKING);
            sender.sendMessage(Message.DIVIDER);
            message.accept(sender);
            sender.sendMessage(Message.DIVIDER);
        } catch (StorageAdapter.StorageException | DataRequestException e) {
            logger.severe("Failed to get data", e);
            sender.sendMessage(Message.ERROR_GENERIC);
        }
    }

    private Consumer<SRCommandSender> getPlayerInfoMessage(String input) {
        Optional<UUID> parsedUniqueId = UUIDUtils.tryParseUniqueId(input);
        if (parsedUniqueId.isEmpty()) {
            return sender -> sender.sendMessage(Message.ADMINCOMMAND_INFO_INVALID_UUID);
        }

        Optional<SkinIdentifier> playerSkinData = playerStorage.getSkinIdOfPlayer(parsedUniqueId.get());

        return playerSkinData.<Consumer<SRCommandSender>>map(skinData -> sender -> sender.sendMessage(Message.ADMINCOMMAND_INFO_PLAYER,
                Placeholder.parsed("uuid", parsedUniqueId.get().toString()),
                Placeholder.parsed("identifier", skinData.getIdentifier()),
                Placeholder.parsed("variant", String.valueOf(skinData.getSkinVariant())),
                Placeholder.parsed("type", skinData.getSkinType().toString()))).orElseGet(() -> sender -> sender.sendMessage(Message.ADMINCOMMAND_INFO_NO_SET_SKIN));
    }

    private void sendGenericSkinInfoMessage(SRCommandSender sender, SkinProperty property) {
        MojangProfileResponse profile = PropertyUtils.getSkinProfileData(property);
        String texturesUrl = PropertyUtils.getSkinTextureUrl(property);
        SkinVariant variant = PropertyUtils.getSkinVariant(property);
        MojangProfileTextureMeta skinMetadata = profile.getTextures().getSKIN().getMetadata();

        long timestamp = profile.getTimestamp();
        String requestTime = SRHelpers.formatEpochMillis(settings, timestamp, sender.getLocale());

        sender.sendMessage(Message.ADMINCOMMAND_INFO_GENERIC,
                Placeholder.parsed("url", texturesUrl),
                Placeholder.parsed("variant", variant.name().toLowerCase(Locale.ROOT)),
                Placeholder.parsed("uuid", UUIDUtils.convertToDashed(profile.getProfileId()).toString()),
                Placeholder.parsed("name", profile.getProfileName()),
                Placeholder.parsed("request_time", requestTime));
    }

    private Consumer<SRCommandSender> getSkinInfoMessage(String input) throws StorageAdapter.StorageException, DataRequestException {
        if (ValidationUtil.validSkinUrl(input)) {
            Optional<URLIndexData> urlSkinIndex = adapterReference.get().getURLSkinIndex(input);

            if (urlSkinIndex.isEmpty()) {
                return sender -> sender.sendMessage(Message.NO_SKIN_DATA);
            }

            Optional<URLSkinData> urlSkinData = adapterReference.get().getURLSkinData(input, urlSkinIndex.get().getSkinVariant());
            return urlSkinData.<Consumer<SRCommandSender>>map(skinData -> sender -> {
                sender.sendMessage(Message.ADMINCOMMAND_INFO_URL_SKIN,
                        Placeholder.parsed("url", input),
                        Placeholder.parsed("mine_skin_id", urlSkinData.get().getMineSkinId()));
                sendGenericSkinInfoMessage(sender, skinData.getProperty());
            }).orElseGet(() -> sender -> sender.sendMessage(Message.NO_SKIN_DATA));
        } else {
            Optional<InputDataResult> result = HardcodedSkins.getHardcodedSkin(input);

            if (result.isPresent()) {
                return sender -> {
                    sender.sendMessage(Message.ADMINCOMMAND_INFO_HARDCODED_SKIN,
                            Placeholder.parsed("skin", result.get().getIdentifier().getIdentifier()));
                    sendGenericSkinInfoMessage(sender, result.get().getProperty());
                };
            }

            Optional<CustomSkinData> customSkinData = adapterReference.get().getCustomSkinData(input);

            if (customSkinData.isPresent()) {
                return sender -> {
                    sender.sendMessage(Message.ADMINCOMMAND_INFO_CUSTOM_SKIN,
                            Placeholder.parsed("skin", input));
                    sendGenericSkinInfoMessage(sender, customSkinData.get().getProperty());
                };
            }

            Optional<UUID> uuid = cacheStorage.getUUID(input, false);

            if (uuid.isEmpty()) {
                return sender -> sender.sendMessage(Message.NO_SKIN_DATA);
            }

            Optional<PlayerSkinData> playerSkinData = adapterReference.get().getPlayerSkinData(uuid.get());

            return playerSkinData.<Consumer<SRCommandSender>>map(skinData -> sender -> {
                sender.sendMessage(Message.ADMINCOMMAND_INFO_PLAYER_SKIN,
                        Placeholder.parsed("skin", input),
                        Placeholder.parsed("timestamp", SRHelpers.formatEpochSeconds(settings, playerSkinData.get().getTimestamp(), sender.getLocale())),
                        Placeholder.parsed("expires", SRHelpers.formatEpochSeconds(settings, playerSkinData.get().getTimestamp()
                                + TimeUnit.MINUTES.toSeconds(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER)), sender.getLocale())));
                sendGenericSkinInfoMessage(sender, skinData.getProperty());
            }).orElseGet(() -> sender -> sender.sendMessage(Message.NO_SKIN_DATA));
        }
    }

    @Command("applyskin <selector>")
    @CommandPermission(PermissionRegistry.SR_APPLY_SKIN)
    @CommandDescription(Message.HELP_SR_APPLY_SKIN)
    private void onApplySkin(SRCommandSender sender, PlayerSelector selector) {
        for (UUID target : selector.resolve(sender)) {
            Optional<SRPlayer> targetPlayer = adapter.getPlayer(sender, target);
            if (targetPlayer.isEmpty()) {
                continue;
            }

            try {
                skinApplier.applySkin(targetPlayer.get().getAs(Object.class));
                sender.sendMessage(Message.SUCCESS_ADMIN_APPLYSKIN, Placeholder.unparsed("player", targetPlayer.get().getName()));
            } catch (DataRequestException e) {
                ComponentHelper.sendException(e, sender, locale, logger);
            }
        }
    }

    @Command("createcustom <skinName> <skinInput>")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @CommandDescription(Message.HELP_SR_CREATE_CUSTOM)
    private void onCreateCustom(SRCommandSender sender, String skinName, @Quoted String skinInput) {
        createCustom(sender, skinName, skinInput, null);
    }

    @Command("createcustom <skinName> <skinInput> <skinVariant>")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @CommandDescription(Message.HELP_SR_CREATE_CUSTOM)
    private void onCreateCustom(SRCommandSender sender, String skinName, @Quoted String skinInput, SkinVariant skinVariant) {
        createCustom(sender, skinName, skinInput, skinVariant);
    }

    private void createCustom(SRCommandSender sender, String skinName, @Quoted String skinInput, SkinVariant skinVariant) {
        try {
            Optional<InputDataResult> response = skinStorage.findOrCreateSkinData(skinInput, skinVariant);
            if (response.isEmpty()) {
                sender.sendMessage(Message.NOT_PREMIUM); // TODO: Is this the right message?
                return;
            }

            skinStorage.setCustomSkinData(skinName, response.get().getProperty());
            sender.sendMessage(Message.SUCCESS_ADMIN_CREATECUSTOM, Placeholder.unparsed("skin", skinName));
        } catch (DataRequestException | MineSkinException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        }
    }

    @Command("setcustomname <skinName> <displayName>")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @CommandDescription(Message.HELP_SR_CREATE_CUSTOM)
    private void onSetCustomName(SRCommandSender sender, String skinName, @Greedy String displayName) {
        try {
            ComponentString componentString = ComponentHelper.parseMiniMessageToJsonString(displayName);
            skinStorage.setCustomSkinDisplayName(skinName, componentString);
            sender.sendMessage(Message.SUCCESS_ADMIN_SETCUSTOMNAME,
                    Placeholder.unparsed("skin", skinName),
                    Placeholder.component("display_name", ComponentHelper.convertJsonToComponent(componentString)));
        } catch (StorageAdapter.StorageException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        }
    }

    @ConsoleOnly
    @Command("purgeolddata <days>")
    @CommandPermission(PermissionRegistry.SR_PURGE_OLD_DATA)
    @CommandDescription(Message.HELP_SR_PURGE_OLD_DATA)
    private void onPurgeOldData(SRCommandSender sender, int days) {
        if (skinStorage.purgeOldSkins(days)) {
            sender.sendMessage(Message.ADMINCOMMAND_PURGEOLDDATA_SUCCESS);
        } else {
            sender.sendMessage(Message.ADMINCOMMAND_PURGEOLDDATA_ERROR);
        }
    }

    @Command("dump")
    @CommandPermission(PermissionRegistry.SR_DUMP)
    @CommandDescription(Message.HELP_SR_DUMP)
    private void onDump(SRCommandSender sender) {
        srCommandService.executeDump(sender);
    }
}
