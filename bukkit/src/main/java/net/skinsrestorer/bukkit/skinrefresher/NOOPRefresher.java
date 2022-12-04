package net.skinsrestorer.bukkit.skinrefresher;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class NOOPRefresher implements Consumer<Player> {
    @Override
    public void accept(Player player) {
        // NOOP
    }
}
