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

				@SuppressWarnings("deprecation")
				@Override
				public void run() {

					Object props = null;

					try {
						props = MojangAPI.getSkinProperty(MojangAPI.getUUID(skin));
					} catch (SkinRequestException e) {
						props = SkinStorage.getSkinData(skin);

						if (props != null) {
							SkinStorage.setPlayerSkin(pl, skin);

							if (p != null) {
								p.sendMessage(Locale.SKIN_CHANGE_SUCCESS_DATABASE);
								SkinApplier.applySkin(p);
							}
							return;
						}
						return;
					}

					SkinStorage.setSkinData(skin, props);
					SkinStorage.setPlayerSkin(p.getName(), skin);
					if (p != null) {
						p.sendMessage(Locale.SKIN_CHANGE_SUCCESS);
						SkinApplier.applySkin(p);
					}
					return;
				}

			});

		} catch (Exception ex) {
		}

	}

}
