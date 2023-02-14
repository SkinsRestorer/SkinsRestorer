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
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.bukkit.gui.SkinsGUI;
import net.skinsrestorer.shared.SkinsRestorerLocale;
import net.skinsrestorer.shared.interfaces.SRPlayer;
import net.skinsrestorer.shared.storage.Message;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import javax.inject.Inject;

@SuppressWarnings({"unused"})
@CommandAlias("skins")
@CommandPermission("%skins")
@Conditions("cooldown")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GUICommand extends BaseCommand {
    private final SRBukkitAdapter plugin;
    private final SkinsRestorerLocale locale;
    private final SRLogger logger;
    private final SkinStorageImpl skinStorage;
    private final SkinsGUI.ServerGUIActions serverGUIActions;
    private final Server server;

    @Default
    public void onDefault(SRPlayer srPlayer) {
        plugin.runAsync(() -> {
            srPlayer.sendMessage(Message.SKINSMENU_OPEN);

            Inventory inventory = SkinsGUI.createGUI(serverGUIActions, locale, logger, server, skinStorage, srPlayer, 0);
            plugin.runSync(() -> srPlayer.getAs(Player.class).openInventory(inventory));
        });
    }
}
