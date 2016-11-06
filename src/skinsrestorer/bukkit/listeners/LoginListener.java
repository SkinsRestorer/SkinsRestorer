package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;

public class LoginListener implements Listener {

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		PacketListener.inject(e.getPlayer());

		final Player p = e.getPlayer();

		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
				&& (p.isOp() || p.hasPermission("skinsrestorer.cmds")))
			p.sendMessage(C.c(Locale.OUTDATED));
	}
}