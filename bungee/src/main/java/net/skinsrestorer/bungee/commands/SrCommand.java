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

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bungee.SkinApplierBungeeShared;
import net.skinsrestorer.bungee.SkinsRestorerBungee;
import net.skinsrestorer.bungee.utils.WrapperBungee;
import net.skinsrestorer.shared.commands.SharedSRCommand;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
@SuppressWarnings({"unused"})
public class SrCommand extends SharedSRCommand {
    private final SkinsRestorerBungee plugin;
    private final SkinApplierBungeeShared skinApplier;
    private final WrapperBungee wrapper;

    @Inject
    public SrCommand(SkinsRestorerBungee plugin, MojangAPI mojangAPI, SkinStorage skinStorage, SettingsManager settings, SRLogger logger, SkinApplierBungeeShared skinApplier, WrapperBungee wrapper,
                     @OnlinePlayersMethod CallableValue<Collection<ISRPlayer>> onlinePlayersFunction) {
        super(plugin, mojangAPI, skinStorage, settings, logger, onlinePlayersFunction);
        this.plugin = plugin;
        this.skinApplier = skinApplier;
        this.wrapper = wrapper;
    }

    @HelpCommand
    @Syntax("%helpHelpCommand")
    public void onHelp(CommandSender sender, CommandHelp help) {
        onHelp(wrapper.commandSender(sender), help);
    }

    @Subcommand("reload")
    @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        onReload(wrapper.commandSender(sender));
    }

    @Subcommand("status")
    @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        onStatus(wrapper.commandSender(sender));
    }

    @Subcommand("drop|remove")
    @CommandPermission("%srDrop")
    @CommandCompletion("PLAYER|SKIN @players @players @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSender sender, PlayerOrSkin playerOrSkin, String target) {
        onDrop(wrapper.commandSender(sender), playerOrSkin, target);
    }

    @Subcommand("props")
    @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSender sender, @Single OnlinePlayer target) {
        onProps(wrapper.commandSender(sender), wrapper.player(target.getPlayer()));
    }

    @Subcommand("applyskin")
    @CommandPermission("%srApplySkin")
    @CommandCompletion("@players")
    @Description("%helpSrApplySkin")
    @Syntax(" <target>")
    public void onApplySkin(CommandSender sender, @Single OnlinePlayer target) {
        onApplySkin(wrapper.commandSender(sender), wrapper.player(target.getPlayer()));
    }

    @Subcommand("createcustom")
    @CommandPermission("%srCreateCustom")
    @CommandCompletion("@skinName @skinUrl")
    @Description("%helpSrCreateCustom")
    @Syntax(" <skinName> <skinUrl> [classic/slim]")
    public void onCreateCustom(CommandSender sender, String skinName, String skinUrl, @Optional SkinVariant skinVariant) {
        onCreateCustom(wrapper.commandSender(sender), skinName, skinUrl, skinVariant);
    }

    @Subcommand("setskinall")
    @CommandCompletion("@Skin")
    @Description("Set the skin to every player")
    @Syntax(" <Skin / Url> [classic/slim]")
    public void onSetSkinAll(CommandSender sender, String skin, @Optional SkinVariant skinVariant) {
        onSetSkinAll(wrapper.commandSender(sender), skin, skinVariant);
    }

    @Subcommand("applyskinall")
    @Description("Re-apply the skin for every player")
    public void onApplySkinAll(CommandSender sender) {
        onApplySkinAll(wrapper.commandSender(sender));
    }

    @Subcommand("purgeolddata")
    @Description("Purge old skin data from over x days ago")
    @Syntax(" <targetdaysold>")
    public void onPurgeOldData(CommandSender sender, int days) {
        onPurgeOldData(wrapper.commandSender(sender), days);
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
        List<IProperty> props = skinApplier.getProperties(player.getWrapper().get(ProxiedPlayer.class));

        if (props == null) {
            return Collections.emptyList();
        } else {
            return props.stream()
                    .map(property -> new GenericProperty(property.getName(), property.getValue(), property.getSignature()))
                    .collect(Collectors.toList());
        }
    }
}
