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

import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public interface ISkinCommand {
    IProperty emptySkin = SkinsRestorerAPI.getApi().createProperty("textures", "", "");

    default void sendHelp(ISRCommandSender sender) {
        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);

        sender.sendMessage(Locale.CUSTOM_HELP_IF_ENABLED.replace("%ver%", getPlugin().getVersion()));

        if (!Locale.SR_LINE.isEmpty())
            sender.sendMessage(Locale.SR_LINE);
    }

    default void rollback(String pName, String oldSkinName, boolean save) {
        if (save)
            getPlugin().getSkinStorage().setSkinName(pName, oldSkinName);
    }

    ISRPlugin getPlugin();

    // if save is false, we won't save the skin name
    // because default skin names shouldn't be saved as the users custom skin
    default boolean setSkin(ISRCommandSender sender, PlayerWrapper player, String skin, boolean save, boolean clear, SkinType skinType) {
        ISRPlugin plugin = getPlugin();

        if (skin.equalsIgnoreCase("null")) {
            sender.sendMessage(Locale.INVALID_PLAYER.replace("%player", skin));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear && !sender.hasPermission("skinsrestorer.bypassdisabled")
                && Config.DISABLED_SKINS.stream().anyMatch(skin::equalsIgnoreCase)) {
            sender.sendMessage(Locale.SKIN_DISABLED);
            return false;
        }

        final String senderName = sender.getName();
        if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(senderName)) {
            sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", String.valueOf(CooldownStorage.getCooldown(senderName))));
            return false;
        }

        final String pName = player.getName();
        final java.util.Optional<String> oldSkinName = plugin.getSkinStorage().getSkinName(pName);
        if (C.validUrl(skin)) {
            if (!sender.hasPermission("skinsrestorer.command.set.url")
                    && !Config.SKIN_WITHOUT_PERM
                    && !clear) { // ignore /skin clear when defaultSkin = url
                sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_URL);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                sender.sendMessage(Locale.SKINURL_DISALLOWED);
                return false;
            }

            // Apply cooldown to sender
            CooldownStorage.setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

            try {
                sender.sendMessage(Locale.MS_UPDATING_SKIN);
                String skinentry = " " + pName; // so won't overwrite premium player names
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);

                IProperty generatedSkin = SkinsRestorerAPI.getApi().genSkinUrl(skin, String.valueOf(skinType));
                plugin.getSkinStorage().setSkinData(skinentry, generatedSkin,
                        System.currentTimeMillis() + Duration.of(100, ChronoUnit.YEARS).toMillis()); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setSkinName(pName, skinentry); // set player to "whitespaced" name then reload skin
                SkinsRestorerAPI.getApi().applySkin(player, generatedSkin);
                
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", "skinUrl"));
                
                return true;
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
            } catch (Exception e) {
                plugin.getSrLogger().debug("[ERROR] Exception: could not generate skin url:" + skin + "\nReason= " + e.getMessage());
                sender.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            }
        } else {
            // If skin is not an url, it's a username
            try {
                if (save)
                    plugin.getSkinStorage().setSkinName(pName, skin);
                // TODO: #getSkinForPlayer() is nested and on different places around bungee/sponge/velocity
                SkinsRestorerAPI.getApi().applySkin(player, skin);
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", skin)); // TODO: should this not be sender? -> hidden skin set?
                return true;
            } catch (SkinRequestException e) {
                if (clear) {
                    clearSkin(player);

                    return true;
                }
                sender.sendMessage(e.getMessage());
            }
        }
        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        CooldownStorage.setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        rollback(pName, oldSkinName.orElse(pName), save);
        return false;
    }

    void clearSkin(PlayerWrapper player);

    void runAsync(Runnable runnable);
    
    enum SkinType {
        STEVE,
        SLIM,
    }
}
