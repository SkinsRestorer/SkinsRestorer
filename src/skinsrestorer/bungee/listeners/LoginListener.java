package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.LoginResult.Property;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

public class LoginListener implements Listener {

	@EventHandler
	public void onLogin(final LoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		SkinApplier.applySkin(e.getConnection());

		SkinsRestorer.getInstance().getExecutor().submit(new Runnable() {

			@Override
			public void run() {
				try {
					String skin = SkinStorage.getPlayerSkin(e.getConnection().getName());

					if (skin == null)
						skin = e.getConnection().getName();

					Property props = (Property) MojangAPI.getSkinProperty(skin, MojangAPI.getUUID(skin));
					SkinStorage.setSkinData(skin, props);
				} catch (SkinRequestException ex) {
				}
			}
		});
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPostLogin(final PostLoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
				&& (e.getPlayer().hasPermission("skinsrestorer.cmds")))
			e.getPlayer().sendMessage(C.c(Locale.OUTDATED));

		SkinApplier.applySkin(e.getPlayer());
	}

}
