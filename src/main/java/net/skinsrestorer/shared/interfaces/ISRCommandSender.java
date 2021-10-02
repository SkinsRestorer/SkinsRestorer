package net.skinsrestorer.shared.interfaces;

public interface ISRCommandSender {
    void sendMessage(String message);

    String getName();

    boolean hasPermission(String permission);
}
