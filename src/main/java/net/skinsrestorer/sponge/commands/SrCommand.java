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
package net.skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.ServiceChecker;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
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
    public void onHelp(CommandSource source, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSource source) {
        Locale.load(plugin.getDataFolder(), logger);
        Config.load(plugin.getDataFolder(), plugin.getClass().getClassLoader().getResourceAsStream("config.yml"), logger);
        source.sendMessage(plugin.parseMessage(Locale.RELOAD));
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSource source) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
            source.sendMessage(plugin.parseMessage("§7Checking needed services for SR to work properly..."));

            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            if (Config.DEBUG || !(response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1))
                for (String result : results) {
                    if (Config.DEBUG || result.contains("✘"))
                        source.sendMessage(plugin.parseMessage(result));
                }
            source.sendMessage(plugin.parseMessage("§7Working UUID API count: §6" + response.getWorkingUUID()));
            source.sendMessage(plugin.parseMessage("§7Working Profile API count: §6" + response.getWorkingProfile()));

            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                source.sendMessage(plugin.parseMessage("§aThe plugin currently is in a working state."));
            else
                source.sendMessage(plugin.parseMessage("§cPlugin currently can't fetch new skins. \n Connection is likely blocked because of firewall. \n Please See http://skinsrestorer.net/firewall for more info"));
            source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
            source.sendMessage(plugin.parseMessage("§7SkinsRestorer §6v" + plugin.getVersion()));
            source.sendMessage(plugin.parseMessage("§7Server: §6" + Sponge.getGame().getPlatform().getMinecraftVersion()));
            source.sendMessage(plugin.parseMessage("§7BungeeMode: §6Sponge-Plugin"));
            source.sendMessage(plugin.parseMessage("§7Finished checking services."));
            source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
        });
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("PLAYER|SKIN @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSource source, PlayerOrSkin e, String[] targets) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            if (e == PlayerOrSkin.PLAYER)
                for (String targetPlayer : targets)
                    plugin.getSkinStorage().removePlayerSkin(targetPlayer);
            else
                for (String targetSkin : targets)
                    plugin.getSkinStorage().removeSkinData(targetSkin);
            String targetList = Arrays.toString(targets).substring(1, Arrays.toString(targets).length() - 1);
            source.sendMessage(plugin.parseMessage(Locale.DATA_DROPPED.replace("%playerOrSkin", e.toString()).replace("%targets", targetList)));
        });
    }

    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSource source, @Single OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            Collection<ProfileProperty> prop = target.getPlayer().getProfile().getPropertyMap().get("textures");

            if (prop == null) {
                source.sendMessage(plugin.parseMessage(Locale.NO_SKIN_DATA));
                return;
            }

            prop.forEach(profileProperty -> {
                byte[] decoded = Base64.getDecoder().decode(profileProperty.getValue());

                String decodedString = new String(decoded);
                JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
                String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
                long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
                String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date(timestamp));

                source.sendMessage(plugin.parseMessage("§aRequest time: §e" + requestDate));
                source.sendMessage(plugin.parseMessage("§aprofileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString()));
                source.sendMessage(plugin.parseMessage("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString()));
                source.sendMessage(plugin.parseMessage("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length() - 1)));
                source.sendMessage(plugin.parseMessage("§cMore info in console!"));

                // Console
                logger.info("§aName: §8" + profileProperty.getName());
                logger.info("§aValue : §8" + profileProperty.getValue());
                logger.info("§aSignature : §8" + profileProperty.getSignature());
                logger.info("§aValue Decoded: §e" + Arrays.toString(decoded));
            });
        });
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    public void onApplySkin(CommandSource source, @Single OnlinePlayer target) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            try {
                final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(target.getPlayer().getName());

                plugin.getSkinApplierSponge().updateProfileSkin(target.getPlayer().getProfile(), skin);
                source.sendMessage(plugin.parseMessage("success: player skin has been refreshed!"));
            } catch (SkinRequestException ignored) {
                source.sendMessage(plugin.parseMessage("ERROR: player skin could NOT be refreshed!"));
            }
        });
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [steve/slim]")
    public void onCreateCustom(CommandSource source, String skinName, String skinUrl, @Optional SkinType skinType) {
        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            try {
                if (C.validUrl(skinUrl)) {
                    plugin.getSkinStorage().setSkinData(skinName, plugin.getMineSkinAPI().genSkin(skinUrl, String.valueOf(skinType)),
                            Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                    source.sendMessage(plugin.parseMessage(Locale.SUCCESS_CREATE_SKIN.replace("%skin", skinName)));
                } else {
                    source.sendMessage(plugin.parseMessage(Locale.ERROR_INVALID_URLSKIN));
                }
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.parseMessage(e.getMessage()));
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
