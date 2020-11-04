package skinsrestorer.bukkit.skinfactory;

import lombok.RequiredArgsConstructor;
import skinsrestorer.shared.storage.Config;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class UniversalSkinFactory extends SkinFactory {
    private final Plugin plugin;
    private final Consumer<Player> refresh = detectRefresh();
    public static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(23);

    @Override
    public void updateSkin(Player player) {
        if (!player.isOnline())
            return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

        	//dismounts a player before refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
        	if (Config.DISMOUNT_PLAYER_ON_UPDATE && player.getVehicle() != null) {
        		
        		player.getVehicle().removePassenger(player);

        	}
        	
        	//dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
        	if (Config.DISMOUNT_PASSENGERS_ON_UPDATE && !player.getPassengers().isEmpty()) {
        		
        		for (Entity passenger : player.getPassengers()) {
        		
        			player.removePassenger(passenger);
        			
        		}

        	}
        	
            for (Player ps : Bukkit.getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(this.plugin, player);
                } catch (Error ignored) {
                    ps.hidePlayer(player);
                }
                try {
                    ps.showPlayer(this.plugin, player);
                } catch (Error ignored) {
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private static Consumer<Player> detectRefresh() {
        // force OldSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
        // todo: reuse code
        File ViaVersion = new File("plugins" + File.separator + "ViaVersion");
        File ViaBackwards = new File("plugins" + File.separator + "ViaBackwards");
        File ViaRewind = new File("plugins" + File.separator + "ViaRewind");
        File ProtocolSupport = new File("plugins" + File.separator + "ProtocolSupport.jar");
        if (ViaVersion.exists() || ViaBackwards.exists() || ViaRewind.exists() || ProtocolSupport.exists()) {
            System.out.println("[SkinsRestorer] INFO: Unsupported plugin (ViaVersion) detected, forcing OldSkinRefresher");
            return new OldSkinRefresher();
        }

        try {
            return new PaperSkinRefresher();
        } catch (ExceptionInInitializerError ignored) {
        }

        // if (NMS_VERSION.equals("v1_16_R1"))
        //     return new LegacySkinRefresher_v1_16_R1();

        // return new LegacySkinRefresher();
        return new OldSkinRefresher();
    }
}