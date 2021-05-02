/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SRLogger logger;

    public SrCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
        logger = plugin.getSrLogger();
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        Locale.load(plugin.getConfigPath(), logger);
        Config.load(plugin.getConfigPath(), plugin.getResourceAsStream("config.yml"), logger);
        sender.sendMessage(TextComponent.fromLegacyText(Locale.RELOAD));
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
        sender.sendMessage(TextComponent.fromLegacyText("§7Checking needed services for SR to work properly..."));

        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            if (Config.DEBUG || !(response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1))
                for (String result : results) {
                    if (Config.DEBUG || result.contains("✘"))
                        sender.sendMessage(TextComponent.fromLegacyText(result));
                }
            sender.sendMessage(TextComponent.fromLegacyText("§7Working UUID API count: §6 " + response.getWorkingUUID()));
            sender.sendMessage(TextComponent.fromLegacyText("§7Working Profile API count: §6" + response.getWorkingProfile()));

            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                sender.sendMessage(TextComponent.fromLegacyText("§aThe plugin currently is in a working state."));
            else
                sender.sendMessage(TextComponent.fromLegacyText("§cPlugin currently can't fetch new skins. \n Connection is likely blocked because of firewall. \n Please See http://skinsrestorer.net/firewall for more info"));

            sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
            sender.sendMessage(TextComponent.fromLegacyText("§7SkinsRestorer §6v" + plugin.getVersion()));
            sender.sendMessage(TextComponent.fromLegacyText("§7Server: §6" + plugin.getProxy().getVersion()));
            sender.sendMessage(TextComponent.fromLegacyText("§7BungeeMode: §6Bungee-Plugin"));
            sender.sendMessage(TextComponent.fromLegacyText("§7Finished checking services."));
            sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
        });
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("player|skin @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSender sender, PlayerOrSkin e, String[] targets) {
        if (e == PlayerOrSkin.PLAYER)
            for (String targetPlayer : targets)
                plugin.getSkinStorage().removePlayerSkin(targetPlayer);
        else
            for (String targetSkin : targets)
                plugin.getSkinStorage().removeSkinData(targetSkin);
        String targetList = Arrays.toString(targets).substring(1, Arrays.toString(targets).length() - 1);
        sender.sendMessage(TextComponent.fromLegacyText(Locale.DATA_DROPPED.replace("%playerOrSkin", e.toString()).replace("%targets", targetList)));
    }

    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSender sender, @Single OnlinePlayer target) {
        InitialHandler h = (InitialHandler) target.getPlayer().getPendingConnection();
        LoginResult.Property prop = h.getLoginProfile().getProperties()[0];

        if (prop == null) {
            sender.sendMessage(TextComponent.fromLegacyText(Locale.NO_SKIN_DATA));
            return;
        }

        byte[] decoded = Base64.getDecoder().decode(prop.getValue());

        String decodedString = new String(decoded);
        JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
        String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
        long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
        String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(timestamp));

        sender.sendMessage(TextComponent.fromLegacyText("§aRequest time: §e" + requestDate));
        sender.sendMessage(TextComponent.fromLegacyText("§aprofileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString()));
        sender.sendMessage(TextComponent.fromLegacyText("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString()));
        sender.sendMessage(TextComponent.fromLegacyText("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length() - 1)));
        sender.sendMessage(TextComponent.fromLegacyText("§cMore info in console!"));

        // Console
        logger.info("§aName: §8" + prop.getName());
        logger.info("§aValue : §8" + prop.getValue());
        logger.info("§aSignature : §8" + prop.getSignature());
        logger.info("§aValue Decoded: §e" + Arrays.toString(decoded));
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    public void onApplySkin(CommandSender sender, @Single OnlinePlayer target) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                final ProxiedPlayer p = target.getPlayer();
                final String name = p.getName();
                final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(name);

                plugin.getSkinApplierBungee().applySkin(p, skin, null);
                sender.sendMessage(TextComponent.fromLegacyText("success: player skin has been refreshed!"));
            } catch (Exception ignored) {
                sender.sendMessage(TextComponent.fromLegacyText("ERROR: player skin could NOT be refreshed!"));
            }
        });
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [steve/slim]")
    public void onCreateCustom(CommandSender sender, String skinName, String skinUrl, @Optional SkinType skinType) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try {
                if (C.validUrl(skinUrl)) {
                    plugin.getSkinStorage().setSkinData(skinName, plugin.getMineSkinAPI().genSkin(skinUrl, skinType.toString()),
                            Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.SUCCESS_CREATE_SKIN.replace("%skin", skinName)));
                } else {
                    sender.sendMessage(TextComponent.fromLegacyText(Locale.ERROR_INVALID_URLSKIN));
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(TextComponent.fromLegacyText(e.getMessage()));
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
