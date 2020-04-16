package skinsrestorer.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Created by McLive on 23.01.2019.
 */
public class Helper {

    public static Player getPlayerFromNick(String nick) {
        Player player = Bukkit.getServer().getPlayer(nick);

        if (player == null)
            for (Player pl : Bukkit.getServer().getOnlinePlayers())
                if (pl.getName().startsWith(nick)) {
                    player = pl;
                    break;
                }

        return player;
    }
}