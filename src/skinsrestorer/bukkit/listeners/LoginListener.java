package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import skinsrestorer.bukkit.SkinFactory;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class LoginListener implements Listener {

	//fix skin on player login
	@EventHandler(priority = EventPriority.LOW)
	public void onLoginEvent(final PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}
		final Player player = event.getPlayer();
		String name = player.getName();
		addemptUpdate(name);
		new Thread(new Runnable() {
		    public void run() {
		    	SkinFactory.applySkin(player);
		    }
		}).start();
	}
	public void addemptUpdate(String name){
		SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		try {
			skinprofile.attemptUpdate();
		} catch (SkinFetchFailedException e) {
			SkinsRestorer.getInstance().logInfo("Skin fetch failed for player "+name+": "+e.getMessage());
		}
	}
}