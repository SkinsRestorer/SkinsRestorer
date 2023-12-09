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
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.SkinApplier;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.api.storage.CacheStorage;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.DevConfig;
import net.skinsrestorer.shared.connections.DumpService;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ValidationUtil;
import net.skinsrestorer.shared.utils.ComponentHelper;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
@CommandNames({"sr", "skinsrestorer"})
@Description(Message.HELP_SR)
@CommandPermission(PermissionRegistry.SR)
@CommandConditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand {
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?> adapter;
    private final ServiceCheckerService serviceCheckerService;
    private final PlayerStorage playerStorage;
    private final CacheStorage cacheStorage;
    private final SkinStorageImpl skinStorage;
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
            e.printStackTrace();
        }

        try {
            plugin.loadStorage();
        } catch (InitializeException e) {
            e.printStackTrace();
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_RELOAD);
    }

    @Subcommand("status")
    @CommandPermission(PermissionRegistry.SR_STATUS)
    @Description(Message.HELP_SR_STATUS)
    private void onStatus(SRCommandSender sender) {
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_CHECKING);

        String breakLine = ComponentHelper.parseMiniMessageToJsonString("<dark_aqua>----------------------------------------------");
        sender.sendMessage(breakLine);

        ServiceCheckerService.ServiceCheckResponse response = serviceCheckerService.checkServices();
        List<String> results = response.getResults();

        int workingUUIDCount = response.getWorkingUUID();
        int workingProfileCount = response.getWorkingProfile();

        // only print per API results if in a not working state
        if (settings.getProperty(DevConfig.DEBUG) || workingUUIDCount == 0 || workingProfileCount == 0) {
            for (String result : results) {
                if (settings.getProperty(DevConfig.DEBUG) || result.contains("✘")) {
                    sender.sendMessage(ComponentHelper.parseMiniMessageToJsonString(result));
                }
            }
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_WORKING_COUNT, Placeholder.unparsed("count", String.valueOf(workingUUIDCount)));
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_WORKING_COUNT, Placeholder.unparsed("count", String.valueOf(workingProfileCount)));

        if (workingUUIDCount != 0 && workingProfileCount != 0) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_WORKING);
        } else {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_BROKEN);
        }

        sender.sendMessage(breakLine);
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_VERSION, Placeholder.unparsed("version", plugin.getVersion()));
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_SERVER, Placeholder.unparsed("version", adapter.getPlatformVersion()));

        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
            sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_PROXYMODE, Placeholder.unparsed("proxy_mode", Boolean.toString(serverPlugin.isProxyMode())));
        }

        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_COMMIT, Placeholder.unparsed("hash", BuildData.COMMIT_SHORT));
        sender.sendMessage(Message.ADMINCOMMAND_STATUS_SUMMARY_FINISHED);
        sender.sendMessage(breakLine);
    }

    @Subcommand({"drop", "remove"})
    @CommandPermission(PermissionRegistry.SR_DROP)
    @Description(Message.HELP_SR_DROP)
    private void onDrop(SRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        switch (playerOrSkin) {
            case PLAYER:
                try {
                    Optional<UUID> targetId = cacheStorage.getUUID(target, false);

                    if (!targetId.isPresent()) {
                        sender.sendMessage(Message.ADMINCOMMAND_DROP_PLAYER_NOT_FOUND, Placeholder.unparsed("player", target));
                        return;
                    }

                    playerStorage.removeSkinIdOfPlayer(targetId.get());
                } catch (DataRequestException e) {
                    sender.sendMessage(Message.ADMINCOMMAND_DROP_UUID_ERROR);
                    return;
                }
                break;
            case SKIN:
                Optional<InputDataResult> optional = skinStorage.findSkinData(target);

                if (!optional.isPresent()) {
                    sender.sendMessage(Message.ADMINCOMMAND_DROP_SKIN_NOT_FOUND, Placeholder.unparsed("skin", target));
                    return;
                }

                InputDataResult result = optional.get();

                skinStorage.removeSkinData(result.getIdentifier());
                break;
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_DROP, Placeholder.unparsed("type", playerOrSkin.toString()), Placeholder.unparsed("target", target));
    }

    @Subcommand("props")
    @CommandPermission(PermissionRegistry.SR_PROPS)
    @Description(Message.HELP_SR_PROPS)
    private void onProps(SRCommandSender sender, SRPlayer target) {
        try {
            Optional<SkinProperty> properties = adapter.getSkinProperty(target);

            if (!properties.isPresent()) {
                sender.sendMessage(Message.NO_SKIN_DATA);
                return;
            }

            MojangProfileResponse profile = PropertyUtils.getSkinProfileData(properties.get());
            String decodedSkin = profile.getTextures().getSKIN().getUrl();
            long timestamp = profile.getTimestamp();
            String requestTime = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(timestamp));

            sender.sendMessage(Message.ADMINCOMMAND_PROPS_REQUEST_TIME, Placeholder.unparsed("request_time", requestTime));
            sender.sendMessage(Message.ADMINCOMMAND_PROPS_PROFILE_ID, Placeholder.unparsed("profile_id", profile.getProfileId()));
            sender.sendMessage(Message.ADMINCOMMAND_PROPS_NAME, Placeholder.unparsed("profile_name", profile.getProfileName()));
            sender.sendMessage(Message.ADMINCOMMAND_PROPS_SKIN_TEXTURE, Placeholder.unparsed("skin_texture", decodedSkin));
            sender.sendMessage(Message.ADMINCOMMAND_PROPS_MORE_INFO_IN_CONSOLE);

            // Console
            logger.info("§aValue: §8" + properties.get().getValue());
            logger.info("§aSignature: §8" + properties.get().getSignature());
            logger.info("§aValue Decoded: §e" + profile);
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Message.NO_SKIN_DATA);
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

        if (!optional.isPresent()) {
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
            sender.sendMessage(Message.ADMINCOMMAND_DUMP_ERROR);
            e.printStackTrace();
        }
    }

    public enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }
}
