package net.skinsrestorer.shared.commands.library;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandLiteral {
    private final String literal;
    private final String description;
    private final String permission;
    private final String permissionMessage;
    private final String[] aliases;
    private final String[] conditions;
}
