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
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.NotPremiumException;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.config.Config;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.DumpService;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@SuppressWarnings("unused")
@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
@Conditions("allowed-server")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRCommand extends BaseCommand {
    private final ISRPlugin plugin;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final DumpService dumpService;

    @HelpCommand
    @Syntax("%helpHelpCommand")
    private void onHelp(ISRCommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    private void onReload(ISRCommandSender sender) {
        plugin.reloadPlatformHook();
        plugin.loadConfig();
        plugin.loadLocales();
        plugin.prepareACF();

        sender.sendMessage(Message.SUCCESS_ADMIN_RELOAD);
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    private void onStatus(ISRCommandSender sender) {
        plugin.runAsync(() -> {
            sender.sendMessage("§7Checking needed services for SR to work properly...");

            List<String> statusMessages = new LinkedList<>();
            String breakLine = "§3----------------------------------------------";
            statusMessages.add(breakLine);

            ServiceChecker.ServiceCheckResponse response = ServiceChecker.checkServices(mojangAPI);
            List<String> results = response.getResults();

            int workingUUIDCount = response.getWorkingUUID();
            int workingProfileCount = response.getWorkingProfile();

            // only print per API results if in a not working state
            if (settings.getProperty(Config.DEBUG) || workingUUIDCount == 0 || workingProfileCount == 0) {
                for (String result : results) {
                    if (settings.getProperty(Config.DEBUG) || result.contains("✘")) {
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
            statusMessages.add("§7Server: §6" + plugin.getPlatformVersion());
            statusMessages.add("§7ProxyMode: §6" + plugin.getProxyModeInfo());
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
    private void onDrop(ISRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        plugin.runAsync(() -> {
            switch (playerOrSkin) {
                case PLAYER:
                    skinStorage.removeSkinOfPlayer(target);
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
    private void onProps(ISRCommandSender sender, ISRPlayer target) {
        plugin.runAsync(() -> {
            try {
                List<IProperty> properties = plugin.getPropertiesOfPlayer(target);

                if (properties.isEmpty()) {
                    sender.sendMessage(Message.NO_SKIN_DATA);
                    return;
                }

                IProperty prop = properties.get(0);

                String name = prop.getName();
                String value = prop.getValue();
                String signature = prop.getSignature();

                MojangProfileResponse profile = SkinsRestorerAPI.getApi().getSkinProfileData(prop);
                String decodedSkin = profile.getTextures().getSKIN().getUrl();
                long timestamp = profile.getTimestamp();
                String requestDate = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(timestamp));

                sender.sendMessage("§aRequest time: §e" + requestDate);
                sender.sendMessage("§aProfileId: §e" + profile.getProfileId());
                sender.sendMessage("§aName: §e" + profile.getProfileName());
                sender.sendMessage("§aSkinTexture: §e" + decodedSkin);
                sender.sendMessage("§cMore info in console!");

                // Console
                logger.info("§aName: §8" + name);
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
    private void onApplySkin(ISRCommandSender sender, ISRPlayer target) {
        plugin.runAsync(() -> {
            try {
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper());
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
    private void onCreateCustom(ISRCommandSender sender, String name, String skinUrl, SkinVariant skinVariant) {
        plugin.runAsync(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    skinStorage.setSkinData(name, SkinsRestorerAPI.getApi().genSkinUrl(skinUrl, skinVariant),
                            System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000)); // "generate" and save skin for 100 years
                    sender.sendMessage(Message.SUCCESS_ADMIN_CREATECUSTOM, name);
                } else {
                    sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        });
    }

    @Subcommand("setskinall")
    @CommandCompletion("@Skin")
    @Description("Set the skin to every player")
    @Syntax(" <Skin / Url> [classic/slim]")
    @Conditions("console-only")
    private void onSetSkinAll(ISRCommandSender sender, String skin, SkinVariant skinVariant) {
        plugin.runAsync(() -> {
            String skinName = " ·setSkinAll";
            try {
                IProperty skinProps;
                if (C.validUrl(skin)) {
                    skinProps = SkinsRestorerAPI.getApi().genSkinUrl(skin, skinVariant);
                } else {
                    skinProps = mojangAPI.getSkin(skin).orElse(null);
                }
                if (skinProps == null) {
                    sender.sendMessage("§e[§2SkinsRestorer§e] §4no skin found....");
                    return;
                }

                skinStorage.setSkinData(skinName, skinProps);

                for (ISRPlayer player : plugin.getOnlinePlayers()) {
                    final String pName = player.getName();
                    skinStorage.setSkinOfPlayer(pName, skinName); // Set player to "whitespaced" name then reload skin
                    SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), skinProps);
                }
                sender.sendMessage("§aSuccessfully set skin of all online players to " + skin);
            } catch (NotPremiumException | SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        });
    }

    @Subcommand("applyskinall")
    @Description("Re-apply the skin for every player")
    @Conditions("console-only")
    private void onApplySkinAll(ISRCommandSender sender) {
        plugin.runAsync(() -> {
            for (ISRPlayer player : plugin.getOnlinePlayers()) {
                try {
                    SkinsRestorerAPI.getApi().applySkin(player.getWrapper());
                } catch (SkinRequestException | NotPremiumException ignored) {
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
    private void onPurgeOldData(ISRCommandSender sender, int days) {
        plugin.runAsync(() -> {
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
    private void onDump(ISRCommandSender sender) {
        plugin.runAsync(() -> {
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
