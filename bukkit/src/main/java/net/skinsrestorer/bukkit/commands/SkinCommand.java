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
package net.skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.commands.ISkinCommand;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@CommandAlias("skin")
@CommandPermission("%skin")
public class SkinCommand extends BaseCommand implements ISkinCommand {
    @Getter
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @Default
    @SuppressWarnings({"deprecation"})
    public void onDefault(CommandSender sender) {
        onHelp(sender, getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    @SuppressWarnings({"unused"})
    public void onSkinSetShort(Player player, @Single String skin) {
        onSkinSetOther(player, new OnlinePlayer(player), skin, null);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSender sender, CommandHelp help) {
        ISRCommandSender wrapped = wrap(sender);
        if (Config.ENABLE_CUSTOM_HELP)
            sendHelp(wrapped);
        else
            help.showHelp();
    }

    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    public void onSkinClear(Player player) {
        onSkinClearOther(player, new OnlinePlayer(player));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSender sender, @Single OnlinePlayer target) {
        ISRCommandSender wrapped = wrap(sender);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
                return;
            }

            final Player player = target.getPlayer();
            final String pName = player.getName();
            final String skin = plugin.getSkinStorage().getDefaultSkinName(pName, true);

            // remove users defined skin from database
            plugin.getSkinStorage().removeSkin(pName);

            if (setSkin(wrap(sender), new PlayerWrapper(player), skin, false, true, null)) {
                if (sender == player)
                    sender.sendMessage(Locale.SKIN_CLEAR_SUCCESS);
                else
                    sender.sendMessage(Locale.SKIN_CLEAR_ISSUER.replace("%player", pName));
            }
        });
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    @SuppressWarnings({"unused"})
    public void onSkinUpdate(Player player) {
        onSkinUpdateOther(player, new OnlinePlayer(player));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSender sender, @Single OnlinePlayer target) {
        ISRCommandSender wrapped = wrap(sender);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!sender.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(sender.getName())) {
                sender.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(sender.getName())));
                return;
            }

            final Player player = target.getPlayer();
            java.util.Optional<String> skin = plugin.getSkinStorage().getSkinName(player.getName());

            try {
                if (skin.isPresent()) {
                    //filter skinUrl
                    if (skin.get().startsWith(" ")) {
                        sender.sendMessage(Locale.ERROR_UPDATING_URL);
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin.get())) {
                        sender.sendMessage(Locale.ERROR_UPDATING_SKIN);
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = java.util.Optional.of(plugin.getSkinStorage().getDefaultSkinName(player.getName(), true));
                }
            } catch (SkinRequestException e) {
                sender.sendMessage(e.getMessage());
                return;
            }

            // TODO: Use its own code instead of bloat #setSkin()
            if (setSkin(wrap(sender), new PlayerWrapper(player), skin.get(), false, false, null)) {
                if (sender == player)
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN);
                else
                    sender.sendMessage(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", player.getName()));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @CommandCompletion("@skin")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player player, String[] skin) {
        if (skin.length == 0)
            throw new InvalidCommandArgument(true);

        onSkinSetOther(player, new OnlinePlayer(player), skin[0], null);
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players @skin")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSender sender, OnlinePlayer target, String skin, @Optional SkinType skinType) {
        ISRCommandSender wrapped = wrap(sender);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final Player player = target.getPlayer();
            if (Config.PER_SKIN_PERMISSIONS && !sender.hasPermission("skinsrestorer.skin." + skin)) {
                if (!sender.hasPermission("skinsrestorer.ownskin") && !sender.getName().equalsIgnoreCase(player.getName()) || !skin.equalsIgnoreCase(sender.getName())) {
                    sender.sendMessage(Locale.PLAYER_HAS_NO_PERMISSION_SKIN);
                    return;
                }
            }

            if (setSkin(wrapped, new PlayerWrapper(player), skin, true, false, skinType) && (sender != player))
                sender.sendMessage(Locale.ADMIN_SET_SKIN.replace("%player", player.getName()));
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @CommandCompletion("@skinUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    @SuppressWarnings({"unused"})
    public void onSkinSetUrl(Player player, String url, @Optional SkinType skinType) {
        if (!C.validUrl(url)) {
            player.sendMessage(Locale.ERROR_INVALID_URLSKIN);
            return;
        }

        onSkinSetOther(player, new OnlinePlayer(player), url, skinType);
    }

    private ISRCommandSender wrap(CommandSender sender) {
        return new ISRCommandSender() {
            @Override
            public void sendMessage(String message) {
                sender.sendMessage(message);
            }

            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }
        };
    }

    @Override
    public void clearSkin(PlayerWrapper player) {
        SkinsRestorerAPI.getApi().applySkin(player, emptySkin);
        plugin.getSkinApplierBukkit().updateSkin(player.get(Player.class)); // TODO: make not platform specific
    }
}
