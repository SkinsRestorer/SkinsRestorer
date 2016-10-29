package skinsrestorer.bukkit.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.ReflectionUtil;

public class LoginListener implements Listener {

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		final Player p = e.getPlayer();

		SkinsRestorer.getInstance().getExecutor().submit(new Runnable() {

			@Override
			public void run() {
				SkinsRestorer.getInstance().getFactory().applySkin(p,
						SkinStorage.getOrCreateSkinForPlayer(p.getName()));
			}
		});
	}

	@EventHandler
	public void onJoin(final PlayerJoinEvent e) {
		if (ReflectionUtil.serverVersion.contains("1_7")) {
			PacketListenerv1_7.inject(e.getPlayer());
		} else {
			PacketListener.inject(e.getPlayer());
		}

		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		final Player p = e.getPlayer();

		SkinsRestorer.getInstance().getExecutor().submit(new Runnable() {

			@Override
			public void run() {
				try {
					String skin = SkinStorage.getPlayerSkin(p.getName());

					if (skin == null)
						skin = e.getPlayer().getName();

					Object props = MojangAPI.getSkinProperty(skin, MojangAPI.getUUID(skin));

					SkinStorage.setSkinData(skin, props);
					SkinsRestorer.getInstance().getFactory().applySkin(p,
							SkinStorage.getOrCreateSkinForPlayer(p.getName()));

					SkinsRestorer.getInstance().getFactory().updateSkin(p);
				} catch (SkinRequestException ex) {
				}
			}

		});
	}
}