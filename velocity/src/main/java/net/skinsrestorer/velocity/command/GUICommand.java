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
package net.skinsrestorer.velocity.command;

import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import net.skinsrestorer.shared.commands.ShredProxyGUICommand;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;

import static net.skinsrestorer.velocity.utils.WrapperVelocity.wrapCommandSender;
import static net.skinsrestorer.velocity.utils.WrapperVelocity.wrapPlayer;

@Getter
@CommandAlias("skins")
@CommandPermission("%skins")
@SuppressWarnings({"unused"})
public class GUICommand extends ShredProxyGUICommand {
    public GUICommand(ISRProxyPlugin plugin) {
        super(plugin);
    }

    @HelpCommand
    public void onHelp(CommandSource sender, CommandHelp help) {
        onHelp(wrapCommandSender(sender), help);
    }

    @Default
    @CommandPermission("%skins")
    public void onDefault(Player player) {
        onDefault(wrapPlayer(player));
    }
}
