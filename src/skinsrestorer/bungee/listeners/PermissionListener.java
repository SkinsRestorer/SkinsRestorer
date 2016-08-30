package skinsrestorer.bungee.listeners;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PermissionListener implements Listener {

	@EventHandler
	public void onPluginMessage(PluginMessageEvent e) {
		if (!e.getTag().equalsIgnoreCase("BungeeCord"))
			return;
		
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

		try {
			String subchannel = in.readUTF();
			if (subchannel.equalsIgnoreCase("SkinsPermission"))
				((ProxiedPlayer) e.getReceiver()).setPermission(in.readUTF(), true);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
