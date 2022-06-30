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
package net.skinsrestorer.shared.commands;

import co.aikar.commands.CommandHelp;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;

public interface IProxyGUICommand {
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    default void onDefault(ISRProxyPlayer player) {
        if (!player.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(player.getName())) {
            player.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", String.valueOf(CooldownStorage.getCooldown(player.getName()))));
            return;
        }
        player.sendMessage(Locale.SKINSMENU_OPEN);

        getPlugin().sendGuiOpenRequest(player);
    }

    ISRProxyPlugin getPlugin();
}
