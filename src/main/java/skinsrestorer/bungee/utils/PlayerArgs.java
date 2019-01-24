package skinsrestorer.bungee.utils;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Created by McLive on 23.01.2019.
 */
public class PlayerArgs {
    private ProxiedPlayer player;
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
            if (!(sender instanceof ProxiedPlayer)) {
                return false;
            }
            this.player = (ProxiedPlayer) sender;
            this.isOtherPlayer = false;
            return true;
        }

        if (args.length > checkGreater) {
            ProxiedPlayer p = Helper.getPlayerFromNick(args[1]);

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

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public void setPlayer(ProxiedPlayer player) {
        this.player = player;
    }

    public boolean foundPlayer() {
        return foundPlayer;
    }

    public void setFoundPlayer(boolean foundPlayer) {
        this.foundPlayer = foundPlayer;
    }
}