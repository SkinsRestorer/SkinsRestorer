package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;

public class LoginListener implements Listener {

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPostLogin(final PostLoginEvent e) {
		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
				&& (e.getPlayer().hasPermission("skinsrestorer.cmds")))
			e.getPlayer().sendMessage(C.c(Locale.OUTDATED));

		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		SkinApplier.applySkin(e.getPlayer());
	}

}
