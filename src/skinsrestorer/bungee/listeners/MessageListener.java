package skinsrestorer.bungee.listeners;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class MessageListener implements Listener {

	/** Class by Blackfire62 **/

	@EventHandler
	public void onPluginMessage(PluginMessageEvent e) {
		if (!e.getTag().equalsIgnoreCase("BungeeCord"))
			return;

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(e.getData()));

		try {

			final String channel = dis.readUTF();

			if (!channel.equalsIgnoreCase("SkinsRestorer"))
				return;

			final String pl = dis.readUTF();

			final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pl);

			if (player == null || player.getServer() == null)
				return;

			final String skin = dis.readUTF();

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {
					try {

						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(skin, null);
						SkinStorage.getInstance().setSkinData(skinprofile);
						SkinStorage.getInstance().setPlayerSkin(player.getName(), skinprofile.getName());
						SkinsRestorer.getInstance().getFactory().applySkin(player);
					} catch (SkinFetchFailedException ex) {
						SkinProfile skinprofile = SkinStorage.getInstance().getSkinData(skin);
						if (skinprofile != null) {
							SkinStorage.getInstance().setSkinData(skinprofile);
							SkinStorage.getInstance().setPlayerSkin(player.getName(), skinprofile.getName());
							SkinsRestorer.getInstance().getFactory().applySkin(player);
						}

					}
				}
			});

		} catch (Exception ex) {
			// These things like to throw exceptions because of possible
			// deformation
			return;
		}

	}

}
