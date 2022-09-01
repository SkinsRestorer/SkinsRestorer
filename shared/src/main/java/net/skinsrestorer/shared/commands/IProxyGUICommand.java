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
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IProxyGUICommand {
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    default void onDefault(ISRProxyPlayer player) {
        CooldownStorage cooldownStorage = getPlugin().getCooldownStorage();
        if (!player.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(player.getName())) {
            player.sendMessage(Locale.SKIN_COOLDOWN, String.valueOf(cooldownStorage.getCooldownSeconds(player.getName())));
            return;
        }
        player.sendMessage(Locale.SKINSMENU_OPEN);

        sendGuiOpenRequest(player);
    }

    default void sendGuiOpenRequest(ISRProxyPlayer player) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("OPENGUI");
            out.writeUTF(player.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendDataToServer("sr:messagechannel", b.toByteArray());
    }

    ISRProxyPlugin getPlugin();
}
