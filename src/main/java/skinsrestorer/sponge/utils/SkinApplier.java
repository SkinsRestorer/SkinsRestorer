package skinsrestorer.sponge.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import com.flowpowered.math.vector.Vector3d;

import skinsrestorer.sponge.SkinsRestorer;

/**
 * Created by McLive on 02.04.2018.
 */
public class SkinApplier {

    private Player receiver;

    public SkinApplier(SkinsRestorer plugin) {
    }


    public void updatePlayerSkin(Player p) {
    	receiver = p;

    	sendUpdate();
    }

    private void sendUpdate() {
        sendUpdateSelf();

        receiver.offer(Keys.VANISH, true);
        Sponge.getScheduler().createTaskBuilder().execute(() -> receiver.offer(Keys.VANISH, false)).delayTicks(1).submit(SkinsRestorer.getInstance());
    }

    private void sendUpdateSelf() {
        receiver.getTabList().removeEntry(receiver.getUniqueId());
        receiver.getTabList().addEntry(TabListEntry.builder()
                .displayName(receiver.getDisplayNameData().displayName().get())
                .latency(receiver.getConnection().getLatency())
                .list(receiver.getTabList())
                .gameMode(receiver.getGameModeData().type().get())
                .profile(receiver.getProfile())
                .build());

     // Simulate respawn to see skin active
        Location<World> loc = receiver.getLocation();
        Vector3d rotation = receiver.getRotation();

        WorldProperties other = null;
        for (WorldProperties w : Sponge.getServer().getAllWorldProperties()) {
            if (other != null) {
                break;
            }

            if (!w.getUniqueId().equals(receiver.getWorld().getUniqueId())) {
            	Sponge.getServer().loadWorld(w.getUniqueId());
                other = w;
            }
        }

        if (other != null) {
        	receiver.setLocation(Sponge.getServer().getWorld(other.getUniqueId()).get().getSpawnLocation());
        	receiver.setLocationAndRotation(loc, rotation);
}
    }

}
