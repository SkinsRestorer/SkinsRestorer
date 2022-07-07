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
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bukkit.SkinSkull;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
@CommandAlias("Skull")
@CommandPermission("%skull")
public class SkullCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SkinSkull SkinsSkull;

    // TODO: is help even needed for /skins?
    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    @Default
    @CommandPermission("%skull")
    public void onDefault(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            /*if (!player.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(player.getName())) {
                player.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(player.getName())));
                return;
            }*/
            player.sendMessage("Here you go, your skull!");

            Optional<String> skinName = plugin.getSkinStorage().getSkinOfPlayer(player.getName());
            Optional<IProperty> skin = plugin.getSkinStorage().getSkinData(skinName.orElse(null));
            if (!skin.isPresent()) {
                player.sendMessage(Locale.NO_SKIN_DATA);
                return;
            }

            SkinsSkull.giveSkull(player, player.getName(), skin.get());
        });
    }
}