package net.skinsrestorer.shared.commands.library;

import com.mojang.brigadier.tree.CommandNode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SRCommandNode<T> {
    private final CommandNode<T> brigadierNode;
    private final String description;
    private final String permission;
    private final String permissionMessage;
    private final String[] aliases;
    private final String[] conditions;
}
