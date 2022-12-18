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

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.api.reflection.exception.ReflectionException;
import net.skinsrestorer.bukkit.SkinApplierBukkit;
import net.skinsrestorer.bukkit.SkinsRestorerBukkit;
import net.skinsrestorer.bukkit.utils.WrapperBukkit;
import net.skinsrestorer.shared.commands.SharedSRCommand;
import net.skinsrestorer.shared.injector.OnlinePlayersMethod;
import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.storage.CallableValue;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.connections.MojangAPI;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.*;

@CommandAlias("sr|skinsrestorer")
@CommandPermission("%sr")
@SuppressWarnings({"unused"})
public class SrCommand extends SharedSRCommand {
    private final SkinsRestorerBukkit plugin;
    private final SkinApplierBukkit skinApplier;
    private final WrapperBukkit wrapper;

    @Inject
    public SrCommand(SkinsRestorerBukkit plugin, MojangAPI mojangAPI, SkinStorage skinStorage, SettingsManager settings, SRLogger logger, SkinApplierBukkit skinApplier, WrapperBukkit wrapperBukkit,
                     @OnlinePlayersMethod CallableValue<Collection<ISRPlayer>> onlinePlayersFunction) {
        super(plugin, mojangAPI, skinStorage, settings, logger, onlinePlayersFunction);
        this.plugin = plugin;
        this.skinApplier = skinApplier;
        this.wrapper = wrapperBukkit;
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
    public void onDrop(CommandSender sender, PlayerOrSkin playerOrSkin, String targets) {
        onDrop(wrapper.commandSender(sender), playerOrSkin, targets);
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
    public void onCreateCustom(CommandSender sender, String name, String skinUrl, @Optional SkinVariant skinVariant) {
        onCreateCustom(wrapper.commandSender(sender), name, skinUrl, skinVariant);
    }

    @Subcommand("setskinall")
    @CommandCompletion("@Skin")
    @Description("Set the skin to every player")
    @Syntax(" <Skin / Url> [classic/slim]")
    public void onSetSkinAll(CommandSender sender, String skinUrl, @Optional SkinVariant skinVariant) {
        onSetSkinAll(wrapper.commandSender(sender), skinUrl, skinVariant);
    }

    @Subcommand("applyskinall")
    @Description("Re-apply the skin for every player")
    public void onApplySkinAll(CommandSender sender) {
        onApplySkinAll(wrapCommandSender(sender));
    }

    @Subcommand("purgeolddata")
    @Description("Purge old skin data from over x days ago")
    @Syntax(" <targetdaysold>")
    public void onPurgeOldData(CommandSender sender, int days) {
        onPurgeOldData(wrapper.commandSender(sender), days);
    }

    @Override
    public void reloadCustomHook() {
        skinApplier.setOptFileChecked(false);
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getServer().getVersion();
    }

    @Override
    public String getProxyMode() {
        return String.valueOf(plugin.isProxyMode());
    }

    @Override
    public List<IProperty> getPropertiesOfPlayer(ISRPlayer player) {
        try {
            Map<String, Collection<IProperty>> propertyMap = skinApplier.getPlayerProperties(player.getWrapper().get(Player.class));
            return new ArrayList<>(propertyMap.get(IProperty.TEXTURES_NAME));
        } catch (ReflectionException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
