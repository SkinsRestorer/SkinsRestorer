package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.LoginResult.Property;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

public class LoginListener implements Listener {

	@EventHandler
	public void onLogin(PreLoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS || e.isCancelled() || e.getConnection() == null
				|| e.getConnection().getName() == null)
			return;

		String skin = SkinStorage.getPlayerSkin(e.getConnection().getName());

		e.registerIntent(SkinsRestorer.getInstance());
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

			@Override
			public void run() {
				try {
					Property props = (Property) MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));
					SkinStorage.setSkinData(skin, props);
				} catch (SkinRequestException ex) {
				} catch (NullPointerException ex) {
					ex.printStackTrace();
					System.out.println("============================================");
					System.out.println("ConnName : " + e.getConnection().getName());
					System.out.println("SKIN : " + skin);
					System.out.println("============================================");
				}
				e.completeIntent(SkinsRestorer.getInstance());
			}

		});
	}

	@EventHandler
	public void onServerConnect(ServerConnectedEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		SkinApplier.applySkin(e.getPlayer());
	}

	@EventHandler
	public void onServerConnect(PostLoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		SkinApplier.applySkin(e.getPlayer());
	}

}
