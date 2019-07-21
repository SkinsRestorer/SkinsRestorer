package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.exception.SkinRequestException;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.C;

public class LoginListener implements Listener {
    private SkinsRestorer plugin;

    public LoginListener(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerChange(final LoginEvent e) {
        e.registerIntent(plugin);
        String nick = e.getConnection().getName();

        if (e.isCancelled() && Config.NO_SKIN_IF_LOGIN_CANCELED) {
            e.completeIntent(plugin);
            return;
        }

        if (Config.DISABLE_ONJOIN_SKINS) {
            e.completeIntent(plugin);
            return;
        }

        // Don't change skin if player has no custom skin-name set and his username is invalid
        if (plugin.getSkinStorage().getPlayerSkin(nick) == null && !C.validUsername(nick)) {
            System.out.println("[SkinsRestorer] Not applying skin to " + nick + " (invalid username).");
            e.completeIntent(plugin);
            return;
        }

        SkinsRestorer.getInstance().getProxy().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(nick);
            try {
                plugin.getSkinApplier().applySkin(null, skin, (InitialHandler) e.getConnection());
            } catch (SkinRequestException ignored) {
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            e.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onServerChange(final ServerConnectEvent e) {
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()) {
                if (e.getPlayer().hasPermission("skinsrestorer.admincommand") || e.getPlayer().hasPermission("skinsrestorer.cmds"))
                    e.getPlayer().sendMessage(new TextComponent(Locale.OUTDATED));
            }
        });
    }
}