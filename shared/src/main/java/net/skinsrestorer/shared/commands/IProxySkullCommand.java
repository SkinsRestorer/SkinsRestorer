package net.skinsrestorer.shared.commands;

import co.aikar.commands.CommandHelp;
import net.skinsrestorer.api.interfaces.ISRCommandSender;
import net.skinsrestorer.api.interfaces.ISRProxyPlayer;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IProxySkullCommand {
    default void onHelp(ISRCommandSender sender, CommandHelp help) {
        sender.sendMessage("SkinsRestorer Help");
        help.showHelp();
    }

    default void onDefault(ISRProxyPlayer player) {
        // todo: add seperate cooldown storage
        /* if (!player.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(player.getName())) {
            player.sendMessage(Locale.SKIN_COOLDOWN.replace("%s", String.valueOf(CooldownStorage.getCooldown(player.getName()))));
            return;
        } */

        player.sendMessage("Here you go, your skull!");

        sendGiveSkullRequest(player);
    }

    default void sendGiveSkullRequest(ISRProxyPlayer player) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("GIVESKULL");
            out.writeUTF(player.getName());
            //todo: add props here
        } catch (IOException e) {
            e.printStackTrace();
        }

        player.sendDataToServer("sr:messagechannel", b.toByteArray());
    }

    ISRProxyPlugin getPlugin();
}
