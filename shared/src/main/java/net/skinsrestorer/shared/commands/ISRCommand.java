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
import co.aikar.commands.annotation.Optional;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.PropertyMap;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

public interface ISRCommand {
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    default void reloadCustomHook() {
    }

    default void onReload(ISRCommandSender sender) {
        ISRPlugin plugin = getPlugin();
        reloadCustomHook();
        Locale.load(plugin.getDataFolder(), plugin.getSrLogger());
        Config.load(plugin.getDataFolder(), plugin.getResource("config.yml"), plugin.getSrLogger());

        plugin.prepareACF(plugin.getManager(), plugin.getSrLogger());

        sender.sendMessage(Locale.RELOAD);
    }

    default void onStatus(ISRCommandSender sender) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            sender.sendMessage("§3----------------------------------------------");
            sender.sendMessage("§7Checking needed services for SR to work properly...");

            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            if (Config.DEBUG || !(response.getWorkingUUID().get() >= 1) || !(response.getWorkingProfile().get() >= 1))
                for (String result : results) {
                    if (Config.DEBUG || result.contains("✘"))
                        sender.sendMessage(result);
                }

            sender.sendMessage("§7Working UUID API count: §6" + response.getWorkingUUID());
            sender.sendMessage("§7Working Profile API count: §6" + response.getWorkingProfile());

            if (response.getWorkingUUID().get() >= 1 && response.getWorkingProfile().get() >= 1)
                sender.sendMessage("§aThe plugin currently is in a working state.");
            else
                sender.sendMessage("§cPlugin currently can't fetch new skins. \n Connection is likely blocked because of firewall. \n Please See http://skinsrestorer.net/firewall for more info");
            sender.sendMessage("§3----------------------------------------------");
            sender.sendMessage("§7SkinsRestorer §6v" + plugin.getVersion());
            sender.sendMessage("§7Server: §6" + getPlatformVersion());
            sender.sendMessage("§7ProxyMode: §6" + getProxyMode());
            sender.sendMessage("§7Finished checking services.");
            sender.sendMessage("§3----------------------------------------------");
        });
    }

    default void onDrop(ISRCommandSender sender, PlayerOrSkin playerOrSkin, String[] targets) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            if (playerOrSkin == PlayerOrSkin.PLAYER)
                for (String targetPlayer : targets)
                    plugin.getSkinStorage().removeSkin(targetPlayer);
            else
                for (String targetSkin : targets)
                    plugin.getSkinStorage().removeSkinData(targetSkin);

            String targetList = Arrays.toString(targets).substring(1, Arrays.toString(targets).length() - 1);
            sender.sendMessage(Locale.DATA_DROPPED.replace("%playerOrSkin", playerOrSkin.toString()).replace("%targets", targetList));
        });
    }

    default void onProps(ISRCommandSender sender, ISRPlayer target) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {
                PropertyMap propertyMap = plugin.getSkinApplierBukkit().getGameProfile(target.getWrapper()).getProperties(); // TODO: make not platform specific
                Collection<?> props = (Collection<?>) ReflectionUtil.invokeMethod(propertyMap.getClass(), propertyMap, "get",
                        new Class<?>[]{Object.class}, "textures");

                if (props == null || props.isEmpty()) {
                    sender.sendMessage(Locale.NO_SKIN_DATA);
                    return;
                }

                for (Object prop : props) {
                    String name = (String) ReflectionUtil.invokeMethod(prop, "getName");
                    String value = (String) ReflectionUtil.invokeMethod(prop, "getValue");
                    String signature = (String) ReflectionUtil.invokeMethod(prop, "getSignature");

                    byte[] decoded = Base64.getDecoder().decode(value);
                    String decodedString = new String(decoded);
                    JsonObject jsonObject = JsonParser.parseString(decodedString).getAsJsonObject();
                    String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
                    long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
                    String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(timestamp));

                    sender.sendMessage("§aRequest time: §e" + requestDate);
                    sender.sendMessage("§aProfileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString());
                    sender.sendMessage("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString());
                    sender.sendMessage("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length() - 1));
                    sender.sendMessage("§cMore info in console!");

                    // Console
                    plugin.getSrLogger().info("§aName: §8" + name);
                    plugin.getSrLogger().info("§aValue : §8" + value);
                    plugin.getSrLogger().info("§aSignature : §8" + signature);
                    plugin.getSrLogger().info("§aValue Decoded: §e" + Arrays.toString(decoded));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(Locale.NO_SKIN_DATA);
            }
        });
    }

    default void onApplySkin(ISRCommandSender sender, ISRPlayer target) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {

                final String name = sender.getName();
                final String skin = plugin.getSkinStorage().getDefaultSkinName(name);

                if (C.validUrl(skin)) {
                    SkinsRestorerAPI.getApi().applySkin(target.getWrapper(), SkinsRestorerAPI.getApi().genSkinUrl(skin, null));
                } else {
                    SkinsRestorerAPI.getApi().applySkin(target.getWrapper(), skin);
                }
                sender.sendMessage("success: player skin has been refreshed!");
            } catch (Exception ignored) {
                sender.sendMessage("ERROR: player skin could NOT be refreshed!");
            }
        });
    }

    default void onCreateCustom(ISRCommandSender sender, String name, String skinUrl, SkinType skinType) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    plugin.getSkinStorage().setSkinData(name, SkinsRestorerAPI.getApi().genSkinUrl(skinUrl, String.valueOf(skinType)),
                            System.currentTimeMillis() + Duration.of(100, ChronoUnit.YEARS).toMillis()); // "generate" and save skin for 100 years
                    sender.sendMessage(Locale.SUCCESS_CREATE_SKIN.replace("%skin", name));
                } else {
                    sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }
        });
    }


    default void onSetSkinAll(ISRCommandSender sender, String skin, @Optional SkinType skinType) {
        ISRPlugin plugin = getPlugin();
        plugin.runAsync(() -> {
            if (!(sender instanceof ConsoleCommandSender)) { //only make console perform this command
                sender.sendMessage(Locale.PREFIX + "Only console may execute this command!"); // TODO: add chat color
                return;
            }

            String skinName = " ·setSkinAll";
            try {
                IProperty skinProps;
                if (C.validUrl(skin)) {
                    skinProps = SkinsRestorerAPI.getApi().genSkinUrl(skin, String.valueOf(skinType));
                } else {
                    skinProps = plugin.getMojangAPI().getSkin(skin).orElse(null);
                }
                if (skinProps == null) {
                    sender.sendMessage(Locale.PREFIX + "no skin found...."); // TODO: add chat color
                    return;
                }

                plugin.getSkinStorage().setSkinData(skinName, skinProps);

                for (ISRPlayer player : plugin.getOnlinePlayers()) {
                    String pName = player.getName();
                    plugin.getSkinStorage().setSkinName(pName, skinName); // set player to "whitespaced" name then reload skin
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

    enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }

    enum SkinType {
        STEVE,
        SLIM,
    }
}
