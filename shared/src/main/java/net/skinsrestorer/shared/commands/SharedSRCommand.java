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
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.plugin.SkinsRestorerShared;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static net.skinsrestorer.shared.utils.SharedMethods.getRootCause;

@RequiredArgsConstructor
public abstract class SharedSRCommand extends BaseCommand {
    protected final SkinsRestorerShared plugin;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;
    private final SettingsManager settings;
    private final SRLogger logger;
    private final CallableValue<Collection<ISRPlayer>> onlinePlayersFunction;

    protected void onHelp(ISRCommandSender sender, CommandHelp help) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        help.showHelp();
    }

    protected void reloadCustomHook() {
    }

    protected void onReload(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        reloadCustomHook();
        plugin.loadConfig();
        plugin.loadLocales();
        plugin.prepareACF();

        sender.sendMessage(Message.RELOAD);
    }

    protected void onStatus(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            sender.sendMessage("§7Checking needed services for SR to work properly...");

            List<String> statusMessages = new LinkedList<>();
            String breakLine = "§3----------------------------------------------";
            statusMessages.add(breakLine);

            ServiceChecker.ServiceCheckResponse response = ServiceChecker.checkServices(mojangAPI);
            List<String> results = response.getResults();

            final int workingUUIDCount = response.getWorkingUUID().get();
            final int workingProfileCount = response.getWorkingProfile().get();

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
            statusMessages.add("§7Server: §6" + getPlatformVersion());
            statusMessages.add("§7ProxyMode: §6" + getProxyMode());
            statusMessages.add("§7Finished checking services.");
            statusMessages.add(breakLine);
            statusMessages.forEach(sender::sendMessage);
        });
    }

    protected void onDrop(ISRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            switch (playerOrSkin) {
                case PLAYER:
                    skinStorage.removeSkinOfPlayer(target);
                    break;
                case SKIN:
                    skinStorage.removeSkinData(target);
                    break;
            }

            sender.sendMessage(Message.DATA_DROPPED, playerOrSkin.toString(), target);
        });
    }

    protected void onProps(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            try {
                List<IProperty> properties = getPropertiesOfPlayer(target);

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

    protected void onApplySkin(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            try {
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper());
                sender.sendMessage(Message.ADMIN_APPLYSKIN_SUCCES);
            } catch (Exception ignored) {
                sender.sendMessage(Message.ADMIN_APPLYSKIN_ERROR);
            }
        });
    }

    protected void onCreateCustom(ISRCommandSender sender, String name, String skinUrl, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    skinStorage.setSkinData(name, SkinsRestorerAPI.getApi().genSkinUrl(skinUrl, skinVariant),
                            System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000)); // "generate" and save skin for 100 years
                    sender.sendMessage(Message.SUCCESS_CREATE_SKIN, name);
                } else {
                    sender.sendMessage(Message.ERROR_INVALID_URLSKIN);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        });
    }

    protected void onSetSkinAll(ISRCommandSender sender, String skin, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender, settings)) return;

        plugin.runAsync(() -> {
            if (!sender.isConsole()) {
                sender.sendMessage(Message.PREFIX + "§4Only console may execute this command!");
                return;
            }

            String skinName = " ·setSkinAll";
            try {
                IProperty skinProps;
                if (C.validUrl(skin)) {
                    skinProps = SkinsRestorerAPI.getApi().genSkinUrl(skin, skinVariant);
                } else {
                    skinProps = mojangAPI.getSkin(skin).orElse(null);
                }
                if (skinProps == null) {
                    sender.sendMessage(Message.PREFIX + "§4no skin found....");
                    return;
                }

                skinStorage.setSkinData(skinName, skinProps);

                for (ISRPlayer player : onlinePlayersFunction.call()) {
                    final String pName = player.getName();
                    skinStorage.setSkinOfPlayer(pName, skinName); // Set player to "whitespaced" name then reload skin
                    SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), skinProps);
                }
                sender.sendMessage("§aSuccessfully set skin of all online players to " + skin);
            } catch (SkinRequestException e) {
                sender.sendMessage(getRootCause(e).getMessage());
            }
        });
    }

    protected void onApplySkinAll(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        plugin.runAsync(() -> {
            if (!sender.isConsole()) {
                sender.sendMessage(Message.PREFIX + "§4Only console may execute this command!");
                return;
            }

            for (ISRPlayer player : plugin.getOnlinePlayers()) {
                try {
                    SkinsRestorerAPI.getApi().applySkin(player.getWrapper());
                } catch (SkinRequestException ignored) {
                    sender.sendMessage(Message.PREFIX + "§cFailed to apply skin to " + player.getName());
                }
            }
            sender.sendMessage(Message.PREFIX + "§aRe-applied skin of all online players");
        });
    }

    protected void onPurgeOldData(ISRCommandSender sender, int days) {
        plugin.runAsync(() -> {
            if (!sender.isConsole()) {
                sender.sendMessage(Message.PREFIX + "§4Only console may execute this command!");
                return;
            }
            if (skinStorage.purgeOldSkins(days)) {
                sender.sendMessage(Message.PREFIX + "§aSuccessfully purged old skins!");
            } else {
                sender.sendMessage(Message.PREFIX + "§4A error occurred while purging old skins!");
            }
        });
    }

    protected abstract String getPlatformVersion();

    protected abstract String getProxyMode();

    protected abstract List<IProperty> getPropertiesOfPlayer(ISRPlayer player);

    protected enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }
}
