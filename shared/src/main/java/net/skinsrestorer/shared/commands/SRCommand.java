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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.model.MojangProfileTextureMeta;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.DevConfig;
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
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ComponentHelper;
import net.skinsrestorer.shared.utils.SRConstants;
import net.skinsrestorer.shared.utils.UUIDUtils;
import net.skinsrestorer.shared.utils.ValidationUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@CommandNames({"sr", "skinsrestorer"})
@Description(Message.HELP_SR)
@CommandPermission(PermissionRegistry.SR)
@CommandConditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand {
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?, ?> adapter;
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
    private final CommandManager<SRCommandSender> commandManager;

    @RootCommand
    private void onDefault(SRCommandSender sender) {
        commandManager.getHelpMessage("sr", sender).forEach(sender::sendMessage);
    }

    @Subcommand("reload")
    @CommandPermission(PermissionRegistry.SR_RELOAD)
    @Description(Message.HELP_SR_RELOAD)
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

    @Subcommand("status")
    @CommandPermission(PermissionRegistry.SR_STATUS)
    @Description(Message.HELP_SR_STATUS)
    private void onStatus(SRCommandSender sender) {
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_CHECKING);

        sender.sendMessage(Message.DIVIDER);

        ServiceCheckerService.ServiceCheckResponse response = serviceCheckerService.checkServices();
        for (ServiceCheckerService.ServiceCheckResponse.ServiceCheckMessage message : response.getResults()) {
            if (!message.success() || settings.getProperty(DevConfig.DEBUG)) {
                sender.sendMessage(ComponentHelper.parseMiniMessageToJsonString(message.message()));
            }
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_UUID_API,
                Placeholder.unparsed("count", String.valueOf(response.getSuccessCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.UUID))),
                Placeholder.unparsed("total", String.valueOf(response.getTotalCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.UUID)))
        );
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_PROFILE_API,
                Placeholder.unparsed("count", String.valueOf(response.getSuccessCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.PROFILE))),
                Placeholder.unparsed("total", String.valueOf(response.getTotalCount(ServiceCheckerService.ServiceCheckResponse.ServiceCheckType.PROFILE)))
        );

        if (response.allFullySuccessful()) {
            // There were no unavailable services
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_WORKING);
        } else if (response.minOneServiceUnavailable()) {
            // At least one service was fully unavailable
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_BROKEN);
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_FIREWALL);
        } else {
            // No services are unavailable, but some APIs are not working
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_DEGRADED);
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_FIREWALL);
        }

        sender.sendMessage(Message.DIVIDER);
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_VERSION, Placeholder.unparsed("version", BuildData.VERSION));
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_SERVER, Placeholder.unparsed("version", adapter.getPlatformVersion()));

        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_PROXYMODE, Placeholder.unparsed("proxy_mode", Boolean.toString(serverPlugin.isProxyMode())));
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_COMMIT, Placeholder.unparsed("hash", BuildData.COMMIT_SHORT));
        sender.sendMessage(Message.DIVIDER);
    }

    @Subcommand({"drop", "remove"})
    @CommandPermission(PermissionRegistry.SR_DROP)
    @Description(Message.HELP_SR_DROP)
    private void onDrop(SRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        switch (playerOrSkin) {
            case PLAYER -> {
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
            }
            case SKIN -> {
                Optional<InputDataResult> optional = skinStorage.findSkinData(target);

                if (optional.isEmpty()) {
                    sender.sendMessage(Message.ADMINCOMMAND_DROP_SKIN_NOT_FOUND, Placeholder.unparsed("skin", target));
                    return;
                }

                InputDataResult result = optional.get();

                skinStorage.removeSkinData(result.getIdentifier());
            }
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_DROP, Placeholder.unparsed("type", playerOrSkin.toString()), Placeholder.unparsed("target", target));
    }

    @Subcommand({"info", "props", "lookup"})
    @CommandPermission(PermissionRegistry.SR_INFO)
    @Description(Message.HELP_SR_INFO)
    private void onInfo(SRCommandSender sender, PlayerOrSkin playerOrSkin, String input) {
        try {
            sender.sendMessage(Message.ADMINCOMMAND_INFO_CHECKING);
            var message = switch (playerOrSkin) {
                case PLAYER -> getPlayerInfoMessage(input);
                case SKIN -> getSkinInfoMessage(input);
            };

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

        return playerSkinData.<Consumer<SRCommandSender>>map(skinData -> sender -> {
            sender.sendMessage(Message.ADMINCOMMAND_INFO_PLAYER,
                    Placeholder.parsed("uuid", parsedUniqueId.get().toString()),
                    Placeholder.parsed("identifier", skinData.getIdentifier()),
                    Placeholder.parsed("variant", String.valueOf(skinData.getSkinVariant())),
                    Placeholder.parsed("type", skinData.getSkinType().toString()));
        }).orElseGet(() -> sender -> sender.sendMessage(Message.ADMINCOMMAND_INFO_NO_SET_SKIN));
    }

    private void sendGenericSkinInfoMessage(SRCommandSender sender, SkinProperty property) {
        MojangProfileResponse profile = PropertyUtils.getSkinProfileData(property);
        String texturesUrl = PropertyUtils.getSkinTextureUrl(property);
        SkinVariant variant = PropertyUtils.getSkinVariant(property);
        MojangProfileTextureMeta skinMetadata = profile.getTextures().getSKIN().getMetadata();

        long timestamp = profile.getTimestamp();
        String requestTime = new SimpleDateFormat(SRConstants.DATE_FORMAT, sender.getLocale())
                .format(new Date(timestamp));

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
                        Placeholder.parsed("timestamp", new SimpleDateFormat(SRConstants.DATE_FORMAT, sender.getLocale())
                                .format(new Date(TimeUnit.SECONDS.toMillis(playerSkinData.get().getTimestamp())))),
                        Placeholder.parsed("expires", new SimpleDateFormat(SRConstants.DATE_FORMAT, sender.getLocale())
                                .format(new Date(TimeUnit.SECONDS.toMillis(playerSkinData.get().getTimestamp()
                                        + TimeUnit.MINUTES.toSeconds(settings.getProperty(StorageConfig.SKIN_EXPIRES_AFTER)))))));
                sendGenericSkinInfoMessage(sender, skinData.getProperty());
            }).orElseGet(() -> sender -> sender.sendMessage(Message.NO_SKIN_DATA));
        }
    }

    @Subcommand("applyskin")
    @CommandPermission(PermissionRegistry.SR_APPLY_SKIN)
    @Description(Message.HELP_SR_APPLY_SKIN)
    private void onApplySkin(SRCommandSender sender, SRPlayer target) {
        try {
            skinApplier.applySkin(target.getAs(Object.class));
            sender.sendMessage(Message.SUCCESS_ADMIN_APPLYSKIN);
        } catch (DataRequestException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        }
    }

    @Subcommand("createcustom")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @Description(Message.HELP_SR_CREATE_CUSTOM)
    private void onCreateCustom(SRCommandSender sender, String skinName, String skinUrl, SkinVariant skinVariant) {
        try {
            if (ValidationUtil.validSkinUrl(skinUrl)) {
                MineSkinResponse response = mineSkinAPI.genSkin(skinUrl, skinVariant);
                skinStorage.setCustomSkinData(skinName, response.getProperty());
                sender.sendMessage(Message.SUCCESS_ADMIN_CREATECUSTOM, Placeholder.unparsed("skin", skinName));
            } else {
                sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
            }
        } catch (DataRequestException | MineSkinException e) {
            ComponentHelper.sendException(e, sender, locale, logger);
        }
    }

    @Subcommand("setskinall")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @Description(Message.HELP_SR_SET_SKIN_ALL)
    @CommandConditions("console-only")
    private void onSetSkinAll(SRCommandSender sender, String skinName, SkinVariant skinVariant) {
        Optional<InputDataResult> optional = skinStorage.findSkinData(skinName);

        if (optional.isEmpty()) {
            sender.sendMessage(Message.ADMINCOMMAND_SETSKINALL_NOT_FOUND);
            return;
        }

        for (SRPlayer player : adapter.getOnlinePlayers()) {
            playerStorage.setSkinIdOfPlayer(player.getUniqueId(), optional.get().getIdentifier());
            skinApplier.applySkin(player.getAs(Object.class), optional.get().getProperty());
        }

        sender.sendMessage(Message.ADMINCOMMAND_SETSKINALL_SUCCESS, Placeholder.unparsed("skin", skinName));
    }

    @Subcommand("applyskinall")
    @CommandPermission(PermissionRegistry.SR_APPLY_SKIN_ALL)
    @Description(Message.HELP_SR_APPLY_SKIN_ALL)
    @CommandConditions("console-only")
    private void onApplySkinAll(SRCommandSender sender) {
        for (SRPlayer player : adapter.getOnlinePlayers()) {
            try {
                skinApplier.applySkin(player.getAs(Object.class));
            } catch (DataRequestException ignored) {
                sender.sendMessage(Message.ADMINCOMMAND_APPLYSKINALL_PLAYER_ERROR, Placeholder.unparsed("player", player.getName()));
            }
        }

        sender.sendMessage(Message.ADMINCOMMAND_APPLYSKINALL_SUCCESS);
    }

    @Subcommand("purgeolddata")
    @CommandPermission(PermissionRegistry.SR_PURGE_OLD_DATA)
    @Description(Message.HELP_SR_PURGE_OLD_DATA)
    @CommandConditions("console-only")
    private void onPurgeOldData(SRCommandSender sender, int days) {
        if (skinStorage.purgeOldSkins(days)) {
            sender.sendMessage(Message.ADMINCOMMAND_PURGEOLDDATA_SUCCESS);
        } else {
            sender.sendMessage(Message.ADMINCOMMAND_PURGEOLDDATA_ERROR);
        }
    }

    @Subcommand("dump")
    @CommandPermission(PermissionRegistry.SR_DUMP)
    @Description(Message.HELP_SR_DUMP)
    private void onDump(SRCommandSender sender) {
        try {
            sender.sendMessage(Message.ADMINCOMMAND_DUMP_UPLOADING);
            Optional<String> url = dumpService.dump();
            if (url.isPresent()) {
                sender.sendMessage(Message.ADMINCOMMAND_DUMP_SUCCESS, Placeholder.unparsed("url", "https://bytebin.lucko.me/" + url.get()));
            } else {
                sender.sendMessage(Message.ADMINCOMMAND_DUMP_ERROR);
            }
        } catch (IOException | DataRequestException e) {
            logger.severe("Failed to dump data", e);
            sender.sendMessage(Message.ADMINCOMMAND_DUMP_ERROR);
        }
    }

    public enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }
}
