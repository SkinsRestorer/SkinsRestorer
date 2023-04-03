package net.skinsrestorer.shared.commands.library;

public interface CommandPlatform<T> {
    void registerCommand(PlatformRegistration<T> registration);

    void runAsync(Runnable runnable);
}
