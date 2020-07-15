package skinsrestorer.bukkit.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;

/**
 * Created by McLive on 21.01.2019.
 */
public class PlayerJoin implements Listener {
    private final SkinsRestorer plugin;

    public PlayerJoin(final SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        if (Config.DISABLE_ONJOIN_SKINS) {
            // factory.applySkin(e.getPlayer(), SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.getPlayer().getName())));
            // shouldn't it just skip it if it's true?
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final SkinStorage skinStorage = plugin.getSkinStorage();
                final Player player = e.getPlayer();
                final String playerName = player.getName();
                
                // Don't change skin if player has no custom skin-name set and his username is invalid
                if (skinStorage.getPlayerSkin(playerName) == null && !C.validUsername(playerName)) {
                    System.out.println("[SkinsRestorer] Not applying skin to " + playerName + " (invalid username).");
                    return;
                }

                final String skin = skinStorage.getDefaultSkinNameIfEnabled(playerName);

                plugin.getFactory().applySkin(player, skinStorage.getOrCreateSkinForPlayer(skin));
            } catch (SkinRequestException ignored) {
            }
        });
    }
}
