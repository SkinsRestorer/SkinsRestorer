package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import skinsrestorer.bukkit.SkinFactory;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class LoginListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(AsyncPlayerPreLoginEvent event){
		if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
			return;
		}
		String name = event.getName();
		SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		try {
			skinprofile.attemptUpdate();
		} catch (SkinFetchFailedException e) {
			SkinsRestorer.getInstance().logInfo("Skin fetch failed for player "+name+": "+e.getMessage());
		}
    }
	//fix skin on player login
	@EventHandler(priority = EventPriority.LOW)
	public void onLoginEvent(PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}
		final Player player = event.getPlayer();
	SkinFactory.applySkin(player);
	}
}