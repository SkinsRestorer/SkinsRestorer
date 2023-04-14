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
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.commands.library.annotations.*;
import net.skinsrestorer.shared.config.DevConfig;
import net.skinsrestorer.shared.connections.DumpService;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.messages.Message;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.C;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@SuppressWarnings("unused")
@CommandNames({"sr", "skinsrestorer"})
@Description(Message.HELP_SR)
@CommandPermission(PermissionRegistry.SR)
@CommandConditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand {
    private final SRPlugin plugin;
    private final SRPlatformAdapter<?> adapter;
    private final MojangAPIImpl mojangAPI;
    private final ServiceCheckerService serviceCheckerService;
    private final SkinStorageImpl skinStorage;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final DumpService dumpService;
    private final SkinsRestorer skinsRestorer;
    private final MineSkinAPI mineSkinAPI;
    private final SkinApplier<Object> skinApplier;
    private final Injector injector;
    private final CommandManager<SRCommandSender> commandManager;

    @RootCommand
    private void onDefault(SRCommandSender sender) {
        for (String line : commandManager.getRootHelp("sr", sender)) {
            sender.sendMessage(line);
        }
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

        sender.sendMessage(Message.SUCCESS_ADMIN_RELOAD);
    }

    @Subcommand("status")
    @CommandPermission(PermissionRegistry.SR_STATUS)
    @Description(Message.HELP_SR_STATUS)
    private void onStatus(SRCommandSender sender) {
        sender.sendMessage("§7Checking needed services for SR to work properly...");

        List<String> statusMessages = new LinkedList<>();
        String breakLine = "§3----------------------------------------------";
        statusMessages.add(breakLine);

        ServiceCheckerService.ServiceCheckResponse response = serviceCheckerService.checkServices();
        List<String> results = response.getResults();

        int workingUUIDCount = response.getWorkingUUID();
        int workingProfileCount = response.getWorkingProfile();

        // only print per API results if in a not working state
        if (settings.getProperty(DevConfig.DEBUG) || workingUUIDCount == 0 || workingProfileCount == 0) {
            for (String result : results) {
                if (settings.getProperty(DevConfig.DEBUG) || result.contains("✘")) {
                    statusMessages.add(result);
                }
            }
        }

        statusMessages.add("§7Working UUID API count: §6" + workingUUIDCount);
        statusMessages.add("§7Working Profile API count: §6" + workingProfileCount);

        if (workingUUIDCount != 0 && workingProfileCount != 0)
            statusMessages.add("§aThe plugin currently is in a working state.");
        else
            statusMessages.add("§cPlugin currently can't fetch new skins. \n Connection is likely blocked because of firewall. \n Please See https://skinsrestorer.net/firewall for more info");
        statusMessages.add(breakLine);
        statusMessages.add("§7SkinsRestorer §6v" + plugin.getVersion());
        statusMessages.add("§7Server: §6" + adapter.getPlatformVersion());
        SRServerPlugin serverPlugin = injector.getIfAvailable(SRServerPlugin.class);
        if (serverPlugin != null) {
            statusMessages.add("§7ProxyMode: §6" + serverPlugin.isProxyMode());
        }
        statusMessages.add("§7Commit: §6" + BuildData.COMMIT_SHORT);
        statusMessages.add("§7Finished checking services.");
        statusMessages.add(breakLine);
        statusMessages.forEach(sender::sendMessage);
    }

    @Subcommand({"drop", "remove"})
    @CommandPermission(PermissionRegistry.SR_DROP)
    @Description(Message.HELP_SR_DROP)
    private void onDrop(SRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        switch (playerOrSkin) {
            case PLAYER:
                skinStorage.removeSkinNameOfPlayer(target);
                break;
            case SKIN:
                skinStorage.removeSkinData(target);
                break;
        }

        sender.sendMessage(Message.SUCCESS_ADMIN_DROP, playerOrSkin.toString(), target);
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

            MojangProfileResponse profile = skinsRestorer.getSkinProfileData(properties.get());
            String decodedSkin = profile.getTextures().getSKIN().getUrl();
            long timestamp = profile.getTimestamp();
            String requestDate = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(timestamp));

            sender.sendMessage("§aRequest time: §e" + requestDate);
            sender.sendMessage("§aProfileId: §e" + profile.getProfileId());
            sender.sendMessage("§aName: §e" + profile.getProfileName());
            sender.sendMessage("§aSkinTexture: §e" + decodedSkin);
            sender.sendMessage("§cMore info in console!");

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
            sender.sendMessage(getRootCause(e).getMessage());
        }
    }

    @Subcommand("createcustom")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @Description(Message.HELP_SR_CREATE_CUSTOM)
    private void onCreateCustom(SRCommandSender sender, String skinName, String skinUrl, SkinVariant skinVariant) {
        try {
            if (C.validUrl(skinUrl)) {
                skinStorage.setSkinData(skinName, mineSkinAPI.genSkin(skinUrl, skinVariant), 0); // "generate" and save skin
                sender.sendMessage(Message.SUCCESS_ADMIN_CREATECUSTOM, skinName);
            } else {
                sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
            }
        } catch (DataRequestException e) {
            sender.sendMessage(getRootCause(e).getMessage());
        }
    }

    @Subcommand("setskinall")
    @CommandPermission(PermissionRegistry.SR_CREATE_CUSTOM)
    @Description(Message.HELP_SR_SET_SKIN_ALL)
    @CommandConditions("console-only")
    private void onSetSkinAll(SRCommandSender sender, String skinName, SkinVariant skinVariant) {
        String appliedSkinName = " ·setSkinAll";
        try {
            Optional<SkinProperty> skinProps;
            if (C.validUrl(skinName)) {
                skinProps = Optional.of(mineSkinAPI.genSkin(skinName, skinVariant));
            } else {
                skinProps = mojangAPI.getSkin(skinName);
            }

            if (!skinProps.isPresent()) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §4no skin found....");
                return;
            }

            skinStorage.setSkinData(appliedSkinName, skinProps.get());

            for (SRPlayer player : adapter.getOnlinePlayers()) {
                skinStorage.setSkinNameOfPlayer(player.getName(), appliedSkinName); // Set player to "whitespaced" name then reload skin
                skinApplier.applySkin(player.getAs(Object.class), skinProps.get());
            }

            sender.sendMessage("§aSuccessfully set skin of all online players to " + appliedSkinName);
        } catch (DataRequestException e) {
            sender.sendMessage(getRootCause(e).getMessage());
        }
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
                sender.sendMessage("§e[§2SkinsRestorer§e] §cFailed to apply skin to " + player.getName());
            }
        }
        sender.sendMessage("§e[§2SkinsRestorer§e] §aRe-applied skin of all online players");
    }

    @Subcommand("purgeolddata")
    @CommandPermission(PermissionRegistry.SR_PURGE_OLD_DATA)
    @Description(Message.HELP_SR_PURGE_OLD_DATA)
    @CommandConditions("console-only")
    private void onPurgeOldData(SRCommandSender sender, int days) {
        if (skinStorage.purgeOldSkins(days)) {
            sender.sendMessage("§e[§2SkinsRestorer§e] §aSuccessfully purged old skins!");
        } else {
            sender.sendMessage("§e[§2SkinsRestorer§e] §4A error occurred while purging old skins!");
        }
    }

    @Subcommand("dump")
    @CommandPermission(PermissionRegistry.SR_DUMP)
    @Description(Message.HELP_SR_DUMP)
    private void onDump(SRCommandSender sender) {
        try {
            sender.sendMessage("§e[§2SkinsRestorer§e] §aUploading data to bytebin.lucko.me...");
            Optional<String> url = dumpService.dump();
            if (url.isPresent()) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §aUpload successful! §ehttps://bytebin.lucko.me/" + url.get());
            } else {
                sender.sendMessage("§e[§2SkinsRestorer§e] §4Upload failed!");
            }
        } catch (IOException | DataRequestException e) {
            sender.sendMessage("§e[§2SkinsRestorer§e] §cFailed to upload data to bytebin.lucko.me");
            e.printStackTrace();
        }
    }

    public enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }
}
