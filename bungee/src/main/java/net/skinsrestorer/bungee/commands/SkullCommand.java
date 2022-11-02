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
package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.SkullSource;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.commands.IProxySkullCommand;

import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapCommandSender;
import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapPlayer;

@Getter
@RequiredArgsConstructor
@CommandAlias("skull")
@CommandPermission("%skull")
public class SkullCommand extends BaseCommand implements IProxySkullCommand {
    private final SkinsRestorer plugin;

    @HelpCommand
    @Syntax("%helpHelpCommand")
    public void onHelp(CommandSender sender, CommandHelp help) {
        onHelp(wrapCommandSender(sender), help);
    }

    @Default
    @CommandPermission("%skullGet")
    public void onDefault(ProxiedPlayer player) {
        onDefault(wrapPlayer(player));
    }

    @Subcommand("get")
    @CommandPermission("%skullGet")
    @Description("%helpSkullGet")
    @Syntax("%SyntaxSkullGet")
    public void onGet(ProxiedPlayer player, SkullSource skullSource, String value, @Optional SkinVariant skinVariant) {
        onGet(wrapPlayer(player), skullSource, value, skinVariant);
    }

    @Subcommand("give")
    @CommandPermission("%skullGive")
    @CommandCompletion("@players")
    @Description("%helpSkullGive")
    @Syntax("%SyntaxSkullGive")
    public void onGive(CommandSender sender, ProxiedPlayer player, SkullSource skullSource, String value, @Optional SkinVariant skinVariant) {
        onGive(wrapCommandSender(sender), wrapPlayer(player), skullSource, value, skinVariant);
    }

    @Subcommand("update")
    @CommandPermission("%skullUpdate")
    @Description("%helpSkullUpdate")
    public void onUpdate(ProxiedPlayer player) {
        onUpdate(wrapPlayer(player));
    }

    @Subcommand("props")
    @CommandPermission("%skullProps")
    @Description("%helpSkullProps")
    public void onProps(ProxiedPlayer player) {
        onUpdate(wrapPlayer(player));
    }
}
