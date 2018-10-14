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
import skinsrestorer.shared.utils.C;
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

        String nick = e.getConnection().getName();

        if (Config.DISABLE_ONJOIN_SKINS) {
            e.completeIntent(plugin);
            return;
        }

        // Don't change skin if player has no custom skin-name set and his username is invalid
        if (SkinStorage.getPlayerSkin(nick) == null && !C.validUsername(nick)) {
            System.out.println("[SkinsRestorer] Not applying skin to " + nick + " (invalid username).");
            e.completeIntent(plugin);
            return;
        }

        SkinsRestorer.getInstance().getProxy().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (Config.DEFAULT_SKINS_ENABLED) {
                    try {
                        // don't apply to premium players when enabled
                        if (!Config.DEFAULT_SKINS_PREMIUM) {
                            // check if player is premium
                            if (MojangAPI.getUUID(nick) != null) {
                                // apply skin from player instead of default skin from cinfig
                                SkinApplier.applySkin(null, nick, (InitialHandler) e.getConnection());
                                e.completeIntent(plugin);
                                return;
                            }
                        }

                        // check if player has an other skin set so we won't overwrite it
                        if (SkinStorage.getPlayerSkin(nick) == null) {
                            List<String> skins = Config.DEFAULT_SKINS;
                            int randomNum = (int) (Math.random() * skins.size());
                            SkinStorage.getOrCreateSkinForPlayer(nick);
                            SkinStorage.setPlayerSkin(nick, skins.get(randomNum));
                            SkinApplier.applySkin(null, nick, (InitialHandler) e.getConnection());
                            e.completeIntent(plugin);
                            return;
                        }
                    } catch (Exception ignored) {
                        e.completeIntent(plugin);
                    }
                }

                SkinApplier.applySkin(null, nick, (InitialHandler) e.getConnection());
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