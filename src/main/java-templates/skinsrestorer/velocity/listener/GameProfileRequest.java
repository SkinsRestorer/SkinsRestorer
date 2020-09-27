package skinsrestorer.velocity.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.utils.C;
import skinsrestorer.velocity.SkinsRestorer;

/**
 * Created by McLive on 16.02.2019.
 */
public class GameProfileRequest {
    private final SkinsRestorer plugin;

    @Inject
    public GameProfileRequest(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent e) {
        String nick = e.getUsername();

        if (Config.DISABLE_ONJOIN_SKINS) {
            return;
        }

        if (e.isOnlineMode()) {
            return;
        }

        // Don't change skin if player has no custom skin-name set and his username is invalid
        if (plugin.getSkinStorage().getPlayerSkin(nick) == null && !C.validUsername(nick)) {
            System.out.println("[SkinsRestorer] Not applying skin to " + nick + " (invalid username).");
            return;
        }

        String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(nick);

        //todo: default skinurl support
        e.setGameProfile(plugin.getSkinApplier().updateProfileSkin(e.getGameProfile(), skin));
    }
}
