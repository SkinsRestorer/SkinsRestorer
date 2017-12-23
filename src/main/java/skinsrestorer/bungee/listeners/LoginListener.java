package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.MojangAPI;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoginListener implements Listener {

    @EventHandler
    public void onServerChange(final PostLoginEvent e) {
        if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
                && e.getPlayer().hasPermission("skinsrestorer.cmds"))
            e.getPlayer().sendMessage(C.c(Locale.OUTDATED));

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        if (Config.DEFAULT_SKINS_ENABLED) {
            List<String> skins = Config.DEFAULT_SKINS;
            try {
                SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName());
                SkinsRestorer.getInstance().getProxy().getScheduler();
                SkinApplier.applySkin(e.getPlayer());
            } catch (MojangAPI.SkinRequestException ex) {}
            return;
        }

        if (e.getPlayer().getPendingConnection().isOnlineMode()) {
            SkinsRestorer.getInstance().getProxy().getScheduler().schedule(SkinsRestorer.getInstance(), new Runnable() {

                @Override
                public void run() {
                    SkinApplier.applySkin(e.getPlayer());
                }
            }, 10, TimeUnit.MILLISECONDS);
        } else {
            SkinApplier.applySkin(e.getPlayer());
        }
    }
}
