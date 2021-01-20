package skinsrestorer.sponge;

import com.google.common.annotations.Beta;
import org.spongepowered.api.entity.living.player.Player;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.SkinsRestorerAPI;

/**
 * Created by McLive on 10.11.2019.
 */
public class SkinsRestorerSpongeAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<Player> {
    private SkinsRestorer plugin;

    public SkinsRestorerSpongeAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
        super(mojangAPI, skinStorage);
        this.plugin = plugin;
    }

    // Todo: We need to refactor applySkin through all platforms to behave the same!
    @Beta
    @Override
    public void applySkin(Player player, Object props) {
        this.applySkin(player);
    }

    @Beta
    @Override
    public void applySkin(Player player) {
        try {
            plugin.getSkinApplier().applySkin(player, this.getSkinName(player.getName()));
        } catch (SkinRequestException e) {
            e.printStackTrace();
        }
    }
}
