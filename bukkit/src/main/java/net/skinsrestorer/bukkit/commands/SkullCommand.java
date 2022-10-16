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
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

import static net.skinsrestorer.bukkit.utils.WrapperBukkit.wrapPlayer;

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
            ISRPlayer srPlayer = wrapPlayer(player);

            CooldownStorage cooldownStorage = plugin.getCooldownStorage();
            if (!player.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(player.getName())) {
                srPlayer.sendMessage(Message.SKIN_COOLDOWN, cooldownStorage.getCooldownSeconds(player.getName()));
                return;
            }

            Optional<String> skinName = plugin.getSkinStorage().getSkinNameOfPlayer(srPlayer.getName());
            if (skinName.isPresent()) {
                Optional<IProperty> skinData = plugin.getSkinStorage().getSkinData(skinName.get(), false);

                if (skinData.isPresent()) {
                    srPlayer.sendMessage("Here you go, your skull!");
                    SkinsSkull.giveSkull(plugin, srPlayer.getName(), player, skinData.get().getValue());
                } else {
                    srPlayer.sendMessage(Message.NO_SKIN_DATA);
                }
            }
        });
    }

}
