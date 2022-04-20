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
package net.skinsrestorer.sponge8.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.shared.commands.ISkinCommand;
import net.skinsrestorer.sponge8.SkinsRestorer;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static net.skinsrestorer.sponge8.utils.WrapperSponge.wrapCommandSender;
import static net.skinsrestorer.sponge8.utils.WrapperSponge.wrapPlayer;

@RequiredArgsConstructor
@CommandAlias("skin")
@CommandPermission("%skin")
@SuppressWarnings({"unused"})
public class SkinCommand extends BaseCommand implements ISkinCommand {
    @Getter
    private final SkinsRestorer plugin;

    @Default
    public void onDefault(CommandCause source) {
        onDefault(wrapCommandSender(source));
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    public void onSkinSetShort(ServerPlayer player, @Single String skin) {
        onSkinSetShort(wrapPlayer(player), skin);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandCause source, CommandHelp help) {
        onHelp(wrapCommandSender(source), help);
    }

    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    public void onSkinClear(ServerPlayer player) {
        onSkinClear(wrapPlayer(player));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandCause source, @Single OnlinePlayer target) {
        onSkinClearOther(wrapCommandSender(source), wrapPlayer(target.getPlayer()));
    }


    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    public void onSkinUpdate(ServerPlayer player) {
        onSkinUpdate(wrapPlayer(player));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandCause source, @Single OnlinePlayer target) {
        onSkinUpdateOther(wrapCommandSender(source), wrapPlayer(target.getPlayer()));
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @CommandCompletion("@skin")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(ServerPlayer player, String[] skin) {
        onSkinSet(wrapPlayer(player), skin);
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players @skin")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandCause source, OnlinePlayer target, String skin, @Optional SkinVariant skinVariant) {
        onSkinSetOther(wrapCommandSender(source), wrapPlayer(target.getPlayer()), skin, skinVariant);
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @CommandCompletion("@skinUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    public void onSkinSetUrl(ServerPlayer player, String url, @Optional SkinVariant skinVariant) {
        onSkinSetUrl(wrapPlayer(player), url, skinVariant);
    }

    @Override
    public void clearSkin(ISRPlayer player) {
        // TODO: Maybe do something here?
    }
}
