package skinsrestorer.bukkit.listeners;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import skinsrestorer.bukkit.SkinsRestorer;

public class PermissionListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent e) {
		final Player p = e.getPlayer();
		new BukkitRunnable(){ 
			@Override 
			public void run() { 
				if (p.hasPermission("skinsrestorer.cmds") || p.isOp())
			    sendBungeePermission(p, "skinsrestorer.cmds");
				if (p.hasPermission("skinsrestorer.playercmds") || p.isOp())
				sendBungeePermission(p, "skinsrestorer.playercmds");
			}
			}.runTaskLater(SkinsRestorer.getInstance(),20L);
	}
	
	public static void sendBungeePermission(Player p, String perm) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);

			out.writeUTF("SkinsPermission");
			out.writeUTF(perm);

			p.sendPluginMessage(SkinsRestorer.getInstance(), "BungeeCord", b.toByteArray());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
