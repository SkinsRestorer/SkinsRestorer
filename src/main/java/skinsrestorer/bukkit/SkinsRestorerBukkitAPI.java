package skinsrestorer.bukkit;

import com.google.common.annotations.Beta;
import org.bukkit.entity.Player;
import skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.SkinsRestorerAPI;

/**
 * Created by McLive on 27.08.2019.
 */
public class SkinsRestorerBukkitAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<Player> {
    private SkinsRestorer plugin;

    public SkinsRestorerBukkitAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
        super(mojangAPI, skinStorage);
        this.plugin = plugin;
    }

    // Todo: We need to refactor applySkin through all platforms to behave the same!
    @Beta
    @Override
    public void applySkin(Player player, Object props) {
        plugin.getFactory().applySkin(player, props);
    }

    @Beta
    @Override
    public void applySkin(Player player) {
        plugin.getFactory().applySkin(player, this.getSkinData(this.getSkinName(player.getName())));
    }
}
