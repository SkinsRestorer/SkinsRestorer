package skinsrestorer.bungee.listeners;

import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class LoginListener implements Listener {

	//load skin data on login
	@EventHandler(priority = EventPriority.LOW)
	public void onPreLogin(final PostLoginEvent event) {
		
		if (SkinsRestorer.getInstance().isAutoInEnabled()){
			if (ConfigStorage.getInstance().USE_AUTOIN_SKINS==true&&SkinsRestorer.getInstance().getAutoInAPI().getPremiumStatus(event.getPlayer().getName())==com.gmail.bartlomiejkmazur.autoin.api.PremiumStatus.PREMIUM){
				return;
			}
		}
		final String name = event.getPlayer().getName();
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					skinprofile.attemptUpdateBungee();
				} catch (SkinFetchFailedException e) {
					SkinsRestorer.getInstance().logInfo("Skin fetch failed for player " + name + ": " + e.getMessage());
			}
			}
		});
	}
}
