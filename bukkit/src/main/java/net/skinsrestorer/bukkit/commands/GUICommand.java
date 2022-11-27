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
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SkinsGUI;
import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.storage.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import static net.skinsrestorer.bukkit.utils.WrapperBukkit.wrapPlayer;

@RequiredArgsConstructor
@CommandAlias("skins")
@CommandPermission("%skins")
@SuppressWarnings({"unused"})
public class GUICommand extends BaseCommand {
    private final SkinsRestorerBukkit plugin;

    // TODO: is help even needed for /skins?
    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    @Default
    @CommandPermission("%skins")
    public void onDefault(Player player) {
        ISRPlayer srPlayer = wrapPlayer(player);
        plugin.runAsync(() -> {
            if (!player.hasPermission("skinsrestorer.bypasscooldown") && plugin.getCooldownStorage().hasCooldown(player.getName())) {
                srPlayer.sendMessage(Message.SKIN_COOLDOWN, plugin.getCooldownStorage().getCooldownSeconds(player.getName()));
                return;
            }
            srPlayer.sendMessage(Message.SKINSMENU_OPEN);

            Inventory inventory = SkinsGUI.createGUI(plugin, srPlayer, 0);
            plugin.runSync(() -> player.openInventory(inventory));
        });
    }
}
