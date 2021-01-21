package net.skinsrestorer.velocity.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

/**
 * Created by McLive on 16.02.2019.
 */
public class GameProfileRequest {
    private final SkinsRestorer plugin;
    private SRLogger log;

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
        if (plugin.getSkinStorage().getPlayerSkin(nick) == null && !C.validUsername(nick.replaceAll("\\W", ""))) {
            log.log("[SkinsRestorer] Not requesting skin for " + nick + " (invalid username).");
            return;
        }

        String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(nick);

        //todo: default skinurl support
        e.setGameProfile(plugin.getSkinApplierVelocity().updateProfileSkin(e.getGameProfile(), skin));
    }
}
