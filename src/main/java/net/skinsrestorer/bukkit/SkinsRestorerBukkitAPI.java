package net.skinsrestorer.bukkit;

import com.google.common.annotations.Beta;
import net.skinsrestorer.shared.interfaces.ISkinsRestorerAPI;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.MojangAPI;
import net.skinsrestorer.shared.utils.PlayerWrapper;
import net.skinsrestorer.shared.utils.SkinsRestorerAPI;
import org.bukkit.entity.Player;

/**
 * Created by McLive on 27.08.2019.
 */
public class SkinsRestorerBukkitAPI extends SkinsRestorerAPI implements ISkinsRestorerAPI<Player> {
    private final SkinsRestorer plugin;

    public SkinsRestorerBukkitAPI(SkinsRestorer plugin, MojangAPI mojangAPI, SkinStorage skinStorage) {
        super(mojangAPI, skinStorage);
        this.plugin = plugin;
    }

    // Todo: We need to refactor applySkin through all platforms to behave the same!
    @Beta
    @Override
    public void applySkin(PlayerWrapper player, Object props) {
        plugin.getFactory().applySkin(player.get(Player.class), props);
    }

    @Beta
    @Override
    public void applySkin(PlayerWrapper player) {
        plugin.getFactory().applySkin(player.get(Player.class), this.getSkinData(this.getSkinName(player.get(Player.class).getName())));
    }
}
