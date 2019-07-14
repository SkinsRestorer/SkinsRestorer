package skinsrestorer.bukkit.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;

/**
 * Created by McLive on 21.01.2019.
 */
public class PlayerJoin implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            try {
                if (Config.DISABLE_ONJOIN_SKINS) {
                    // factory.applySkin(e.getPlayer(), SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.getPlayer().getName())));
                    // shouldn't it just skip it if it's true?
                    return;
                }

                // Don't change skin if player has no custom skin-name set and his username is invalid
                if (SkinStorage.getPlayerSkin(e.getPlayer().getName()) == null && !C.validUsername(e.getPlayer().getName())) {
                    System.out.println("[SkinsRestorer] Not applying skin to " + e.getPlayer().getName() + " (invalid username).");
                    return;
                }

                String skin = SkinStorage.getDefaultSkinNameIfEnabled(e.getPlayer().getName());

                SkinsRestorer.getInstance().getFactory().applySkin(e.getPlayer(), SkinStorage.getOrCreateSkinForPlayer(skin));
            } catch (MojangAPI.SkinRequestException ignored) {
            }
        });
    }
}
