package skinsrestorer.bungee.listeners;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;

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

			final ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pl);

			final String skin = dis.readUTF();

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {

					Object textures = null;

					try {
						textures = MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));

						if (textures == null)
							throw new SkinRequestException(Locale.NO_SKIN_DATA);

						SkinStorage.setSkinData(skin, textures);
						SkinStorage.setPlayerSkin(pl, skin);
					} catch (SkinRequestException e) {
						SkinStorage.setPlayerSkin(pl, skin);
					}

					if (p != null)
						SkinApplier.applySkin(p);
				}

			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
