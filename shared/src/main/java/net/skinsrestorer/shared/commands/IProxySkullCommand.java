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
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRCommandSender;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

public interface IProxySkullCommand {
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    default void onDefault(ISRProxyPlayer player) {
        // todo: add seperate cooldown storage
        ISRPlugin plugin = getPlugin();
        CooldownStorage cooldownStorage = plugin.getCooldownStorage();
        if (!player.hasPermission("skinsrestorer.bypasscooldown") && cooldownStorage.hasCooldown(player.getName())) {
            player.sendMessage(Message.SKIN_COOLDOWN, String.valueOf(cooldownStorage.getCooldownSeconds(player.getName())));
            return;
        }


        Optional<String> skinName = plugin.getSkinStorage().getSkinNameOfPlayer(player.getName());
        if (skinName.isPresent()) {
            Optional<IProperty> skinData = plugin.getSkinStorage().getSkinData(skinName.get(), false);

            if (skinData.isPresent()) {
                player.sendMessage("Here you go, your skull!");
                sendGiveSkullRequest(player, skinData.get().getValue());
            } else {
                player.sendMessage(Message.NO_SKIN_DATA);
            }
        }
    }


    default void sendGiveSkullRequest(ISRProxyPlayer player, String b64stringTexture) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("GiveSkull");
            out.writeUTF(player.getName());
            out.writeUTF(b64stringTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendDataToServer("sr:messagechannel", b.toByteArray());
    }

    ISRProxyPlugin getPlugin();
}
