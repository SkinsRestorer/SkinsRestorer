package skinsrestorer.bungee.listeners;

import lombok.Setter;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
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
import skinsrestorer.shared.utils.SRLogger;

public class LoginListener implements Listener {
    private SkinsRestorer plugin;
    @Setter
    private SRLogger log;

    public LoginListener(SkinsRestorer plugin, SRLogger log) {
        this.plugin = plugin;
        this.log = log;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final LoginEvent e) {
        if (e.isCancelled() && Config.NO_SKIN_IF_LOGIN_CANCELED) {
            return;
        }

        if (Config.DISABLE_ONJOIN_SKINS) {
            return;
        }

        e.registerIntent(plugin);

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            final PendingConnection connection = e.getConnection();
            final String nick = connection.getName();

            // Don't change skin if player has no custom skin-name set and his username is invalid
            if (!C.validUsername(nick.replaceAll("\\W", "")) && plugin.getSkinStorage().getPlayerSkin(nick) == null) {
                if (Config.DEBUG)
                    System.out.println("[SkinsRestorer] Not applying skin to " + connection.getName() + " (invalid username).");
                return;
            }

            final String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(nick);

            try {
                // todo: add default skinurl support
                plugin.getSkinApplier().applySkin(null, skin, (InitialHandler) connection);
            } catch (SkinRequestException ignored) {
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            e.completeIntent(plugin);
        });
    }

    //think we should no have EventPriority.HIGH just to check for updates...
    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnect(final ServerConnectEvent e) {
        if (e.isCancelled()) {
            return;
        }

        // Mission and vision yet to be decided.
        /* //Better update notifications are in the pipeline.
        if (!Config.UPDATER_ENABLED) {
            return;
        }*/

        // todo: is this even something we should keep after updaterRework?
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            if (plugin.isOutdated()) {
                final ProxiedPlayer player = e.getPlayer();

                if (player.hasPermission("skinsrestorer.admincommand"))
                    player.sendMessage(TextComponent.fromLegacyText(Locale.OUTDATED));
            }
        });
    }
}
