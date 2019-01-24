package skinsrestorer.bukkit.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by McLive on 23.01.2019.
 */
public class PlayerArgs {
    private Player player;
    private boolean isOtherPlayer;
    private boolean foundPlayer;

    public PlayerArgs(CommandSender sender, String[] args) {
        this.foundPlayer = this.compute(sender, args, 1);
    }

    public PlayerArgs(CommandSender sender, String[] args, int checkGreater) {
        this.foundPlayer = this.compute(sender, args, checkGreater);
    }

    private boolean compute(CommandSender sender, String[] args, int checkGreater) {
        if (args.length == 1 || args.length == checkGreater) {
            if (!(sender instanceof Player)) {
                return false;
            }
            this.player = (Player) sender;
            this.isOtherPlayer = false;
            return true;
        }

        if (args.length > checkGreater) {
            Player p = Helper.getPlayerFromNick(args[1]);

            if (p == null) {
                return false;
            }

            this.player = p;
            this.isOtherPlayer = true;
            return true;
        }
        return false;
    }

    public boolean isOtherPlayer() {
        return isOtherPlayer;
    }

    public void setOtherPlayer(boolean otherPlayer) {
        isOtherPlayer = otherPlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public boolean foundPlayer() {
        return foundPlayer;
    }

    public void setFoundPlayer(boolean foundPlayer) {
        this.foundPlayer = foundPlayer;
    }
}