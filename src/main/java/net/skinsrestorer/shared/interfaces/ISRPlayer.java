package net.skinsrestorer.shared.interfaces;

import net.skinsrestorer.api.PlayerWrapper;

public interface ISRPlayer {
    PlayerWrapper getWrapper();

    String getName();

    void sendMessage(String message);
}
