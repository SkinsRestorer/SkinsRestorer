package skinsrestorer.bukkit.skinfactory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;

import java.io.File;
import java.util.function.Consumer;
import java.util.logging.Level;

@RequiredArgsConstructor
public class UniversalSkinFactory extends SkinFactory {

	private final Plugin plugin;
    private final Consumer<Player> refresh = detectRefresh();
    public static final String NMS_VERSION = Bukkit.getServer().getClass().getPackage().getName().substring(23);
    private boolean checkOptFileChecked = false;
    private boolean disableDismountPlayer;
    private boolean enableDismountEntities;
    private boolean enableRemountPlayer;

    @Override
    public void updateSkin(Player player) {

        if (!player.isOnline())
            return;

        if (checkOptFileChecked)

            this.checkoptfile();
        
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

            Entity vehicle = player.getVehicle();

            //dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
            if ((Config.DISMOUNT_PLAYER_ON_UPDATE || !disableDismountPlayer) && vehicle != null) {

            	vehicle.removePassenger(player);

            	if (Config.REMOUNT_PLAYER_ON_UPDATE || enableRemountPlayer) {

	            	//this is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
	                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

	                	//this is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
	                	if (vehicle.isValid()) {
	                	
	                		vehicle.addPassenger(player);
	                		
	                	}

	                }, 1);
	                
            	}

            }

            //dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
            if ((Config.DISMOUNT_PASSENGERS_ON_UPDATE || enableDismountEntities) && !player.getPassengers().isEmpty()) {
            
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
        // No need to check for all three Vias as ViaVersion has to be installed for the other two to work.
        // Ran with getPlugin != null instead of isPluginEnabled as older Spigot builds return false during the login process even if enabled
        boolean ViaVersionExists = SkinsRestorer.getInstance().getServer().getPluginManager().getPlugin("ViaVersion") != null;
        boolean ProtocolSupportExists = SkinsRestorer.getInstance().getServer().getPluginManager().getPlugin("ProtocolSupport") != null;
        if (ViaVersionExists || ProtocolSupportExists) {
            SkinsRestorer.getInstance().getLogger().log(Level.INFO, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing OldSkinRefresher");
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

    private void checkoptfile() {

        File fileDisableDismountPlayer = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "disablesdismountplayer");
        File fileEnableDismountEntities = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "enablesdismountentities");
        File fileEnableRemountEntiteis = new File("plugins" + File.separator + "SkinsRestorer" + File.separator + "enablesremountentities");

        if (fileDisableDismountPlayer.exists())
            disableDismountPlayer = true;

        if (fileEnableDismountEntities.exists())
            enableDismountEntities = true;
        
        if (fileEnableRemountEntiteis.exists())
            enableRemountPlayer = true;

        checkOptFileChecked = true;

    }

}