package net.skinsrestorer.shared.utils;

/**
 * Makes it possible to get all platforms into a single API merged.
 */
public class PlayerWrapper {
    private final Object player;

    public PlayerWrapper(Object player) {
        this.player = player;
    }

    public <A> A get(Class<A> playerClass) {
        return playerClass.cast(player);
    }
}