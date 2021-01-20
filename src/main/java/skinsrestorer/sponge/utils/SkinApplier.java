package skinsrestorer.sponge.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import com.flowpowered.math.vector.Vector3d;

import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.utils.Property;
import skinsrestorer.sponge.SkinsRestorer;

import java.util.Collection;

/**
 * Created by McLive on 02.04.2018.
 */
public class SkinApplier {
    private Player receiver;
    private SkinsRestorer plugin;

    public SkinApplier(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    public void updateProfileSkin(GameProfile profile, String skin) throws SkinRequestException {
        // Todo: new function for this duplicated code
        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin);
        Collection<ProfileProperty> oldProperties = profile.getPropertyMap().get("textures");
        ProfileProperty newTextures = Sponge.getServer().getGameProfileManager().createProfileProperty("textures", textures.getValue(), textures.getSignature());
        oldProperties.clear();
        oldProperties.add(newTextures);
    }

    public void applySkin(final Player p, final String skin) throws SkinRequestException {
        this.applySkin(p, skin, true);
    }

    public void applySkin(final Player p, final String skin, boolean updatePlayer) throws SkinRequestException {
        Collection<ProfileProperty> oldProps = p.getProfile().getPropertyMap().get("textures");
        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin);
        ProfileProperty newTextures = Sponge.getServer().getGameProfileManager().createProfileProperty("textures", textures.getValue(), textures.getSignature());
        oldProps.clear();
        oldProps.add(newTextures);

        if (!updatePlayer)
            return;

        Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> {
            this.updatePlayerSkin(p);
        });
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
