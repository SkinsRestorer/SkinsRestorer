package net.skinsrestorer.shared.commands.library;

public interface CommandPlatform<T> {
    void registerCommand(String rootNode, String[] aliases, String rootPermission, CommandExecutor<T> executor);
}
