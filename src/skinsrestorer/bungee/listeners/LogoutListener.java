package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;

public class LogoutListener implements Listener {
	
	@EventHandler
	public void onServerConnect(final PlayerDisconnectEvent e) {
		SkinApplier.removeOnQuit(e.getPlayer());
	}
	
}
