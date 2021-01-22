package net.skinsrestorer.sponge.utils;

import com.flowpowered.math.vector.Vector3d;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.interfaces.SRApplier;
import net.skinsrestorer.shared.utils.PlayerWrapper;
import net.skinsrestorer.shared.utils.Property;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Collection;

/**
 * Created by McLive on 02.04.2018.
 */
public class SkinApplierSponge implements SRApplier {
    private Player receiver;
    private final SkinsRestorer plugin;

    public SkinApplierSponge(SkinsRestorer plugin) {
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

    public void applySkin(final PlayerWrapper player, final String skin) throws SkinRequestException {
        this.applySkin(player, skin, true);
    }

    public void applySkin(final PlayerWrapper player, final String skin, boolean updatePlayer) throws SkinRequestException {
        Collection<ProfileProperty> oldProps = player.get(Player.class).getProfile().getPropertyMap().get("textures");
        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(skin);
        ProfileProperty newTextures = Sponge.getServer().getGameProfileManager().createProfileProperty("textures", textures.getValue(), textures.getSignature());
        oldProps.clear();
        oldProps.add(newTextures);

        if (!updatePlayer)
            return;

        Sponge.getScheduler().createSyncExecutor(plugin).execute(() -> updatePlayerSkin(player.get(Player.class)));
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
            Sponge.getServer().getWorld(other.getUniqueId()).ifPresent(value -> receiver.setLocation(value.getSpawnLocation()));
            receiver.setLocationAndRotation(loc, rotation);
        }
    }
}
