package skinsrestorer.sponge.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabList;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import com.flowpowered.math.vector.Vector3d;

import skinsrestorer.sponge.SkinsRestorer;

/**
 * Created by McLive on 02.04.2018.
 */
public class SkinApplier {
    public SkinApplier(SkinsRestorer plugin) {
    }

    public void updatePlayerSkin(Player p) {
        // Update tablist for player with modified profile
        TabList tab = p.getTabList();
        tab.removeEntry(p.getUniqueId());
        TabListEntry playerTablist = TabListEntry.builder()
                .list(tab)
                .profile(p.getProfile())
                .displayName(Text.of(p.getDisplayNameData().displayName().get()))
                .latency(p.getConnection().getLatency())
                .gameMode(p.getGameModeData().type().get())
                .build();
        tab.addEntry(playerTablist);

        // Simulate respawn to see skin active
        Location<World> loc = p.getLocation();
        Vector3d rotation = p.getRotation();

        WorldProperties other = null;
        for (WorldProperties w : Sponge.getServer().getAllWorldProperties()) {
            if (other != null) {
                break;
            }
            
            if (!w.getUniqueId().equals(p.getWorld().getUniqueId())) {
            	Sponge.getServer().loadWorld(w.getUniqueId());
                other = w;
            }
        }

        if (other != null) {
            p.setLocation(Sponge.getServer().getWorld(other.getUniqueId()).get().getSpawnLocation());
            p.setLocationAndRotation(loc, rotation);
        }
    }
}
