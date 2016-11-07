package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.ReflectionUtil;

public class LoginListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(final PlayerJoinEvent e) {
		final Player p = e.getPlayer();

		if (ReflectionUtil.serverVersion.contains("1_7"))
			PacketListener17.inject(p);
		else
			PacketListener.inject(p);

		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
				&& (p.isOp() || p.hasPermission("skinsrestorer.cmds")))
			p.sendMessage(C.c(Locale.OUTDATED));
	}
}