package skinsrestorer.bukkit.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent e) {
		if (SkinsRestorer.getInstance().is18plus())
			SkinsPacketHandler.inject(e.getPlayer());
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (Config.DISABLE_ONJOIN_SKINS)
			return;

		String skinname = SkinStorage.getPlayerSkin(e.getPlayer().getName());

		if (skinname == null || skinname.isEmpty())
			skinname = e.getPlayer().getName();

		String skin = skinname;

		Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

			@Override
			public void run() {
				try {
					Object props = MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));
					SkinStorage.setSkinData(skin, props);
				} catch (SkinRequestException ex) {
				}
			}

		});

		if (!SkinsRestorer.getInstance().is18plus())
			try {
				Object textures = SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName());

				Object cp = ReflectionUtil.getBukkitClass("entity.CraftPlayer").cast(e.getPlayer());
				Object ep = ReflectionUtil.invokeMethod(cp.getClass(), cp, "getHandle");
				Object profile = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
				SkinsRestorer.getInstance().applyToGameProfile(profile, textures);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}
}