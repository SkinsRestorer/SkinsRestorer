package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.bungee.SkinApplier;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoginListener implements Listener {

    @EventHandler
    public void onServerChange(final PostLoginEvent e) {
<<<<<<< HEAD
    	ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
    		if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
	                && e.getPlayer().hasPermission("skinsrestorer.cmds"))
	            e.getPlayer().sendMessage(new TextComponent(C.c(Locale.OUTDATED)));
	
	        if (Config.DISABLE_ONJOIN_SKINS)
	            return;
	
	        if (Config.DEFAULT_SKINS_ENABLED) {
	        	@SuppressWarnings("unused")
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
    	});
=======
        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (Config.UPDATER_ENABLED && SkinsRestorer.getInstance().isOutdated()
                    && e.getPlayer().hasPermission("skinsrestorer.cmds"))
                e.getPlayer().sendMessage(new TextComponent(C.c(Locale.OUTDATED)));

            if (Config.DISABLE_ONJOIN_SKINS)
                return;

            if (Config.DEFAULT_SKINS_ENABLED) {
                try {
                    List<String> skins = Config.DEFAULT_SKINS;
                    int randomNum = 0 + (int) (Math.random() * skins.size());
                    SkinStorage.getOrCreateSkinForPlayer(e.getPlayer().getName());
                    SkinStorage.setPlayerSkin(e.getPlayer().getName(), skins.get(randomNum));
                    SkinApplier.applySkin(e.getPlayer().getName());
                    return;
                } catch (MojangAPI.SkinRequestException ex) {
                    ex.printStackTrace();
                }
            }

            SkinsRestorer.getInstance().getProxy().getScheduler().schedule(SkinsRestorer.getInstance(), new Runnable() {

                @Override
                public void run() {
                    SkinApplier.applySkin(e.getPlayer());
                }
            }, 10, TimeUnit.MILLISECONDS);
        });
>>>>>>> 27759a3951853463c5ee67408c9a603de32717d7
    }
}