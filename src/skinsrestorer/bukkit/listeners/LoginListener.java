package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class LoginListener implements Listener {

	//fix skin on player login
	@EventHandler(priority = EventPriority.LOW)
	public void onLoginEvent(final PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}
		if (SkinsRestorer.getInstance().isAutoInEnabled()){
			if (ConfigStorage.getInstance().USE_AUTOIN_SKINS==true&&SkinsRestorer.getInstance().getAutoInAPI().getPremiumStatus(event.getPlayer().getName())==com.gmail.bartlomiejkmazur.autoin.api.PremiumStatus.PREMIUM){
				return;
			}
		}
		//Updating and applying skin.
		updateAndApply(event.getPlayer());
	}
	
	//Here's it :D
	public void updateAndApply(final Player player){
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
		new Thread(new Runnable() {
		    @Override
			public void run() {
		   	try {
					skinprofile.attemptUpdate();
				} catch (SkinFetchFailedException e) {
					SkinsRestorer.getInstance().logInfo("Skin fetch failed for player " + player.getName() + ": " + e.getMessage());
					e.printStackTrace();
				}
		    	SkinsRestorerAPI.applySkinBukkit(player);
	}
	}).start();
}
}