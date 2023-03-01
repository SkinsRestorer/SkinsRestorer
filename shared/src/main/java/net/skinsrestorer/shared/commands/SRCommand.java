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
import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.interfaces.MineSkinAPI;
import net.skinsrestorer.api.interfaces.SkinApplier;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.model.SkinVariant;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.config.DevConfig;
import net.skinsrestorer.shared.connections.DumpService;
import net.skinsrestorer.shared.connections.MojangAPIImpl;
import net.skinsrestorer.shared.connections.ServiceCheckerService;
import net.skinsrestorer.shared.interfaces.SRCommandSender;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
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
@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
@Conditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand extends BaseCommand {
    private final SRPlugin plugin;
    private final SRPlatformAdapter adapter;
    private final MojangAPIImpl mojangAPI;
    private final ServiceCheckerService serviceCheckerService;
    private final SkinStorageImpl skinStorage;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final DumpService dumpService;
    private final SkinsRestorer skinsRestorer;
    private final MineSkinAPI mineSkinAPI;
    private final SkinApplier<Object> skinApplier;

    @HelpCommand
    @Syntax("%helpHelpCommand")
    private void onHelp(SRCommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    private void onReload(SRCommandSender sender) {
        plugin.loadConfig();
        plugin.loadLocales();
        plugin.prepareACF();

        sender.sendMessage(Message.SUCCESS_ADMIN_RELOAD);
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    private void onStatus(SRCommandSender sender) {
        adapter.runAsync(() -> {
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
            SRServerPlugin serverPlugin = plugin.getInjector().getIfAvailable(SRServerPlugin.class);
            if (serverPlugin != null) {
                statusMessages.add("§7ProxyMode: §6" + serverPlugin.isProxyMode());
            }
            statusMessages.add("§7Commit: §6" + BuildData.COMMIT.substring(0, 7));
            statusMessages.add("§7Finished checking services.");
            statusMessages.add(breakLine);
            statusMessages.forEach(sender::sendMessage);
        });
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("PLAYER|SKIN @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    private void onDrop(SRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        adapter.runAsync(() -> {
            switch (playerOrSkin) {
                case PLAYER:
                    skinStorage.removeSkinNameOfPlayer(target);
                    break;
                case SKIN:
                    skinStorage.removeSkinData(target);
                    break;
            }

            sender.sendMessage(Message.SUCCESS_ADMIN_DROP, playerOrSkin.toString(), target);
        });
    }

    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    private void onProps(SRCommandSender sender, SRPlayer target) {
        adapter.runAsync(() -> {
            try {
                List<SkinProperty> properties = adapter.getPropertiesOfPlayer(target);

                if (properties.isEmpty()) {
                    sender.sendMessage(Message.NO_SKIN_DATA);
                    return;
                }

                SkinProperty prop = properties.get(0);

                String value = prop.getValue();
                String signature = prop.getSignature();

                MojangProfileResponse profile = skinsRestorer.getSkinProfileData(prop);
                String decodedSkin = profile.getTextures().getSKIN().getUrl();
                long timestamp = profile.getTimestamp();
                String requestDate = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(timestamp));

                sender.sendMessage("§aRequest time: §e" + requestDate);
                sender.sendMessage("§aProfileId: §e" + profile.getProfileId());
                sender.sendMessage("§aName: §e" + profile.getProfileName());
                sender.sendMessage("§aSkinTexture: §e" + decodedSkin);
                sender.sendMessage("§cMore info in console!");

                // Console
                logger.info("§aValue: §8" + value);
                logger.info("§aSignature: §8" + signature);
                logger.info("§aValue Decoded: §e" + profile);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(Message.NO_SKIN_DATA);
            }
        });
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    private void onApplySkin(SRCommandSender sender, SRPlayer target) {
        adapter.runAsync(() -> {
            try {
                skinApplier.applySkin(target.getAs(Object.class));
                sender.sendMessage(Message.SUCCES_ADMIN_APPLYSKIN);
            } catch (Exception ignored) {
                sender.sendMessage(Message.ERROR_ADMIN_APPLYSKIN);
            }
        });
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [classic/slim]")
    private void onCreateCustom(SRCommandSender sender, String name, String skinUrl, SkinVariant skinVariant) {
        adapter.runAsync(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    skinStorage.setSkinData(name, mineSkinAPI.genSkin(skinUrl, skinVariant), 0); // "generate" and save skin
                    sender.sendMessage(Message.SUCCESS_ADMIN_CREATECUSTOM, name);
                } else {
                    sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
                }
            } catch (DataRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        });
    }

    @Subcommand("setskinall")
    @CommandCompletion("@Skin")
    @Description("Set the skin to every player")
    @Syntax(" <Skin / Url> [classic/slim]")
    @Conditions("console-only")
    private void onSetSkinAll(SRCommandSender sender, String skinName, SkinVariant skinVariant) {
        adapter.runAsync(() -> {
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
        });
    }

    @Subcommand("applyskinall")
    @Description("Re-apply the skin for every player")
    @Conditions("console-only")
    private void onApplySkinAll(SRCommandSender sender) {
        adapter.runAsync(() -> {
            for (SRPlayer player : adapter.getOnlinePlayers()) {
                try {
                    skinApplier.applySkin(player.getAs(Object.class));
                } catch (DataRequestException ignored) {
                    sender.sendMessage("§e[§2SkinsRestorer§e] §cFailed to apply skin to " + player.getName());
                }
            }
            sender.sendMessage("§e[§2SkinsRestorer§e] §aRe-applied skin of all online players");
        });
    }

    @Subcommand("purgeolddata")
    @Description("Purge old skin data from over x days ago")
    @Syntax(" <targetdaysold>")
    @Conditions("console-only")
    private void onPurgeOldData(SRCommandSender sender, int days) {
        adapter.runAsync(() -> {
            if (skinStorage.purgeOldSkins(days)) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §aSuccessfully purged old skins!");
            } else {
                sender.sendMessage("§e[§2SkinsRestorer§e] §4A error occurred while purging old skins!");
            }
        });
    }

    @Subcommand("dump")
    @CommandPermission("%srDump")
    @Description("Upload support data to bytebin.lucko.me")
    private void onDump(SRCommandSender sender) {
        adapter.runAsync(() -> {
            try {
                sender.sendMessage("§e[§2SkinsRestorer§e] §aUploading data to bytebin.lucko.me...");
                val url = dumpService.dump();
                if (url.isPresent()) {
                    sender.sendMessage("§e[§2SkinsRestorer§e] §aUpload successful! §ehttps://bytebin.lucko.me/" + url.get());
                } else {
                    sender.sendMessage("§e[§2SkinsRestorer§e] §4Upload failed!");
                }
            } catch (IOException e) {
                sender.sendMessage("§e[§2SkinsRestorer§e] §cFailed to upload data to bytebin.lucko.me");
                e.printStackTrace();
            }
        });
    }

    public enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }
}
