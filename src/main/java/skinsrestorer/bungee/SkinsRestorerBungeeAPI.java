package skinsrestorer.bungee;

import com.google.common.annotations.Beta;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.SkinsRestorerAPI;

/**
 * Created by McLive on 10.11.2019.
 */
public class SkinsRestorerBungeeAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<ProxiedPlayer> {
    private SkinsRestorer plugin;

    public SkinsRestorerBungeeAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
        super(mojangAPI, skinStorage);
        this.plugin = plugin;
    }

    // Todo: We need to refactor applySkin through all platforms to behave the same!
    @Beta
    @Override
    public void applySkin(ProxiedPlayer player, Object props) {
        this.applySkin(player);
    }

    @Beta
    @Override
    public void applySkin(ProxiedPlayer player) {
        try {
            plugin.getSkinApplier().applySkin(player);
        } catch (Exception e) {
        }
    }
}
