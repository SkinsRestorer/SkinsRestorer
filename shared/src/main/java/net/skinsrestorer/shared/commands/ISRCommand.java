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

import co.aikar.commands.CommandHelp;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.model.MojangProfileResponse;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;

import java.text.SimpleDateFormat;
import java.util.*;

public interface ISRCommand {
    @SuppressWarnings("unused")
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    default void reloadCustomHook() {
    }

    default void onReload(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        reloadCustomHook();
        Locale.load(plugin.getDataFolderPath(), plugin.getSrLogger());
        Config.load(plugin.getDataFolderPath(), plugin.getResource("config.yml"), plugin.getSrLogger());

        plugin.prepareACF(plugin.getManager(), plugin.getSrLogger());

        sender.sendMessage(Locale.RELOAD);
    }

    default void onStatus(ISRCommandSender sender) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            sender.sendMessage("§7Checking needed services for SR to work properly...");

            List<String> statusMessages = new LinkedList<>();
            String breakLine = "§3----------------------------------------------";
            statusMessages.add(breakLine);

            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            final int workingUUIDCount = response.getWorkingUUID().get();
            final int workingProfileCount = response.getWorkingProfile().get();

            // only print per API results if in a not working state
            if (Config.DEBUG || workingUUIDCount == 0 || workingProfileCount == 0)
                for (String result : results)
                    if (Config.DEBUG || result.contains("✘"))
                        statusMessages.add(result);

            statusMessages.add("§7Working UUID API count: §6" + workingUUIDCount);
            statusMessages.add("§7Working Profile API count: §6" + workingProfileCount);

            if (workingUUIDCount != 0 && workingProfileCount != 0)
                statusMessages.add("§aThe plugin currently is in a working state.");
            else
                statusMessages.add("§cPlugin currently can't fetch new skins. \n Connection is likely blocked because of firewall. \n Please See http://skinsrestorer.net/firewall for more info");
            statusMessages.add(breakLine);
            statusMessages.add("§7SkinsRestorer §6v" + plugin.getVersion());
            statusMessages.add("§7Server: §6" + getPlatformVersion());
            statusMessages.add("§7ProxyMode: §6" + getProxyMode());
            statusMessages.add("§7Finished checking services.");
            statusMessages.add(breakLine);
            statusMessages.forEach(sender::sendMessage);
        });
    }

    default void onDrop(ISRCommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            switch (playerOrSkin) {
                case PLAYER:
                    plugin.getSkinStorage().removeSkinOfPlayer(target);
                    break;
                case SKIN:
                    plugin.getSkinStorage().removeSkinData(target);
                    break;
            }

            sender.sendMessage(Locale.DATA_DROPPED.replace("%playerOrSkin", playerOrSkin.toString()).replace("%targets", target));
        });
    }

    default void onProps(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {
                List<IProperty> properties = getPropertiesOfPlayer(target);

                if (properties.isEmpty()) {
                    sender.sendMessage(Locale.NO_SKIN_DATA);
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
                sender.sendMessage("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length() - 1));
                sender.sendMessage("§cMore info in console!");

                // Console
                plugin.getSrLogger().info("§aName: §8" + name);
                plugin.getSrLogger().info("§aValue: §8" + value);
                plugin.getSrLogger().info("§aSignature: §8" + signature);
                plugin.getSrLogger().info("§aValue Decoded: §e" + profile);
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(Locale.NO_SKIN_DATA);
            }
        });
    }

    default void onApplySkin(ISRCommandSender sender, ISRPlayer target) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {
                SkinsRestorerAPI.getApi().applySkin(target.getWrapper(), plugin.getSkinStorage().getDefaultSkinForPlayer(sender.getName()));
                sender.sendMessage(Locale.ADMIN_APPLYSKIN_SUCCES);
            } catch (Exception ignored) {
                sender.sendMessage(Locale.ADMIN_APPLYSKIN_ERROR);
            }
        });
    }

    default void onCreateCustom(ISRCommandSender sender, String name, String skinUrl, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    plugin.getSkinStorage().setSkinData(name, SkinsRestorerAPI.getApi().genSkinUrl(skinUrl, skinVariant),
                            System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000)); // "generate" and save skin for 100 years
                    sender.sendMessage(Locale.SUCCESS_CREATE_SKIN.replace("%skin", name));
                } else {
                    sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }
        });
    }

    default void onSetSkinAll(ISRCommandSender sender, String skin, SkinVariant skinVariant) {
        if (!CommandUtil.isAllowedToExecute(sender)) return;

        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            if (!sender.isConsole()) { // Only make console perform this command
                sender.sendMessage(Locale.PREFIX + "§4Only console may execute this command!");
                return;
            }

            String skinName = " ·setSkinAll";
            try {
                IProperty skinProps;
                if (C.validUrl(skin)) {
                    skinProps = SkinsRestorerAPI.getApi().genSkinUrl(skin, skinVariant);
                } else {
                    skinProps = plugin.getMojangAPI().getSkin(skin).orElse(null);
                }
                if (skinProps == null) {
                    sender.sendMessage(Locale.PREFIX + "§4no skin found....");
                    return;
                }

                plugin.getSkinStorage().setSkinData(skinName, skinProps);

                for (ISRPlayer player : plugin.getOnlinePlayers()) {
                    final String pName = player.getName();
                    plugin.getSkinStorage().setSkinOfPlayer(pName, skinName); // Set player to "whitespaced" name then reload skin
                    SkinsRestorerAPI.getApi().applySkin(player.getWrapper(), skinProps);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }
        });
    }

    String getPlatformVersion();

    String getProxyMode();

    ISRPlugin getPlugin();

    List<IProperty> getPropertiesOfPlayer(ISRPlayer player);

    @SuppressWarnings("unused")
    enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }

    @SuppressWarnings("unused")
    enum SkinType {
        STEVE,
        SLIM,
    }
}
