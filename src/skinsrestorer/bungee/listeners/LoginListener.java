package skinsrestorer.bungee.listeners;

import skinsrestorer.bungee.SkinFactoryBungee;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class LoginListener implements Listener {

	//load skin data on login
	@EventHandler(priority = EventPriority.LOW)
	public void onPreLogin(final LoginEvent event) {
		if (event.isCancelled()) {
			return;
		}
		final String name = event.getConnection().getName();
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		event.registerIntent(SkinsRestorer.getInstance());
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					skinprofile.attemptUpdateBungee();
				} catch (SkinFetchFailedException e) {
					SkinsRestorer.getInstance().logInfo("Skin fetch failed for player " + name + ": " + e.getMessage());
				} finally {
					event.completeIntent(SkinsRestorer.getInstance());
				}
			}
		});
	}

	//fix profile on login
	@EventHandler(priority = EventPriority.LOW)
	public void onPostLogin(final PostLoginEvent event) {
	ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
		public void run() {
		SkinFactoryBungee.getFactory().applySkin(event.getPlayer());
			}
		});
}
}
