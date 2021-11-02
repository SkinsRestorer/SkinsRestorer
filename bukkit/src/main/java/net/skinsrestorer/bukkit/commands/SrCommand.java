/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
package net.skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bukkit.SkinApplierBukkit;
import net.skinsrestorer.api.reflection.ReflectionUtil;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.connections.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SRLogger logger;

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        SkinApplierBukkit.setOptFileChecked(false);
        Locale.load(plugin.getDataFolder(), logger);
        Config.load(plugin.getDataFolder(), plugin.getResource("config.yml"), logger);

        plugin.prepareACF(plugin.getManager(), plugin.getSrLogger());

        sender.sendMessage(Locale.RELOAD);
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
            sender.sendMessage("§7Server: §6" + plugin.getServer().getVersion());
            sender.sendMessage("§7BungeeMode: §6" + plugin.isBungeeEnabled());
            sender.sendMessage("§7Finished checking services.");
            sender.sendMessage("§3----------------------------------------------");
        });
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("PLAYER|SKIN @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSender sender, PlayerOrSkin playerOrSkin, String[] targets) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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


    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSender sender, @Single OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Object ep = ReflectionUtil.invokeMethod(target.getPlayer(), "getHandle");
                Object profile = ReflectionUtil.invokeMethod(ep, "getProfile");
                Object propMap = ReflectionUtil.invokeMethod(profile, "getProperties");

                Collection<?> props = (Collection<?>) ReflectionUtil.invokeMethod(propMap.getClass(), propMap, "get",
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
                    logger.info("§aName: §8" + name);
                    logger.info("§aValue : §8" + value);
                    logger.info("§aSignature : §8" + signature);
                    logger.info("§aValue Decoded: §e" + Arrays.toString(decoded));
                }
            } catch (Exception e) {
                e.printStackTrace();
                sender.sendMessage(Locale.NO_SKIN_DATA);
            }
        });
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    public void onApplySkin(CommandSender sender, @Single OnlinePlayer target) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final Player player = target.getPlayer();
                final String name = player.getName();
                final String skin = plugin.getSkinStorage().getDefaultSkinName(name);

                if (C.validUrl(skin)) {
                    plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), plugin.getMineSkinAPI().genSkin(skin, null, null));
                } else {
                    plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), skin);
                }
                sender.sendMessage("success: player skin has been refreshed!");
            } catch (Exception ignored) {
                sender.sendMessage("ERROR: player skin could NOT be refreshed!");
            }
        });
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [steve/slim]")
    public void onCreateCustom(CommandSender sender, String name, String skinUrl, @Optional SkinType skinType) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (C.validUrl(skinUrl)) {
                    plugin.getSkinStorage().setSkinData(name, plugin.getMineSkinAPI().genSkin(skinUrl, String.valueOf(skinType), null),
                            Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                    sender.sendMessage(Locale.SUCCESS_CREATE_SKIN.replace("%skin", name));
                } else {
                    sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            }
        });
    }

    @SuppressWarnings("unused")
    public enum PlayerOrSkin {
        PLAYER,
        SKIN,
    }

    @SuppressWarnings("unused")
    public enum SkinType {
        STEVE,
        SLIM,
    }
}
