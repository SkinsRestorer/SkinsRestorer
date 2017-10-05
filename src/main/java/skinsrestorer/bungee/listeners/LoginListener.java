package skinsrestorer.bungee.listeners;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.event.ServerSwitchEvent;
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
	public void onServerChange(final ServerSwitchEvent e){
		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
				&& e.getPlayer().hasPermission("skinsrestorer.cmds"))
			e.getPlayer().sendMessage(C.c(Locale.OUTDATED));

		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		if (e.getPlayer().getPendingConnection().isOnlineMode()){
			SkinsRestorer.getInstance().getProxy().getScheduler().schedule(SkinsRestorer.getInstance(), new Runnable(){

				@Override
				public void run() {
					SkinApplier.applySkin(e.getPlayer());
				}
			},10, TimeUnit.MILLISECONDS);
			}else {
		SkinApplier.applySkin(e.getPlayer());
		}
	}
}
