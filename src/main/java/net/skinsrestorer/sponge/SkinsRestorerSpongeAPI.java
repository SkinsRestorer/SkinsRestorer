package net.skinsrestorer.sponge;

import com.google.common.annotations.Beta;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.MojangAPI;
import net.skinsrestorer.shared.utils.SkinsRestorerAPI;
import org.spongepowered.api.entity.living.player.Player;

/**
 * Created by McLive on 10.11.2019.
 */
public class SkinsRestorerSpongeAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<Player> {
    private final SkinsRestorer plugin;

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
            plugin.getSkinApplierSponge().applySkin(player, this.getSkinName(player.getName()));
        } catch (SkinRequestException e) {
            e.printStackTrace();
        }
    }
}
