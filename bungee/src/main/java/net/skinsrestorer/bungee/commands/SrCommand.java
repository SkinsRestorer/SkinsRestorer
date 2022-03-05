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
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.interfaces.ISRPlayer;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.commands.ISRCommand;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapCommandSender;
import static net.skinsrestorer.bungee.utils.WrapperBungee.wrapPlayer;

@RequiredArgsConstructor
@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
public class SrCommand extends BaseCommand implements ISRCommand {
    @Getter
    private final SkinsRestorer plugin;

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSender sender, CommandHelp help) {
        onHelp(wrapCommandSender(sender), help);
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        onReload(wrapCommandSender(sender));
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        onStatus(wrapCommandSender(sender));
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("PLAYER|SKIN @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSender sender, PlayerOrSkin playerOrSkin, String[] targets) {
        onDrop(wrapCommandSender(sender), playerOrSkin, targets);
    }

    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSender sender, @Single OnlinePlayer target) {
        onProps(wrapCommandSender(sender), wrapPlayer(target.getPlayer()));
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    public void onApplySkin(CommandSender sender, @Single OnlinePlayer target) {
        onApplySkin(wrapCommandSender(sender), wrapPlayer(target.getPlayer()));
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [classic/slim]")
    public void onCreateCustom(CommandSender sender, String skinName, String skinUrl, @Optional SkinVariant skinVariant) {
        onCreateCustom(wrapCommandSender(sender), skinName, skinUrl, skinVariant);
    }

    @Subcommand("setskinall")
    @CommandCompletion("@Skin")
    @Description("Set the skin to evey player")
    @Syntax(" <Skin / Url> [classic/slim]")
    public void onSetSkinAll(CommandSender sender, String skin, @Optional SkinVariant skinVariant) {
        onSetSkinAll(wrapCommandSender(sender), skin, skinVariant);
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getProxy().getVersion();
    }

    @Override
    public String getProxyMode() {
        return "Bungee-Plugin";
    }

    @Override
    public List<IProperty> getPropertiesOfPlayer(ISRPlayer player) {
        LoginResult.Property[] props = ((InitialHandler) player.getWrapper().get(ProxiedPlayer.class)
                .getPendingConnection()).getLoginProfile().getProperties();

        if (props == null) {
            return null;
        } else {
            return Arrays.stream(props)
                    .map(property -> new GenericProperty(property.getName(), property.getValue(), property.getSignature()))
                    .collect(Collectors.toList());
        }
    }
}
