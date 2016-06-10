package skinsrestorer.bukkit.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.api.SkinsRestorerAPI;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.ConfigStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class LoginListener implements Listener {

	// fix skin on player login
	@EventHandler(priority = EventPriority.LOW)
	public void onLoginEvent(final PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}
		if (ConfigStorage.getInstance().USE_BOT_FEATURE == true) {
			return;
		}
		if (SkinsRestorer.getInstance().isAutoInEnabled()) {
			if (ConfigStorage.getInstance().USE_AUTOIN_SKINS == true
					&& SkinsRestorer.getInstance().getAutoInAPI().getPremiumStatus(event.getPlayer()
							.getName()) == com.gmail.bartlomiejkmazur.autoin.api.PremiumStatus.PREMIUM) {
				return;
			}
		}
		// Updating and applying skin.
		updateAndApply(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onLoginEvent(final PlayerJoinEvent event) {
		if (ConfigStorage.getInstance().USE_BOT_FEATURE == false) {
			return;
		}
		if (SkinsRestorer.getInstance().isAutoInEnabled()) {
			if (ConfigStorage.getInstance().USE_AUTOIN_SKINS == true
					&& SkinsRestorer.getInstance().getAutoInAPI().getPremiumStatus(event.getPlayer()
							.getName()) == com.gmail.bartlomiejkmazur.autoin.api.PremiumStatus.PREMIUM) {
				return;
			}
		}

		// Fix for online mode
		if (Bukkit.getOnlineMode()) {
			for (Player p : Bukkit.getOnlinePlayers())
				SkinsRestorer.getInstance().getFactory().updateSkin(p);
		}

		if (SkinStorage.getInstance().getSkinData(event.getPlayer().getName()) != null) {
			updateAndApply(event.getPlayer());
			return;
		}
		if (event.getPlayer().hasPermission("skinsrestorer.playercmds")) {
			final TextComponent message = new TextComponent("");
			message.setClickEvent(
					new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skin set " + event.getPlayer().getName()));
			message.addExtra(LocaleStorage.getInstance().DO_YOU_WANT_SKIN.replaceAll("&", "\u00a7"));
			Bukkit.getScheduler().scheduleSyncDelayedTask(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {
					event.getPlayer().sendMessage(message.toPlainText());
				}

			}, 5L);
		}
	}

	// Here's it :D
	public void updateAndApply(final Player player) {
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinForPlayer(player.getName());
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					skinprofile.attemptUpdate();
				} catch (SkinFetchFailedException e) {
					SkinsRestorer.getInstance()
							.logInfo("Skin fetch failed for player " + player.getName() + ": " + e.getMessage());
					e.printStackTrace();
				}
				SkinsRestorerAPI.applySkin(player);
			}
		}).start();
	}
}