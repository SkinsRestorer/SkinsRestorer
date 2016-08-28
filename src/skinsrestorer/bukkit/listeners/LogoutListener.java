package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import skinsrestorer.bukkit.SkinsRestorer;

public class LogoutListener implements Listener{

	@EventHandler
	public void onQuit(final PlayerQuitEvent e) {
		final Player p = e.getPlayer();

		SkinsRestorer.getInstance().getFactory().removeOnQuit(p);
	}
	
	@EventHandler
	public void onKick(final PlayerKickEvent e) {
		final Player p = e.getPlayer();

		SkinsRestorer.getInstance().getFactory().removeOnQuit(p);
	}
	
}
