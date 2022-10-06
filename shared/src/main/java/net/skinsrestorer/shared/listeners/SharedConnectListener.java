package net.skinsrestorer.shared.listeners;

import net.skinsrestorer.shared.interfaces.ISRPlayer;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;
import net.skinsrestorer.shared.storage.Message;

public abstract class SharedConnectListener {
    protected void handleConnect(SRServerConnectedEvent event) {
        ISRProxyPlugin plugin = getPlugin();

        plugin.runAsync(() -> {
            if (plugin.isOutdated()) {
                ISRPlayer player = event.getPlayer();

                if (player.hasPermission("skinsrestorer.admincommand")) {
                    player.sendMessage(Message.OUTDATED);
                }
            }
        });
    }

    protected abstract ISRProxyPlugin getPlugin();
}
