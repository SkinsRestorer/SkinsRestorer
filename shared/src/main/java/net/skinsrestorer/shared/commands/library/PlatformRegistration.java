package net.skinsrestorer.shared.commands.library;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlatformRegistration<T> {
    private final String rootNode;
    private final String[] aliases;
    private final String rootPermission;
    private final CommandExecutor<T> executor;
}
