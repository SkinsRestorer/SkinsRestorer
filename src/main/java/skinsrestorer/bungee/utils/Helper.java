package skinsrestorer.bungee.utils;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Created by McLive on 23.01.2019.
 */
public class Helper {

    public static ProxiedPlayer getPlayerFromNick(String nick) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(nick);

        if (player == null)
            for (ProxiedPlayer pl : ProxyServer.getInstance().getPlayers())
                if (pl.getName().startsWith(nick)) {
                    player = pl;
                    break;
                }

        return player;
    }
}