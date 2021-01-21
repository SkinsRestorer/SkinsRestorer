package net.skinsrestorer.sponge.listeners;

import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Auth;
import org.spongepowered.api.profile.GameProfile;

public class LoginListener implements EventListener<ClientConnectionEvent.Auth> {
    private final SkinsRestorer plugin;
    private SRLogger log;

    public LoginListener(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Auth e) {
        if (e.isCancelled())
            return;

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        GameProfile profile = e.getProfile();

        profile.getName().ifPresent(name -> {
            try {
                // Don't change skin if player has no custom skin-name set and his username is invalid
                if (plugin.getSkinStorage().getPlayerSkin(name) == null && !C.validUsername(name)) {
                    log.log("[SkinsRestorer] Not applying skin to " + name + " (invalid username).");
                    return;
                }

                String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(name);

                //todo: add default skinurl support
                plugin.getSkinApplierSponge().updateProfileSkin(profile, skin);
            } catch (SkinRequestException ignored) {
            }
        });
    }
}