package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import skinsrestorer.bungee.SkinApplier;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;

import java.util.List;

public class LoginListener implements Listener {

    private SkinsRestorer plugin;

    public LoginListener(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerChange(final LoginEvent e) {
        e.registerIntent(plugin);

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        SkinsRestorer.getInstance().getProxy().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (Config.DEFAULT_SKINS_ENABLED) {
                    try {
                        List<String> skins = Config.DEFAULT_SKINS;
                        int randomNum = (int) (Math.random() * skins.size());
                        SkinStorage.getOrCreateSkinForPlayer(e.getConnection().getName());
                        SkinStorage.setPlayerSkin(e.getConnection().getName(), skins.get(randomNum));
                        SkinApplier.applySkin(null, e.getConnection().getName(), (InitialHandler) e.getConnection());
                        return;
                    } catch (MojangAPI.SkinRequestException ex) {
                        ex.printStackTrace();
                    } finally {
                        e.completeIntent(plugin);
                    }
                }

                SkinApplier.applySkin(null, e.getConnection().getName(), (InitialHandler) e.getConnection());

                e.completeIntent(plugin);
            }
        });
    }

    @EventHandler
    public void onServerChange(final ServerConnectEvent e) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
                    && e.getPlayer().hasPermission("skinsrestorer.cmds"))
                e.getPlayer().sendMessage(new TextComponent(Locale.OUTDATED));
        });
    }

}