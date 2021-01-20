package skinsrestorer.sponge.listeners;

import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Auth;
import org.spongepowered.api.profile.GameProfile;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SRLogger;
import skinsrestorer.sponge.SkinsRestorer;


public class LoginListener implements EventListener<ClientConnectionEvent.Auth> {
    private SkinsRestorer plugin;
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
                    if (Config.DEBUG)
                        System.out.println("[SkinsRestorer] Not applying skin to " + name + " (invalid username).");
                    return;
                }

                String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(name);

                //todo: add default skinurl support
                plugin.getSkinApplier().updateProfileSkin(profile, skin);
            } catch (SkinRequestException ignored) {
            }
        });
    }
}